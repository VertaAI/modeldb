package ai.verta.client.entities.subobjects

import ai.verta.client.entities.ExperimentRun
import ai.verta.client.entities.utils.{KVHandler, ValueType}
import ai.verta.swagger._public.modeldb.model.{CommonKeyValue, ModeldbLogHyperparameter}
import ai.verta.swagger.client.ClientSet
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.Success

class Hyperparameters(clientSet: ClientSet, ec: ExecutionContext, run: ExperimentRun) extends mutable.MapLike[String, ValueType, Hyperparameters] with mutable.Map[String, ValueType] {
  implicit val _ec = ec

  // add SO reference
  override protected[this] def newBuilder: mutable.Builder[(String, ValueType), Hyperparameters] = ???

  override def get(key: String): Option[ValueType] = seq.get(key)

  override def +=(kv: (String, ValueType)): Hyperparameters.this.type = {
    clientSet.experimentRunService.ExperimentRunService_logHyperparameter(ModeldbLogHyperparameter(
      id = run.run.id,
      hyperparameter = Some(CommonKeyValue(
        key = Some(kv._1),
        value = Some(KVHandler.convertFromValueType(kv._2, s"unknown type for hyperparameter ${kv._1}: ${kv._2.toString} (${kv._2.getClass.toString})").get)
      ))
    ))
      .get
    this
  }

  override def -=(key: String): Hyperparameters.this.type = throw new NotImplementedException

  override def empty: Hyperparameters = new Hyperparameters(clientSet, ec, run)

  override def iterator: Iterator[(String, ValueType)] = seq.iterator

  override def seq: mutable.Map[String, ValueType] =
    mutable.Map(
      clientSet.experimentRunService.ExperimentRunService_getHyperparameters(run.run.id)
        .flatMap(_.hyperparameters match {
          case Some(x) => Success(x)
          case None => Success(Nil)
        })
        .flatMap(KVHandler.kvListToMap)
        .get.toSeq: _*
    )
}
