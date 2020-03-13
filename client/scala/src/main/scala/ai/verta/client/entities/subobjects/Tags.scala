package ai.verta.client.entities.subobjects

import ai.verta.client.entities.Taggable
import ai.verta.swagger.client.ClientSet

import scala.collection.mutable
import scala.concurrent.ExecutionContext

class Tags(clientSet: ClientSet, ec: ExecutionContext, base: Taggable) extends mutable.SetLike[String, Tags] with mutable.Set[String] {
  implicit val _ec = ec

  override protected[this] def newBuilder: mutable.Builder[String, Tags] = ???

  override def empty: Tags = new Tags(clientSet, ec, base)

  override def +=(elem: String): Tags.this.type = {
    base.addTags(List(elem))
      .get
    this
  }

  override def -=(elem: String): Tags.this.type = {
    base.delTags(List(elem))
      .get
    this
  }

  override def contains(elem: String): Boolean =
    base.getTags().get.contains(elem)

  override def iterator: Iterator[String] =
    base.getTags().get.iterator
}
