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

case class VersioningQueryDatasetBlob (
  components: Option[List[VersioningQueryDatasetComponentBlob]] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningQueryDatasetBlob.toJson(this)
}

object VersioningQueryDatasetBlob {
  def toJson(obj: VersioningQueryDatasetBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.components.map(x => JField("components", ((x: List[VersioningQueryDatasetComponentBlob]) => JArray(x.map(((x: VersioningQueryDatasetComponentBlob) => VersioningQueryDatasetComponentBlob.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningQueryDatasetBlob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningQueryDatasetBlob(
          // TODO: handle required
          components = fieldsMap.get("components").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningQueryDatasetComponentBlob.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
