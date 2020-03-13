// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.JobStatusEnumJobStatus._
import ai.verta.swagger._public.modeldb.model.JobTypeEnumJobType._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger.client.objects._

case class ModeldbUpdateJobResponse (
  job: Option[ModeldbJob] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbUpdateJobResponse.toJson(this)
}

object ModeldbUpdateJobResponse {
  def toJson(obj: ModeldbUpdateJobResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.job.map(x => JField("job", ((x: ModeldbJob) => ModeldbJob.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbUpdateJobResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbUpdateJobResponse(
          // TODO: handle required
          job = fieldsMap.get("job").map(ModeldbJob.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
