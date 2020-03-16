// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningConfigDiff (
  hyperparameter_set: Option[VersioningHyperparameterSetConfigDiff] = None,
  hyperparameters: Option[VersioningHyperparameterConfigDiff] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningConfigDiff.toJson(this)
}

object VersioningConfigDiff {
  def toJson(obj: VersioningConfigDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.hyperparameter_set.map(x => JField("hyperparameter_set", ((x: VersioningHyperparameterSetConfigDiff) => VersioningHyperparameterSetConfigDiff.toJson(x))(x))),
        obj.hyperparameters.map(x => JField("hyperparameters", ((x: VersioningHyperparameterConfigDiff) => VersioningHyperparameterConfigDiff.toJson(x))(x)))
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
          hyperparameter_set = fieldsMap.get("hyperparameter_set").map(VersioningHyperparameterSetConfigDiff.fromJson),
          hyperparameters = fieldsMap.get("hyperparameters").map(VersioningHyperparameterConfigDiff.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
