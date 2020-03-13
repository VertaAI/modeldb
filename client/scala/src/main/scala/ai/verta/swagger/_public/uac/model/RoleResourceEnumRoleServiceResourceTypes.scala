// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

object RoleResourceEnumRoleServiceResourceTypes {
  type RoleResourceEnumRoleServiceResourceTypes = String
  val UNKNOWN: RoleResourceEnumRoleServiceResourceTypes = "UNKNOWN"
  val ALL: RoleResourceEnumRoleServiceResourceTypes = "ALL"
  val ROLE: RoleResourceEnumRoleServiceResourceTypes = "ROLE"
  val ROLE_BINDING: RoleResourceEnumRoleServiceResourceTypes = "ROLE_BINDING"

  def toJson(obj: RoleResourceEnumRoleServiceResourceTypes): JString = JString(obj)

  def fromJson(v: JValue): RoleResourceEnumRoleServiceResourceTypes = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
