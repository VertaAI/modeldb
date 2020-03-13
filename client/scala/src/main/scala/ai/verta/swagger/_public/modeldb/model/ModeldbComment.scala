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

case class ModeldbComment (
  id: Option[String] = None,
  user_id: Option[String] = None,
  date_time: Option[String] = None,
  message: Option[String] = None,
  user_info: Option[UacUserInfo] = None,
  verta_id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbComment.toJson(this)
}

object ModeldbComment {
  def toJson(obj: ModeldbComment): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.user_id.map(x => JField("user_id", JString(x))),
        obj.date_time.map(x => JField("date_time", JString(x))),
        obj.message.map(x => JField("message", JString(x))),
        obj.user_info.map(x => JField("user_info", ((x: UacUserInfo) => UacUserInfo.toJson(x))(x))),
        obj.verta_id.map(x => JField("verta_id", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbComment =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbComment(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          user_id = fieldsMap.get("user_id").map(JsonConverter.fromJsonString),
          date_time = fieldsMap.get("date_time").map(JsonConverter.fromJsonString),
          message = fieldsMap.get("message").map(JsonConverter.fromJsonString),
          user_info = fieldsMap.get("user_info").map(UacUserInfo.fromJson),
          verta_id = fieldsMap.get("verta_id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
