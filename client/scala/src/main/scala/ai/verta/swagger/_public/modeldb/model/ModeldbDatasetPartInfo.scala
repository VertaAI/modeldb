// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.AuthzActionEnumAuthzServiceActions._
import ai.verta.swagger._public.modeldb.model.CollaboratorTypeEnumCollaboratorType._
import ai.verta.swagger._public.modeldb.model.DatasetTypeEnumDatasetType._
import ai.verta.swagger._public.modeldb.model.DatasetVisibilityEnumDatasetVisibility._
import ai.verta.swagger._public.modeldb.model.EntitiesEnumEntitiesTypes._
import ai.verta.swagger._public.modeldb.model.IdServiceProviderEnumIdServiceProvider._
import ai.verta.swagger._public.modeldb.model.ModelDBActionEnumModelDBServiceActions._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.PathLocationTypeEnumPathLocationType._
import ai.verta.swagger._public.modeldb.model.RoleActionEnumRoleServiceActions._
import ai.verta.swagger._public.modeldb.model.ServiceEnumService._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger._public.modeldb.model.ModeldbProjectVisibility._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.model.UacFlagEnum._
import ai.verta.swagger.client.objects._

case class ModeldbDatasetPartInfo (
  path: Option[String] = None,
  size: Option[String] = None,
  checksum: Option[String] = None,
  last_modified_at_source: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbDatasetPartInfo.toJson(this)
}

object ModeldbDatasetPartInfo {
  def toJson(obj: ModeldbDatasetPartInfo): JObject = {
    new JObject(
      List[Option[JField]](
        obj.path.map(x => JField("path", JString(x))),
        obj.size.map(x => JField("size", JString(x))),
        obj.checksum.map(x => JField("checksum", JString(x))),
        obj.last_modified_at_source.map(x => JField("last_modified_at_source", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbDatasetPartInfo =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbDatasetPartInfo(
          // TODO: handle required
          path = fieldsMap.get("path").map(JsonConverter.fromJsonString),
          size = fieldsMap.get("size").map(JsonConverter.fromJsonString),
          checksum = fieldsMap.get("checksum").map(JsonConverter.fromJsonString),
          last_modified_at_source = fieldsMap.get("last_modified_at_source").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
