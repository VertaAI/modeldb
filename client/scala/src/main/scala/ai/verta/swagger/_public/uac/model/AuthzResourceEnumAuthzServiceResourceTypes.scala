// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

object AuthzResourceEnumAuthzServiceResourceTypes {
  type AuthzResourceEnumAuthzServiceResourceTypes = String
  val UNKNOWN: AuthzResourceEnumAuthzServiceResourceTypes = "UNKNOWN"
  val ALL: AuthzResourceEnumAuthzServiceResourceTypes = "ALL"

  def toJson(obj: AuthzResourceEnumAuthzServiceResourceTypes): JString = JString(obj)

  def fromJson(v: JValue): AuthzResourceEnumAuthzServiceResourceTypes = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
