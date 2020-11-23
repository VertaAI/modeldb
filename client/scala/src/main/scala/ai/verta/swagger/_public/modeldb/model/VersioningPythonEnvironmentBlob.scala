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

case class VersioningPythonEnvironmentBlob (
  constraints: Option[List[VersioningPythonRequirementEnvironmentBlob]] = None,
  requirements: Option[List[VersioningPythonRequirementEnvironmentBlob]] = None,
  version: Option[VersioningVersionEnvironmentBlob] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningPythonEnvironmentBlob.toJson(this)
}

object VersioningPythonEnvironmentBlob {
  def toJson(obj: VersioningPythonEnvironmentBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.constraints.map(x => JField("constraints", ((x: List[VersioningPythonRequirementEnvironmentBlob]) => JArray(x.map(((x: VersioningPythonRequirementEnvironmentBlob) => VersioningPythonRequirementEnvironmentBlob.toJson(x)))))(x))),
        obj.requirements.map(x => JField("requirements", ((x: List[VersioningPythonRequirementEnvironmentBlob]) => JArray(x.map(((x: VersioningPythonRequirementEnvironmentBlob) => VersioningPythonRequirementEnvironmentBlob.toJson(x)))))(x))),
        obj.version.map(x => JField("version", ((x: VersioningVersionEnvironmentBlob) => VersioningVersionEnvironmentBlob.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningPythonEnvironmentBlob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningPythonEnvironmentBlob(
          // TODO: handle required
          constraints = fieldsMap.get("constraints").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningPythonRequirementEnvironmentBlob.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          requirements = fieldsMap.get("requirements").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningPythonRequirementEnvironmentBlob.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          version = fieldsMap.get("version").map(VersioningVersionEnvironmentBlob.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
