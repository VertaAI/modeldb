// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger.client.objects._

case class ModeldbGetVersionedInputResponse (
  versioned_inputs: Option[ModeldbVersioningEntry] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbGetVersionedInputResponse.toJson(this)
}

object ModeldbGetVersionedInputResponse {
  def toJson(obj: ModeldbGetVersionedInputResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.versioned_inputs.map(x => JField("versioned_inputs", ((x: ModeldbVersioningEntry) => ModeldbVersioningEntry.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbGetVersionedInputResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbGetVersionedInputResponse(
          // TODO: handle required
          versioned_inputs = fieldsMap.get("versioned_inputs").map(ModeldbVersioningEntry.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
