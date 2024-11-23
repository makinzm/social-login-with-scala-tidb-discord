package models

import java.sql.Timestamp

case class Note(
  id: Option[Long],
  userId: Long,
  content: String,
  createdAt: Option[Timestamp],
  updatedAt: Option[Timestamp]
)

