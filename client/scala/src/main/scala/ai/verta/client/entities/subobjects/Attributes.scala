package ai.verta.client.entities.subobjects

import ai.verta.client.entities.ExperimentRun
import ai.verta.client.entities.utils.KVHandler
import ai.verta.swagger._public.modeldb.model.{CommonKeyValue, ModeldbLogAttribute}
import ai.verta.swagger.client.ClientSet
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.Success

class Attributes(clientSet: ClientSet, ec: ExecutionContext, run: ExperimentRun) extends mutable.MapLike[String, Any, Attributes] with mutable.Map[String, Any] {
  implicit val _ec = ec

  // add SO reference
  override protected[this] def newBuilder: mutable.Builder[(String, Any), Attributes] = ???

  override def get(key: String): Option[Any] = seq.get(key)

  override def +=(kv: (String, Any)): Attributes.this.type = {
    clientSet.experimentRunService.logAttribute(ModeldbLogAttribute(
      id = run.run.id,
      attribute = Some(CommonKeyValue(
        key = Some(kv._1),
        value = Some(KVHandler.convertFromAny(kv._2, s"unknown type for attribute ${kv._1}: ${kv._2.toString} (${kv._2.getClass.toString})").get)
      ))
    ))
      .get
    this
  }

  override def -=(key: String): Attributes.this.type = throw new NotImplementedException

  override def empty: Attributes = new Attributes(clientSet, ec, run)

  override def iterator: Iterator[(String, Any)] = seq.iterator

  override def seq: mutable.Map[String, Any] =
    mutable.Map(
      clientSet.experimentRunService.getExperimentRunAttributes(run.run.id.get, Nil, true)
        .flatMap(_.attributes match {
          case Some(x) => Success(x)
          case None => Success(Nil)
        })
        .flatMap(KVHandler.kvListToMap)
        .get.toSeq: _*
    )
}
