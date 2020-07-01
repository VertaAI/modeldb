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

case class VersioningPathDatasetDiff (
  components: Option[List[VersioningPathDatasetComponentDiff]] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningPathDatasetDiff.toJson(this)
}

object VersioningPathDatasetDiff {
  def toJson(obj: VersioningPathDatasetDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.components.map(x => JField("components", ((x: List[VersioningPathDatasetComponentDiff]) => JArray(x.map(((x: VersioningPathDatasetComponentDiff) => VersioningPathDatasetComponentDiff.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningPathDatasetDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningPathDatasetDiff(
          // TODO: handle required
          components = fieldsMap.get("components").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningPathDatasetComponentDiff.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
