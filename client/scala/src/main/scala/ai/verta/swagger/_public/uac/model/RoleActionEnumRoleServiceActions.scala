// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

object RoleActionEnumRoleServiceActions {
  type RoleActionEnumRoleServiceActions = String
  val UNKNOWN: RoleActionEnumRoleServiceActions = "UNKNOWN"
  val ALL: RoleActionEnumRoleServiceActions = "ALL"
  val GET_BY_ID: RoleActionEnumRoleServiceActions = "GET_BY_ID"
  val GET_BY_NAME: RoleActionEnumRoleServiceActions = "GET_BY_NAME"
  val CREATE: RoleActionEnumRoleServiceActions = "CREATE"
  val UPDATE: RoleActionEnumRoleServiceActions = "UPDATE"
  val LIST: RoleActionEnumRoleServiceActions = "LIST"
  val DELETE: RoleActionEnumRoleServiceActions = "DELETE"

  def toJson(obj: RoleActionEnumRoleServiceActions): JString = JString(obj)

  def fromJson(v: JValue): RoleActionEnumRoleServiceActions = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
