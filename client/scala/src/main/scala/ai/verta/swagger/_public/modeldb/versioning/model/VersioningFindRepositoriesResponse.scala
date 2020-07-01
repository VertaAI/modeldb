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

case class VersioningFindRepositoriesResponse (
  repositories: Option[List[VersioningRepository]] = None,
  total_records: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningFindRepositoriesResponse.toJson(this)
}

object VersioningFindRepositoriesResponse {
  def toJson(obj: VersioningFindRepositoriesResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.repositories.map(x => JField("repositories", ((x: List[VersioningRepository]) => JArray(x.map(((x: VersioningRepository) => VersioningRepository.toJson(x)))))(x))),
        obj.total_records.map(x => JField("total_records", JInt(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningFindRepositoriesResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningFindRepositoriesResponse(
          // TODO: handle required
          repositories = fieldsMap.get("repositories").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningRepository.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          total_records = fieldsMap.get("total_records").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
