// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

object UacFlagEnum {
  type UacFlagEnum = String
  val UNDEFINED: UacFlagEnum = "UNDEFINED"
  val TRUE: UacFlagEnum = "TRUE"
  val FALSE: UacFlagEnum = "FALSE"

  def toJson(obj: UacFlagEnum): JString = JString(obj)

  def fromJson(v: JValue): UacFlagEnum = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
