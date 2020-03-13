// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningHyperparameterSetConfigDiff (
  A: Option[List[VersioningHyperparameterSetConfigBlob]] = None,
  B: Option[List[VersioningHyperparameterSetConfigBlob]] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningHyperparameterSetConfigDiff.toJson(this)
}

object VersioningHyperparameterSetConfigDiff {
  def toJson(obj: VersioningHyperparameterSetConfigDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.A.map(x => JField("A", ((x: List[VersioningHyperparameterSetConfigBlob]) => JArray(x.map(((x: VersioningHyperparameterSetConfigBlob) => VersioningHyperparameterSetConfigBlob.toJson(x)))))(x))),
        obj.B.map(x => JField("B", ((x: List[VersioningHyperparameterSetConfigBlob]) => JArray(x.map(((x: VersioningHyperparameterSetConfigBlob) => VersioningHyperparameterSetConfigBlob.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningHyperparameterSetConfigDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningHyperparameterSetConfigDiff(
          // TODO: handle required
          A = fieldsMap.get("A").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningHyperparameterSetConfigBlob.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          B = fieldsMap.get("B").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningHyperparameterSetConfigBlob.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
