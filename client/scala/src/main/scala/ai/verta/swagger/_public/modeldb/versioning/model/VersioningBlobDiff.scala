// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningBlobDiff (
  location: Option[List[String]] = None,
  status: Option[DiffStatusEnumDiffStatus] = None,
  dataset: Option[VersioningDatasetDiff] = None,
  environment: Option[VersioningEnvironmentDiff] = None,
  code: Option[VersioningCodeDiff] = None,
  config: Option[VersioningConfigDiff] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningBlobDiff.toJson(this)
}

object VersioningBlobDiff {
  def toJson(obj: VersioningBlobDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.location.map(x => JField("location", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.status.map(x => JField("status", ((x: DiffStatusEnumDiffStatus) => DiffStatusEnumDiffStatus.toJson(x))(x))),
        obj.dataset.map(x => JField("dataset", ((x: VersioningDatasetDiff) => VersioningDatasetDiff.toJson(x))(x))),
        obj.environment.map(x => JField("environment", ((x: VersioningEnvironmentDiff) => VersioningEnvironmentDiff.toJson(x))(x))),
        obj.code.map(x => JField("code", ((x: VersioningCodeDiff) => VersioningCodeDiff.toJson(x))(x))),
        obj.config.map(x => JField("config", ((x: VersioningConfigDiff) => VersioningConfigDiff.toJson(x))(x)))
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
          location = fieldsMap.get("location").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          status = fieldsMap.get("status").map(DiffStatusEnumDiffStatus.fromJson),
          dataset = fieldsMap.get("dataset").map(VersioningDatasetDiff.fromJson),
          environment = fieldsMap.get("environment").map(VersioningEnvironmentDiff.fromJson),
          code = fieldsMap.get("code").map(VersioningCodeDiff.fromJson),
          config = fieldsMap.get("config").map(VersioningConfigDiff.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
