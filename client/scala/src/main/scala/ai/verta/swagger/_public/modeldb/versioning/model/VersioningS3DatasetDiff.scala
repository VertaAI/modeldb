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

case class VersioningS3DatasetDiff (
  components: Option[List[VersioningS3DatasetComponentDiff]] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningS3DatasetDiff.toJson(this)
}

object VersioningS3DatasetDiff {
  def toJson(obj: VersioningS3DatasetDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.components.map(x => JField("components", ((x: List[VersioningS3DatasetComponentDiff]) => JArray(x.map(((x: VersioningS3DatasetComponentDiff) => VersioningS3DatasetComponentDiff.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningS3DatasetDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningS3DatasetDiff(
          // TODO: handle required
          components = fieldsMap.get("components").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningS3DatasetComponentDiff.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
