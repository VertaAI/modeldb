// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.versioning.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.versioning.model.RepositoryVisibilityEnumRepositoryVisibility._
import ai.verta.swagger._public.modeldb.versioning.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.versioning.model.VersioningBlobType._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningBlobDiff (
  code: Option[VersioningCodeDiff] = None,
  config: Option[VersioningConfigDiff] = None,
  dataset: Option[VersioningDatasetDiff] = None,
  environment: Option[VersioningEnvironmentDiff] = None,
  location: Option[List[String]] = None,
  status: Option[DiffStatusEnumDiffStatus] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningBlobDiff.toJson(this)
}

object VersioningBlobDiff {
  def toJson(obj: VersioningBlobDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.code.map(x => JField("code", ((x: VersioningCodeDiff) => VersioningCodeDiff.toJson(x))(x))),
        obj.config.map(x => JField("config", ((x: VersioningConfigDiff) => VersioningConfigDiff.toJson(x))(x))),
        obj.dataset.map(x => JField("dataset", ((x: VersioningDatasetDiff) => VersioningDatasetDiff.toJson(x))(x))),
        obj.environment.map(x => JField("environment", ((x: VersioningEnvironmentDiff) => VersioningEnvironmentDiff.toJson(x))(x))),
        obj.location.map(x => JField("location", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.status.map(x => JField("status", ((x: DiffStatusEnumDiffStatus) => DiffStatusEnumDiffStatus.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningBlobDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningBlobDiff(
          // TODO: handle required
          code = fieldsMap.get("code").map(VersioningCodeDiff.fromJson),
          config = fieldsMap.get("config").map(VersioningConfigDiff.fromJson),
          dataset = fieldsMap.get("dataset").map(VersioningDatasetDiff.fromJson),
          environment = fieldsMap.get("environment").map(VersioningEnvironmentDiff.fromJson),
          location = fieldsMap.get("location").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          status = fieldsMap.get("status").map(DiffStatusEnumDiffStatus.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
