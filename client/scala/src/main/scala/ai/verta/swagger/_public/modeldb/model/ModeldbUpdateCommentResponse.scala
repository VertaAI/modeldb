// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.IdServiceProviderEnumIdServiceProvider._
import ai.verta.swagger._public.modeldb.model.UacFlagEnum._
import ai.verta.swagger.client.objects._

case class ModeldbUpdateCommentResponse (
  comment: Option[ModeldbComment] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbUpdateCommentResponse.toJson(this)
}

object ModeldbUpdateCommentResponse {
  def toJson(obj: ModeldbUpdateCommentResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.comment.map(x => JField("comment", ((x: ModeldbComment) => ModeldbComment.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbUpdateCommentResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbUpdateCommentResponse(
          // TODO: handle required
          comment = fieldsMap.get("comment").map(ModeldbComment.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
