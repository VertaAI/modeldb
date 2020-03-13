// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

object DatasetVisibilityEnumDatasetVisibility {
  type DatasetVisibilityEnumDatasetVisibility = String
  val PRIVATE: DatasetVisibilityEnumDatasetVisibility = "PRIVATE"
  val PUBLIC: DatasetVisibilityEnumDatasetVisibility = "PUBLIC"
  val ORG_SCOPED_PUBLIC: DatasetVisibilityEnumDatasetVisibility = "ORG_SCOPED_PUBLIC"

  def toJson(obj: DatasetVisibilityEnumDatasetVisibility): JString = JString(obj)

  def fromJson(v: JValue): DatasetVisibilityEnumDatasetVisibility = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
