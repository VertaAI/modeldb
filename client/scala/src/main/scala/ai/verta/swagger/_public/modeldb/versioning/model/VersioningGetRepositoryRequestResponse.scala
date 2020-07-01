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

case class VersioningGetRepositoryRequestResponse (
  repository: Option[VersioningRepository] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningGetRepositoryRequestResponse.toJson(this)
}

object VersioningGetRepositoryRequestResponse {
  def toJson(obj: VersioningGetRepositoryRequestResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.repository.map(x => JField("repository", ((x: VersioningRepository) => VersioningRepository.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningGetRepositoryRequestResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningGetRepositoryRequestResponse(
          // TODO: handle required
          repository = fieldsMap.get("repository").map(VersioningRepository.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
