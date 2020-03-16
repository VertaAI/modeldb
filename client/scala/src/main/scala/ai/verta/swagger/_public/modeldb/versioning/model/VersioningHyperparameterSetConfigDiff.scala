// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningHyperparameterSetConfigDiff (
  status: Option[DiffStatusEnumDiffStatus] = None,
  name: Option[String] = None,
  continuous_a: Option[VersioningContinuousHyperparameterSetConfigBlob] = None,
  discrete_a: Option[VersioningDiscreteHyperparameterSetConfigBlob] = None,
  continuous_b: Option[VersioningContinuousHyperparameterSetConfigBlob] = None,
  discrete_b: Option[VersioningDiscreteHyperparameterSetConfigBlob] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningHyperparameterSetConfigDiff.toJson(this)
}

object VersioningHyperparameterSetConfigDiff {
  def toJson(obj: VersioningHyperparameterSetConfigDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.status.map(x => JField("status", ((x: DiffStatusEnumDiffStatus) => DiffStatusEnumDiffStatus.toJson(x))(x))),
        obj.name.map(x => JField("name", JString(x))),
        obj.continuous_a.map(x => JField("continuous_a", ((x: VersioningContinuousHyperparameterSetConfigBlob) => VersioningContinuousHyperparameterSetConfigBlob.toJson(x))(x))),
        obj.discrete_a.map(x => JField("discrete_a", ((x: VersioningDiscreteHyperparameterSetConfigBlob) => VersioningDiscreteHyperparameterSetConfigBlob.toJson(x))(x))),
        obj.continuous_b.map(x => JField("continuous_b", ((x: VersioningContinuousHyperparameterSetConfigBlob) => VersioningContinuousHyperparameterSetConfigBlob.toJson(x))(x))),
        obj.discrete_b.map(x => JField("discrete_b", ((x: VersioningDiscreteHyperparameterSetConfigBlob) => VersioningDiscreteHyperparameterSetConfigBlob.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningHyperparameterSetConfigDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningHyperparameterSetConfigDiff(
          // TODO: handle required
          status = fieldsMap.get("status").map(DiffStatusEnumDiffStatus.fromJson),
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          continuous_a = fieldsMap.get("continuous_a").map(VersioningContinuousHyperparameterSetConfigBlob.fromJson),
          discrete_a = fieldsMap.get("discrete_a").map(VersioningDiscreteHyperparameterSetConfigBlob.fromJson),
          continuous_b = fieldsMap.get("continuous_b").map(VersioningContinuousHyperparameterSetConfigBlob.fromJson),
          discrete_b = fieldsMap.get("discrete_b").map(VersioningDiscreteHyperparameterSetConfigBlob.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
