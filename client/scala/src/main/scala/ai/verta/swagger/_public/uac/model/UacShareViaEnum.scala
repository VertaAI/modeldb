// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

object UacShareViaEnum {
  type UacShareViaEnum = String
  val USER_ID: UacShareViaEnum = "USER_ID"
  val EMAIL_ID: UacShareViaEnum = "EMAIL_ID"
  val USERNAME: UacShareViaEnum = "USERNAME"

  def toJson(obj: UacShareViaEnum): JString = JString(obj)

  def fromJson(v: JValue): UacShareViaEnum = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
