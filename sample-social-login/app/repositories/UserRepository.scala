package repositories

import java.sql.Timestamp
import javax.inject.{Inject, Singleton}
import models.User
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.concurrent.{Future, ExecutionContext}

@Singleton
class UserRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  private class UserTable(tag: Tag) extends Table[User](tag, "User") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def discordId = column[String]("discord_id")
    def createdAt = column[Timestamp]("created_at")
    def updatedAt = column[Timestamp]("updated_at")

    def * = (id.?, discordId, createdAt.?, updatedAt.?) <> ((User.apply _).tupled, User.unapply)
  }

  private val users = TableQuery[UserTable]

  def findByDiscordId(discordId: String): Future[Option[User]] = db.run {
    users.filter(_.discordId === discordId).result.headOption
  }

  def create(user: User): Future[User] = db.run {
    (users returning users.map(_.id) into ((user, id) => user.copy(id = Some(id)))) += user
  }
}

