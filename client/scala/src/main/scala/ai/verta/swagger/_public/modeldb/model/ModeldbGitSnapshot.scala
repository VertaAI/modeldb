// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger._public.modeldb.model.ModeldbProjectVisibility._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger.client.objects._

case class ModeldbGitSnapshot (
  filepaths: Option[List[String]] = None,
  repo: Option[String] = None,
  hash: Option[String] = None,
  is_dirty: Option[TernaryEnumTernary] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbGitSnapshot.toJson(this)
}

object ModeldbGitSnapshot {
  def toJson(obj: ModeldbGitSnapshot): JObject = {
    new JObject(
      List[Option[JField]](
        obj.filepaths.map(x => JField("filepaths", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.repo.map(x => JField("repo", JString(x))),
        obj.hash.map(x => JField("hash", JString(x))),
        obj.is_dirty.map(x => JField("is_dirty", ((x: TernaryEnumTernary) => TernaryEnumTernary.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbGitSnapshot =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbGitSnapshot(
          // TODO: handle required
          filepaths = fieldsMap.get("filepaths").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          repo = fieldsMap.get("repo").map(JsonConverter.fromJsonString),
          hash = fieldsMap.get("hash").map(JsonConverter.fromJsonString),
          is_dirty = fieldsMap.get("is_dirty").map(TernaryEnumTernary.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
