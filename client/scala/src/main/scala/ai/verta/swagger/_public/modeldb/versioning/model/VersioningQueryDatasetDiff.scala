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

case class VersioningQueryDatasetDiff (
  components: Option[List[VersioningQueryDatasetComponentDiff]] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningQueryDatasetDiff.toJson(this)
}

object VersioningQueryDatasetDiff {
  def toJson(obj: VersioningQueryDatasetDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.components.map(x => JField("components", ((x: List[VersioningQueryDatasetComponentDiff]) => JArray(x.map(((x: VersioningQueryDatasetComponentDiff) => VersioningQueryDatasetComponentDiff.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningQueryDatasetDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningQueryDatasetDiff(
          // TODO: handle required
          components = fieldsMap.get("components").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningQueryDatasetComponentDiff.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
