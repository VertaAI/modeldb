package ai.verta.client.entities

import scala.concurrent.ExecutionContext
import scala.util.Try

trait Taggable {
  def addTags(tags: List[String])(implicit ec: ExecutionContext): Try[Unit]

  def delTags(tags: List[String])(implicit ec: ExecutionContext): Try[Unit]

  def getTags()(implicit ec: ExecutionContext): Try[List[String]]

  /** Add a tag to this dataset.
   *  @param tag tag to add.
   */
  def addTag(tag: String)(implicit ec: ExecutionContext): Try[Unit] = addTags(List(tag))

  def delTag(tag: String)(implicit ec: ExecutionContext): Try[Unit] = delTags(List(tag))

  def hasTag(tag: String)(implicit ec: ExecutionContext): Try[Boolean] = getTags().map(_.exists(_ == tag))
}
