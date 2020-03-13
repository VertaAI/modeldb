package ai.verta.client.entities.subobjects

import ai.verta.client.entities.ExperimentRun
import ai.verta.client.entities.utils.KVHandler
import ai.verta.swagger._public.modeldb.model.{CommonKeyValue, ModeldbLogHyperparameter}
import ai.verta.swagger.client.ClientSet
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.Success

class Hyperparameters(clientSet: ClientSet, ec: ExecutionContext, run: ExperimentRun) extends mutable.MapLike[String, Any, Hyperparameters] with mutable.Map[String, Any] {
  implicit val _ec = ec

  // add SO reference
  override protected[this] def newBuilder: mutable.Builder[(String, Any), Hyperparameters] = ???

  override def get(key: String): Option[Any] = seq.get(key)

  override def +=(kv: (String, Any)): Hyperparameters.this.type = {
    clientSet.experimentRunService.logHyperparameter(ModeldbLogHyperparameter(
      id = run.run.id,
      hyperparameter = Some(CommonKeyValue(
        key = Some(kv._1),
        value = Some(KVHandler.convertFromAny(kv._2, s"unknown type for hyperparameter ${kv._1}: ${kv._2.toString} (${kv._2.getClass.toString})").get)
      ))
    ))
      .get
    this
  }

  override def -=(key: String): Hyperparameters.this.type = throw new NotImplementedException

  override def empty: Hyperparameters = new Hyperparameters(clientSet, ec, run)

  override def iterator: Iterator[(String, Any)] = seq.iterator

  override def seq: mutable.Map[String, Any] =
    mutable.Map(
      clientSet.experimentRunService.getHyperparameters(run.run.id.get)
        .flatMap(_.hyperparameters match {
          case Some(x) => Success(x)
          case None => Success(Nil)
        })
        .flatMap(KVHandler.kvListToMap)
        .get.toSeq: _*
    )
}
