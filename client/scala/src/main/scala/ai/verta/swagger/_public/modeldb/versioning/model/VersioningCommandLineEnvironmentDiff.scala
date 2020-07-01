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

case class VersioningCommandLineEnvironmentDiff (
  A: Option[List[String]] = None,
  B: Option[List[String]] = None,
  C: Option[List[String]] = None,
  status: Option[DiffStatusEnumDiffStatus] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningCommandLineEnvironmentDiff.toJson(this)
}

object VersioningCommandLineEnvironmentDiff {
  def toJson(obj: VersioningCommandLineEnvironmentDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.A.map(x => JField("A", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.B.map(x => JField("B", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.C.map(x => JField("C", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.status.map(x => JField("status", ((x: DiffStatusEnumDiffStatus) => DiffStatusEnumDiffStatus.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningCommandLineEnvironmentDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningCommandLineEnvironmentDiff(
          // TODO: handle required
          A = fieldsMap.get("A").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          B = fieldsMap.get("B").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          C = fieldsMap.get("C").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          status = fieldsMap.get("status").map(DiffStatusEnumDiffStatus.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
