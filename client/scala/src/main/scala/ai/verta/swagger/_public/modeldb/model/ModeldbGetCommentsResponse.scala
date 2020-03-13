// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.IdServiceProviderEnumIdServiceProvider._
import ai.verta.swagger._public.modeldb.model.UacFlagEnum._
import ai.verta.swagger.client.objects._

case class ModeldbGetCommentsResponse (
  comments: Option[List[ModeldbComment]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbGetCommentsResponse.toJson(this)
}

object ModeldbGetCommentsResponse {
  def toJson(obj: ModeldbGetCommentsResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.comments.map(x => JField("comments", ((x: List[ModeldbComment]) => JArray(x.map(((x: ModeldbComment) => ModeldbComment.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbGetCommentsResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbGetCommentsResponse(
          // TODO: handle required
          comments = fieldsMap.get("comments").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbComment.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
