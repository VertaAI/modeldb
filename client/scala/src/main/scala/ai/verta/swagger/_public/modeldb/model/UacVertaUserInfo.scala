// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.CollaboratorTypeEnumCollaboratorType._
import ai.verta.swagger._public.modeldb.model.DatasetTypeEnumDatasetType._
import ai.verta.swagger._public.modeldb.model.DatasetVisibilityEnumDatasetVisibility._
import ai.verta.swagger._public.modeldb.model.EntitiesEnumEntitiesTypes._
import ai.verta.swagger._public.modeldb.model.IdServiceProviderEnumIdServiceProvider._
import ai.verta.swagger._public.modeldb.model.ModelDBActionEnumModelDBServiceActions._
import ai.verta.swagger._public.modeldb.model.ModeldbProjectVisibility._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.PathLocationTypeEnumPathLocationType._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.model.ServiceEnumService._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.UacFlagEnum._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class UacVertaUserInfo (
  individual_user: Option[Boolean] = None,
  last_login_timestamp: Option[BigInt] = None,
  publicProfile: Option[UacFlagEnum] = None,
  refresh_timestamp: Option[BigInt] = None,
  user_id: Option[String] = None,
  username: Option[String] = None,
  workspace_id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = UacVertaUserInfo.toJson(this)
}

object UacVertaUserInfo {
  def toJson(obj: UacVertaUserInfo): JObject = {
    new JObject(
      List[Option[JField]](
        obj.individual_user.map(x => JField("individual_user", JBool(x))),
        obj.last_login_timestamp.map(x => JField("last_login_timestamp", JInt(x))),
        obj.publicProfile.map(x => JField("publicProfile", ((x: UacFlagEnum) => UacFlagEnum.toJson(x))(x))),
        obj.refresh_timestamp.map(x => JField("refresh_timestamp", JInt(x))),
        obj.user_id.map(x => JField("user_id", JString(x))),
        obj.username.map(x => JField("username", JString(x))),
        obj.workspace_id.map(x => JField("workspace_id", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacVertaUserInfo =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacVertaUserInfo(
          // TODO: handle required
          individual_user = fieldsMap.get("individual_user").map(JsonConverter.fromJsonBoolean),
          last_login_timestamp = fieldsMap.get("last_login_timestamp").map(JsonConverter.fromJsonInteger),
          publicProfile = fieldsMap.get("publicProfile").map(UacFlagEnum.fromJson),
          refresh_timestamp = fieldsMap.get("refresh_timestamp").map(JsonConverter.fromJsonInteger),
          user_id = fieldsMap.get("user_id").map(JsonConverter.fromJsonString),
          username = fieldsMap.get("username").map(JsonConverter.fromJsonString),
          workspace_id = fieldsMap.get("workspace_id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
