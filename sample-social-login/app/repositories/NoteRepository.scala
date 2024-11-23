package repositories

import java.sql.Timestamp
import javax.inject.{Inject, Singleton}
import models.Note
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.concurrent.{Future, ExecutionContext}

@Singleton
class NoteRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  private class NoteTable(tag: Tag) extends Table[Note](tag, "Note") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def userId = column[Long]("user_id")
    def content = column[String]("content")
    def createdAt = column[Timestamp]("created_at")
    def updatedAt = column[Timestamp]("updated_at")

    def * = (id.?, userId, content, createdAt.?, updatedAt.?) <> ((Note.apply _).tupled, Note.unapply)
  }

  private val notes = TableQuery[NoteTable]

  def create(note: Note): Future[Note] = db.run {
    (notes returning notes.map(_.id) into ((note, id) => note.copy(id = Some(id)))) += note
  }

  def listByUser(userId: Long): Future[Seq[Note]] = db.run {
    notes.filter(_.userId === userId).result
  }
}

