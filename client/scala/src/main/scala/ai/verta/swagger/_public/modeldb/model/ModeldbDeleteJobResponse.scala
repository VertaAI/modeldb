// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.JobStatusEnumJobStatus._
import ai.verta.swagger._public.modeldb.model.JobTypeEnumJobType._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger.client.objects._

case class ModeldbDeleteJobResponse (
  status: Option[Boolean] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbDeleteJobResponse.toJson(this)
}

object ModeldbDeleteJobResponse {
  def toJson(obj: ModeldbDeleteJobResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.status.map(x => JField("status", JBool(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbDeleteJobResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbDeleteJobResponse(
          // TODO: handle required
          status = fieldsMap.get("status").map(JsonConverter.fromJsonBoolean)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
