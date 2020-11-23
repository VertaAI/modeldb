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

case class VersioningVersionEnvironmentBlob (
  major: Option[BigInt] = None,
  minor: Option[BigInt] = None,
  patch: Option[BigInt] = None,
  suffix: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningVersionEnvironmentBlob.toJson(this)
}

object VersioningVersionEnvironmentBlob {
  def toJson(obj: VersioningVersionEnvironmentBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.major.map(x => JField("major", JInt(x))),
        obj.minor.map(x => JField("minor", JInt(x))),
        obj.patch.map(x => JField("patch", JInt(x))),
        obj.suffix.map(x => JField("suffix", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningVersionEnvironmentBlob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningVersionEnvironmentBlob(
          // TODO: handle required
          major = fieldsMap.get("major").map(JsonConverter.fromJsonInteger),
          minor = fieldsMap.get("minor").map(JsonConverter.fromJsonInteger),
          patch = fieldsMap.get("patch").map(JsonConverter.fromJsonInteger),
          suffix = fieldsMap.get("suffix").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
