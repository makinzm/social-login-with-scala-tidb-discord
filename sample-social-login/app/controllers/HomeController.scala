package controllers

import java.net.URLEncoder
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.ws._
import scala.concurrent.{ExecutionContext, Future}
import play.api.Configuration
import play.api.libs.json._
import org.slf4j.LoggerFactory

import repositories.UserRepository
import models.User
import java.sql.Timestamp
import java.time.Instant

@Singleton
class HomeController @Inject()(
    val controllerComponents: ControllerComponents,
    ws: WSClient,
    config: Configuration,
    userRepository: UserRepository
)(implicit ec: ExecutionContext) extends BaseController {
  private val logger = LoggerFactory.getLogger(this.getClass) // SLF4Jロガー

  val clientId = config.get[String]("DISCORD_CLIENT_ID")
  val clientSecret = config.get[String]("DISCORD_CLIENT_SECRET")
  val redirectUri = config.get[String]("REDIRECT_URI")

  def index = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index(request))
  }

  def login = Action { implicit request: Request[AnyContent] =>
    val discordAuthUrl = "https://discord.com/api/oauth2/authorize"
    val params = Map(
      "client_id" -> clientId,
      "redirect_uri" -> redirectUri,
      "response_type" -> "code",
      "scope" -> "identify email"
    )
    val urlWithParams = discordAuthUrl + "?" + params.map { case (k, v) => s"$k=${java.net.URLEncoder.encode(v, "UTF-8")}" }.mkString("&")
    Redirect(urlWithParams)
  }

  def callback(codeOpt: Option[String], errorOpt: Option[String]) = Action.async { implicit request =>
    codeOpt match {
      case Some(code) =>
        // アクセストークンを取得
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
                // ユーザー情報を取得
                ws.url("https://discord.com/api/users/@me")
                  .addHttpHeaders("Authorization" -> s"Bearer $accessToken")
                  .get()
                  .flatMap { userResponse =>
                    val userJson = userResponse.json
                    val discordId = (userJson \ "id").as[String]
                    val username = (userJson \ "username").as[String]
                    // ユーザーが既に存在するかチェック
                    userRepository.findByDiscordId(discordId).flatMap {
                      case Some(user) =>
                        // 既存ユーザーの場合、セッションを開始
                        Future.successful(Redirect(routes.HomeController.index).withSession("user" -> discordId))
                      case None =>
                        // 新規ユーザーの場合、データベースに保存
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
  }

  def logout = Action {
    Redirect(routes.HomeController.index).withNewSession
  }
}

