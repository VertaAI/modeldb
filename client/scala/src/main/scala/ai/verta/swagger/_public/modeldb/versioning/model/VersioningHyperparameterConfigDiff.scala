// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningHyperparameterConfigDiff (
  status: Option[DiffStatusEnumDiffStatus] = None,
  name: Option[String] = None,
  A: Option[VersioningHyperparameterValuesConfigBlob] = None,
  B: Option[VersioningHyperparameterValuesConfigBlob] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningHyperparameterConfigDiff.toJson(this)
}

object VersioningHyperparameterConfigDiff {
  def toJson(obj: VersioningHyperparameterConfigDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.status.map(x => JField("status", ((x: DiffStatusEnumDiffStatus) => DiffStatusEnumDiffStatus.toJson(x))(x))),
        obj.name.map(x => JField("name", JString(x))),
        obj.A.map(x => JField("A", ((x: VersioningHyperparameterValuesConfigBlob) => VersioningHyperparameterValuesConfigBlob.toJson(x))(x))),
        obj.B.map(x => JField("B", ((x: VersioningHyperparameterValuesConfigBlob) => VersioningHyperparameterValuesConfigBlob.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningHyperparameterConfigDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningHyperparameterConfigDiff(
          // TODO: handle required
          status = fieldsMap.get("status").map(DiffStatusEnumDiffStatus.fromJson),
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          A = fieldsMap.get("A").map(VersioningHyperparameterValuesConfigBlob.fromJson),
          B = fieldsMap.get("B").map(VersioningHyperparameterValuesConfigBlob.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
