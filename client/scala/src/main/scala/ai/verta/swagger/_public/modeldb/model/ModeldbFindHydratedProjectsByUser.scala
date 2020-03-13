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

case class ModeldbFindHydratedProjectsByUser (
  find_projects: Option[ModeldbFindProjects] = None,
  email: Option[String] = None,
  username: Option[String] = None,
  verta_id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbFindHydratedProjectsByUser.toJson(this)
}

object ModeldbFindHydratedProjectsByUser {
  def toJson(obj: ModeldbFindHydratedProjectsByUser): JObject = {
    new JObject(
      List[Option[JField]](
        obj.find_projects.map(x => JField("find_projects", ((x: ModeldbFindProjects) => ModeldbFindProjects.toJson(x))(x))),
        obj.email.map(x => JField("email", JString(x))),
        obj.username.map(x => JField("username", JString(x))),
        obj.verta_id.map(x => JField("verta_id", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbFindHydratedProjectsByUser =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbFindHydratedProjectsByUser(
          // TODO: handle required
          find_projects = fieldsMap.get("find_projects").map(ModeldbFindProjects.fromJson),
          email = fieldsMap.get("email").map(JsonConverter.fromJsonString),
          username = fieldsMap.get("username").map(JsonConverter.fromJsonString),
          verta_id = fieldsMap.get("verta_id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
