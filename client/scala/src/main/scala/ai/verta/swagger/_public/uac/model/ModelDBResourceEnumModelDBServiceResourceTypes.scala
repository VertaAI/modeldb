// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

object ModelDBResourceEnumModelDBServiceResourceTypes {
  type ModelDBResourceEnumModelDBServiceResourceTypes = String
  val UNKNOWN: ModelDBResourceEnumModelDBServiceResourceTypes = "UNKNOWN"
  val ALL: ModelDBResourceEnumModelDBServiceResourceTypes = "ALL"
  val PROJECT: ModelDBResourceEnumModelDBServiceResourceTypes = "PROJECT"
  val EXPERIMENT: ModelDBResourceEnumModelDBServiceResourceTypes = "EXPERIMENT"
  val EXPERIMENT_RUN: ModelDBResourceEnumModelDBServiceResourceTypes = "EXPERIMENT_RUN"
  val DATASET: ModelDBResourceEnumModelDBServiceResourceTypes = "DATASET"
  val DATASET_VERSION: ModelDBResourceEnumModelDBServiceResourceTypes = "DATASET_VERSION"
  val DASHBOARD: ModelDBResourceEnumModelDBServiceResourceTypes = "DASHBOARD"
  val REPOSITORY: ModelDBResourceEnumModelDBServiceResourceTypes = "REPOSITORY"
  val REGISTERED_MODEL: ModelDBResourceEnumModelDBServiceResourceTypes = "REGISTERED_MODEL"

  def toJson(obj: ModelDBResourceEnumModelDBServiceResourceTypes): JString = JString(obj)

  def fromJson(v: JValue): ModelDBResourceEnumModelDBServiceResourceTypes = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
