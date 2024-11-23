package models

import java.sql.Timestamp

case class User(
  id: Option[Long],
  discordId: String,
  username: String,
  createdAt: Option[Timestamp],
  updatedAt: Option[Timestamp]
)

