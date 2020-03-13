// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

object WorkspaceTypeEnumWorkspaceType {
  type WorkspaceTypeEnumWorkspaceType = String
  val UNKNOWN: WorkspaceTypeEnumWorkspaceType = "UNKNOWN"
  val ORGANIZATION: WorkspaceTypeEnumWorkspaceType = "ORGANIZATION"
  val USER: WorkspaceTypeEnumWorkspaceType = "USER"

  def toJson(obj: WorkspaceTypeEnumWorkspaceType): JString = JString(obj)

  def fromJson(v: JValue): WorkspaceTypeEnumWorkspaceType = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
