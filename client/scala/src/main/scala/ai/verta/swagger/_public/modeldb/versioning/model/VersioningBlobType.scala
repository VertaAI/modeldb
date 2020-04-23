// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

object VersioningBlobType {
  type VersioningBlobType = String
  val UNKNOWN: VersioningBlobType = "UNKNOWN"
  val DATASET_BLOB: VersioningBlobType = "DATASET_BLOB"
  val ENVIRONMENT_BLOB: VersioningBlobType = "ENVIRONMENT_BLOB"
  val CODE_BLOB: VersioningBlobType = "CODE_BLOB"
  val CONFIG_BLOB: VersioningBlobType = "CONFIG_BLOB"

  def toJson(obj: VersioningBlobType): JString = JString(obj)

  def fromJson(v: JValue): VersioningBlobType = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
