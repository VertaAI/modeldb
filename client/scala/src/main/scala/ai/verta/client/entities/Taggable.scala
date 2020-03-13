package ai.verta.client.entities

import scala.concurrent.ExecutionContext
import scala.util.Try

trait Taggable {
  def addTags(tags: List[String])(implicit ec: ExecutionContext): Try[Unit]

  def delTags(tags: List[String])(implicit ec: ExecutionContext): Try[Unit]

  def getTags()(implicit ec: ExecutionContext): Try[List[String]]
}
