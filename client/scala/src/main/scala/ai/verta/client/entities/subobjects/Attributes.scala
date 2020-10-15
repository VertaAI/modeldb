package ai.verta.client.entities.subobjects

import ai.verta.client.entities.ExperimentRun
import ai.verta.client.entities.utils.{KVHandler, ValueType}
import ai.verta.swagger._public.modeldb.model.{CommonKeyValue, ModeldbLogAttribute}
import ai.verta.swagger.client.ClientSet
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.Success

class Attributes(clientSet: ClientSet, ec: ExecutionContext, run: ExperimentRun) extends mutable.MapLike[String, ValueType, Attributes] with mutable.Map[String, ValueType] {
  implicit val _ec = ec

  // add SO reference
  override protected[this] def newBuilder: mutable.Builder[(String, ValueType), Attributes] = ???

  override def get(key: String): Option[ValueType] = seq.get(key)

  override def +=(kv: (String, ValueType)): Attributes.this.type = {
    clientSet.experimentRunService.ExperimentRunService_logAttribute(ModeldbLogAttribute(
      id = run.run.id,
      attribute = Some(CommonKeyValue(
        key = Some(kv._1),
        value = Some(KVHandler.convertFromValueType(kv._2, s"unknown type for attribute ${kv._1}: ${kv._2.toString} (${kv._2.getClass.toString})").get)
      ))
    ))
      .get
    this
  }

  override def -=(key: String): Attributes.this.type = throw new NotImplementedException

  override def empty: Attributes = new Attributes(clientSet, ec, run)

  override def iterator: Iterator[(String, ValueType)] = seq.iterator

  override def seq: mutable.Map[String, ValueType] =
    mutable.Map(
      clientSet.experimentRunService.ExperimentRunService_getExperimentRunAttributes(id = run.run.id, attribute_keys = None, get_all = Some(true))
        .flatMap(_.attributes match {
          case Some(x) => Success(x)
          case None => Success(Nil)
        })
        .flatMap(KVHandler.kvListToMap)
        .get.toSeq: _*
    )
}
