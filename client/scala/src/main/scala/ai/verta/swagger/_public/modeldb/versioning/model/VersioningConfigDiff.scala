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

case class VersioningConfigDiff (
  hyperparameter_set: Option[List[VersioningHyperparameterSetConfigDiff]] = None,
  hyperparameters: Option[List[VersioningHyperparameterConfigDiff]] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningConfigDiff.toJson(this)
}

object VersioningConfigDiff {
  def toJson(obj: VersioningConfigDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.hyperparameter_set.map(x => JField("hyperparameter_set", ((x: List[VersioningHyperparameterSetConfigDiff]) => JArray(x.map(((x: VersioningHyperparameterSetConfigDiff) => VersioningHyperparameterSetConfigDiff.toJson(x)))))(x))),
        obj.hyperparameters.map(x => JField("hyperparameters", ((x: List[VersioningHyperparameterConfigDiff]) => JArray(x.map(((x: VersioningHyperparameterConfigDiff) => VersioningHyperparameterConfigDiff.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningConfigDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningConfigDiff(
          // TODO: handle required
          hyperparameter_set = fieldsMap.get("hyperparameter_set").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningHyperparameterSetConfigDiff.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          hyperparameters = fieldsMap.get("hyperparameters").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningHyperparameterConfigDiff.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
