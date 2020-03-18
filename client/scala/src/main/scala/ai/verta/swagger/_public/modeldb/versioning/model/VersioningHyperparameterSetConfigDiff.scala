// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.versioning.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger._public.modeldb.versioning.model.ProtobufNullValue._
import ai.verta.swagger.client.objects._

case class VersioningHyperparameterSetConfigDiff (
  continuous_a: Option[VersioningContinuousHyperparameterSetConfigBlob] = None,
  continuous_b: Option[VersioningContinuousHyperparameterSetConfigBlob] = None,
  discrete_a: Option[VersioningDiscreteHyperparameterSetConfigBlob] = None,
  discrete_b: Option[VersioningDiscreteHyperparameterSetConfigBlob] = None,
  name: Option[String] = None,
  status: Option[DiffStatusEnumDiffStatus] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningHyperparameterSetConfigDiff.toJson(this)
}

object VersioningHyperparameterSetConfigDiff {
  def toJson(obj: VersioningHyperparameterSetConfigDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.continuous_a.map(x => JField("continuous_a", ((x: VersioningContinuousHyperparameterSetConfigBlob) => VersioningContinuousHyperparameterSetConfigBlob.toJson(x))(x))),
        obj.continuous_b.map(x => JField("continuous_b", ((x: VersioningContinuousHyperparameterSetConfigBlob) => VersioningContinuousHyperparameterSetConfigBlob.toJson(x))(x))),
        obj.discrete_a.map(x => JField("discrete_a", ((x: VersioningDiscreteHyperparameterSetConfigBlob) => VersioningDiscreteHyperparameterSetConfigBlob.toJson(x))(x))),
        obj.discrete_b.map(x => JField("discrete_b", ((x: VersioningDiscreteHyperparameterSetConfigBlob) => VersioningDiscreteHyperparameterSetConfigBlob.toJson(x))(x))),
        obj.name.map(x => JField("name", JString(x))),
        obj.status.map(x => JField("status", ((x: DiffStatusEnumDiffStatus) => DiffStatusEnumDiffStatus.toJson(x))(x)))
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
          continuous_a = fieldsMap.get("continuous_a").map(VersioningContinuousHyperparameterSetConfigBlob.fromJson),
          continuous_b = fieldsMap.get("continuous_b").map(VersioningContinuousHyperparameterSetConfigBlob.fromJson),
          discrete_a = fieldsMap.get("discrete_a").map(VersioningDiscreteHyperparameterSetConfigBlob.fromJson),
          discrete_b = fieldsMap.get("discrete_b").map(VersioningDiscreteHyperparameterSetConfigBlob.fromJson),
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          status = fieldsMap.get("status").map(DiffStatusEnumDiffStatus.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
