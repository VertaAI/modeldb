// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

object DiffStatusEnumDiffStatus {
  type DiffStatusEnumDiffStatus = String
  val UNKNOWN: DiffStatusEnumDiffStatus = "UNKNOWN"
  val ADDED: DiffStatusEnumDiffStatus = "ADDED"
  val DELETED: DiffStatusEnumDiffStatus = "DELETED"
  val MODIFIED: DiffStatusEnumDiffStatus = "MODIFIED"

  def toJson(obj: DiffStatusEnumDiffStatus): JString = JString(obj)

  def fromJson(v: JValue): DiffStatusEnumDiffStatus = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
