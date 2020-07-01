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

case class VersioningDiscreteHyperparameterSetConfigBlob (
  values: Option[List[VersioningHyperparameterValuesConfigBlob]] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningDiscreteHyperparameterSetConfigBlob.toJson(this)
}

object VersioningDiscreteHyperparameterSetConfigBlob {
  def toJson(obj: VersioningDiscreteHyperparameterSetConfigBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.values.map(x => JField("values", ((x: List[VersioningHyperparameterValuesConfigBlob]) => JArray(x.map(((x: VersioningHyperparameterValuesConfigBlob) => VersioningHyperparameterValuesConfigBlob.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningDiscreteHyperparameterSetConfigBlob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningDiscreteHyperparameterSetConfigBlob(
          // TODO: handle required
          values = fieldsMap.get("values").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningHyperparameterValuesConfigBlob.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
