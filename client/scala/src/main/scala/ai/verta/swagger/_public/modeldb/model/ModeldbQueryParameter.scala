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

case class ModeldbQueryParameter (
  parameter_name: Option[String] = None,
  parameter_type: Option[ValueTypeEnumValueType] = None,
  value: Option[GenericObject] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbQueryParameter.toJson(this)
}

object ModeldbQueryParameter {
  def toJson(obj: ModeldbQueryParameter): JObject = {
    new JObject(
      List[Option[JField]](
        obj.parameter_name.map(x => JField("parameter_name", JString(x))),
        obj.parameter_type.map(x => JField("parameter_type", ((x: ValueTypeEnumValueType) => ValueTypeEnumValueType.toJson(x))(x))),
        obj.value.map(x => JField("value", ((x: GenericObject) => x.toJson())(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbQueryParameter =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbQueryParameter(
          // TODO: handle required
          parameter_name = fieldsMap.get("parameter_name").map(JsonConverter.fromJsonString),
          parameter_type = fieldsMap.get("parameter_type").map(ValueTypeEnumValueType.fromJson),
          value = fieldsMap.get("value").map(GenericObject.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
