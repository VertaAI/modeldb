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

case class VersioningContinuousHyperparameterSetConfigBlob (
  interval_begin: Option[VersioningHyperparameterValuesConfigBlob] = None,
  interval_end: Option[VersioningHyperparameterValuesConfigBlob] = None,
  interval_step: Option[VersioningHyperparameterValuesConfigBlob] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningContinuousHyperparameterSetConfigBlob.toJson(this)
}

object VersioningContinuousHyperparameterSetConfigBlob {
  def toJson(obj: VersioningContinuousHyperparameterSetConfigBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.interval_begin.map(x => JField("interval_begin", ((x: VersioningHyperparameterValuesConfigBlob) => VersioningHyperparameterValuesConfigBlob.toJson(x))(x))),
        obj.interval_end.map(x => JField("interval_end", ((x: VersioningHyperparameterValuesConfigBlob) => VersioningHyperparameterValuesConfigBlob.toJson(x))(x))),
        obj.interval_step.map(x => JField("interval_step", ((x: VersioningHyperparameterValuesConfigBlob) => VersioningHyperparameterValuesConfigBlob.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningContinuousHyperparameterSetConfigBlob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningContinuousHyperparameterSetConfigBlob(
          // TODO: handle required
          interval_begin = fieldsMap.get("interval_begin").map(VersioningHyperparameterValuesConfigBlob.fromJson),
          interval_end = fieldsMap.get("interval_end").map(VersioningHyperparameterValuesConfigBlob.fromJson),
          interval_step = fieldsMap.get("interval_step").map(VersioningHyperparameterValuesConfigBlob.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
