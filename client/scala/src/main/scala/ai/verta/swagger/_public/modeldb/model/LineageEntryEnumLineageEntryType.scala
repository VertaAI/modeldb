// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

object LineageEntryEnumLineageEntryType {
  type LineageEntryEnumLineageEntryType = String
  val UNKNOWN: LineageEntryEnumLineageEntryType = "UNKNOWN"
  val EXPERIMENT_RUN: LineageEntryEnumLineageEntryType = "EXPERIMENT_RUN"
  val DATASET_VERSION: LineageEntryEnumLineageEntryType = "DATASET_VERSION"

  def toJson(obj: LineageEntryEnumLineageEntryType): JString = JString(obj)

  def fromJson(v: JValue): LineageEntryEnumLineageEntryType = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
