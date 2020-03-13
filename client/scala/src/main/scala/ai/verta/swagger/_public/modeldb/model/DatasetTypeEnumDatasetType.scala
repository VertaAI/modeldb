// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

object DatasetTypeEnumDatasetType {
  type DatasetTypeEnumDatasetType = String
  val RAW: DatasetTypeEnumDatasetType = "RAW"
  val PATH: DatasetTypeEnumDatasetType = "PATH"
  val QUERY: DatasetTypeEnumDatasetType = "QUERY"

  def toJson(obj: DatasetTypeEnumDatasetType): JString = JString(obj)

  def fromJson(v: JValue): DatasetTypeEnumDatasetType = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
