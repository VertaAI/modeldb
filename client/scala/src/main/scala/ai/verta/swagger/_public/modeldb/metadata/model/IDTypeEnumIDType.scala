// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.metadata.model

import scala.util.Try

import net.liftweb.json._

object IDTypeEnumIDType {
  type IDTypeEnumIDType = String
  val UNKNOWN: IDTypeEnumIDType = "UNKNOWN"
  val VERSIONING_REPOSITORY: IDTypeEnumIDType = "VERSIONING_REPOSITORY"
  val VERSIONING_COMMIT: IDTypeEnumIDType = "VERSIONING_COMMIT"
  val VERSIONING_REPO_COMMIT_BLOB: IDTypeEnumIDType = "VERSIONING_REPO_COMMIT_BLOB"
  val VERSIONING_REPO_COMMIT: IDTypeEnumIDType = "VERSIONING_REPO_COMMIT"

  def toJson(obj: IDTypeEnumIDType): JString = JString(obj)

  def fromJson(v: JValue): IDTypeEnumIDType = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
