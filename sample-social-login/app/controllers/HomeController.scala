package controllers

import java.security.SecureRandom
import java.util.Base64
import java.net.URLEncoder
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.ws._
import scala.concurrent.{ExecutionContext, Future}
import play.api.Configuration
import play.api.libs.json._
import org.slf4j.LoggerFactory

import repositories.{UserRepository, NoteRepository}
import models.{User, Note}
import java.sql.Timestamp
import java.time.Instant

@Singleton
class HomeController @Inject()(
    val controllerComponents: ControllerComponents,
    ws: WSClient,
    config: Configuration,
    userRepository: UserRepository,
    noteRepository: NoteRepository
)(implicit ec: ExecutionContext) extends BaseController {
  private val logger = LoggerFactory.getLogger(this.getClass)

  val clientId = config.get[String]("DISCORD_CLIENT_ID")
  val clientSecret = config.get[String]("DISCORD_CLIENT_SECRET")
  val redirectUri = config.get[String]("REDIRECT_URI")

  def index = Action.async { implicit request: Request[AnyContent] =>
    request.session.get("user").map { discordId =>
      userRepository.findByDiscordId(discordId).flatMap {
        case Some(user) =>
          noteRepository.listByUser(user.id.get).map { notes =>
            Ok(views.html.index(Some(user), notes))
          }
        case None =>
          Future.successful(Ok(views.html.index(None, Seq.empty)))
      }
    }.getOrElse {
      Future.successful(Ok(views.html.index(None, Seq.empty)))
    }
  }

  // state生成メソッド
  def generateState(): String = {
    val random = new SecureRandom()
    val bytes = new Array[Byte](16)
    random.nextBytes(bytes)
    Base64.getUrlEncoder.encodeToString(bytes)
  }

  def login = Action { implicit request: Request[AnyContent] =>
    val state = generateState()
    val discordAuthUrl = "https://discord.com/api/oauth2/authorize"
    val params = Map(
      "client_id" -> clientId,
      "redirect_uri" -> redirectUri,
      "response_type" -> "code",
      "scope" -> "identify email",
      "state" -> state
    )
    val urlWithParams = discordAuthUrl + "?" + params.map { case (k, v) => s"$k=${URLEncoder.encode(v, "UTF-8")}" }.mkString("&")
    Redirect(urlWithParams).withSession(request.session + ("oauthState" -> state))
  }

  def callback(codeOpt: Option[String], stateOpt: Option[String], errorOpt: Option[String]) = Action.async { implicit request =>
    val sessionStateOpt = request.session.get("oauthState")
    (stateOpt, sessionStateOpt) match {
    case (Some(state), Some(sessionState)) if state == sessionState =>
    // stateが一致する場合、セッションからstateを削除
    val newSession = request.session - "oauthState"
    codeOpt match {
      case Some(code) =>
        val tokenUrl = "https://discord.com/api/oauth2/token"
        val data = Map(
          "client_id" -> clientId,
          "client_secret" -> clientSecret,
          "grant_type" -> "authorization_code",
          "code" -> code,
          "redirect_uri" -> redirectUri,
          "scope" -> "identify email"
        )

        val formData = data.map { case (k, v) => s"${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}" }.mkString("&")

        ws.url(tokenUrl)
          .addHttpHeaders(
            "Content-Type" -> "application/x-www-form-urlencoded"
          )
          .post(formData)
          .flatMap { response =>
            val json = response.json
            val accessTokenOpt = (json \ "access_token").asOpt[String]
            accessTokenOpt match {
              case Some(accessToken) =>
                ws.url("https://discord.com/api/users/@me")
                  .addHttpHeaders("Authorization" -> s"Bearer $accessToken")
                  .get()
                  .flatMap { userResponse =>
                    val userJson = userResponse.json
                    val discordId = (userJson \ "id").as[String]
                    val username = (userJson \ "username").as[String]
                    userRepository.findByDiscordId(discordId).flatMap {
                      case Some(user) =>
                        userRepository.updateUsername(discordId, username).map { _ =>
                          Redirect(routes.HomeController.index).withSession("user" -> discordId)
                        }
                      case None =>
                        val newUser = User(
                          id = None,
                          discordId = discordId,
                          username = username,
                          createdAt = Some(Timestamp.from(Instant.now())),
                          updatedAt = Some(Timestamp.from(Instant.now()))
                        )
                        userRepository.create(newUser).map { _ =>
                          Redirect(routes.HomeController.index).withSession("user" -> discordId)
                        }
                    }
                  }
              case None =>
                Future.successful(Redirect(routes.HomeController.index).flashing("error" -> "アクセストークンの取得に失敗しました。"))
            }
          }
      case None =>
        Future.successful(Redirect(routes.HomeController.index).flashing("error" -> "認可が拒否されました。"))
    }
    case _ =>
        Future.successful(Redirect(routes.HomeController.index).flashing("error" -> "不正な認証リクエストです。"))
    }
  }

  def logout = Action {
    Redirect(routes.HomeController.index).withNewSession
  }

  def createNote = Action.async { implicit request =>
    request.session.get("user").map { discordId =>
      userRepository.findByDiscordId(discordId).flatMap {
        case Some(user) =>
          user.id match {
            case Some(userId) =>
              val contentOpt = request.body.asFormUrlEncoded.flatMap(_.get("content").flatMap(_.headOption))
              contentOpt match {
                case Some(content) if content.nonEmpty =>
                  val note = Note(
                    id = None,
                    userId = userId,
                    content = content,
                    createdAt = Some(Timestamp.from(Instant.now())),
                    updatedAt = Some(Timestamp.from(Instant.now()))
                  )
                  noteRepository.create(note).map { _ =>
                    Redirect(routes.HomeController.index)
                  }.recover {
                    case ex: Exception =>
                      Redirect(routes.HomeController.index).flashing("error" -> s"メモの保存中にエラーが発生しました: ${ex.getMessage}")
                  }
                case _ =>
                  Future.successful(Redirect(routes.HomeController.index).flashing("error" -> "メモの内容が空です。"))
              }
            case None =>
              Future.successful(Redirect(routes.HomeController.index).flashing("error" -> "ユーザーIDが見つかりません。"))
          }
        case None =>
          Future.successful(Redirect(routes.HomeController.login))
      }
    }.getOrElse {
      Future.successful(Redirect(routes.HomeController.login))
    }
  }
}

