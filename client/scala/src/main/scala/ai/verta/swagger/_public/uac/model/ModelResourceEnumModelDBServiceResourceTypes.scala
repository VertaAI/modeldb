// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

object ModelResourceEnumModelDBServiceResourceTypes {
  type ModelResourceEnumModelDBServiceResourceTypes = String
  val UNKNOWN: ModelResourceEnumModelDBServiceResourceTypes = "UNKNOWN"
  val ALL: ModelResourceEnumModelDBServiceResourceTypes = "ALL"
  val PROJECT: ModelResourceEnumModelDBServiceResourceTypes = "PROJECT"
  val EXPERIMENT: ModelResourceEnumModelDBServiceResourceTypes = "EXPERIMENT"
  val EXPERIMENT_RUN: ModelResourceEnumModelDBServiceResourceTypes = "EXPERIMENT_RUN"
  val DATASET: ModelResourceEnumModelDBServiceResourceTypes = "DATASET"
  val DATASET_VERSION: ModelResourceEnumModelDBServiceResourceTypes = "DATASET_VERSION"
  val DASHBOARD: ModelResourceEnumModelDBServiceResourceTypes = "DASHBOARD"

  def toJson(obj: ModelResourceEnumModelDBServiceResourceTypes): JString = JString(obj)

  def fromJson(v: JValue): ModelResourceEnumModelDBServiceResourceTypes = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
