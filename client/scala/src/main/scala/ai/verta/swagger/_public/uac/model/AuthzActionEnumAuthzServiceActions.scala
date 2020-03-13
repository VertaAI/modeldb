// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

object AuthzActionEnumAuthzServiceActions {
  type AuthzActionEnumAuthzServiceActions = String
  val UNKNOWN: AuthzActionEnumAuthzServiceActions = "UNKNOWN"
  val ALL: AuthzActionEnumAuthzServiceActions = "ALL"
  val IS_ALLOWED: AuthzActionEnumAuthzServiceActions = "IS_ALLOWED"
  val GET: AuthzActionEnumAuthzServiceActions = "GET"

  def toJson(obj: AuthzActionEnumAuthzServiceActions): JString = JString(obj)

  def fromJson(v: JValue): AuthzActionEnumAuthzServiceActions = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
