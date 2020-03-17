// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningHyperparameterConfigBlob (
  name: Option[String] = None,
  value: Option[VersioningHyperparameterValuesConfigBlob] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningHyperparameterConfigBlob.toJson(this)
}

object VersioningHyperparameterConfigBlob {
  def toJson(obj: VersioningHyperparameterConfigBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.name.map(x => JField("name", JString(x))),
        obj.value.map(x => JField("value", ((x: VersioningHyperparameterValuesConfigBlob) => VersioningHyperparameterValuesConfigBlob.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningHyperparameterConfigBlob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningHyperparameterConfigBlob(
          // TODO: handle required
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          value = fieldsMap.get("value").map(VersioningHyperparameterValuesConfigBlob.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
