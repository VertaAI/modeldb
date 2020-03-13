// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.JobStatusEnumJobStatus._
import ai.verta.swagger._public.modeldb.model.JobTypeEnumJobType._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger.client.objects._

case class ModeldbCreateJobResponse (
  job: Option[ModeldbJob] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbCreateJobResponse.toJson(this)
}

object ModeldbCreateJobResponse {
  def toJson(obj: ModeldbCreateJobResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.job.map(x => JField("job", ((x: ModeldbJob) => ModeldbJob.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbCreateJobResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbCreateJobResponse(
          // TODO: handle required
          job = fieldsMap.get("job").map(ModeldbJob.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
