// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

object TernaryEnumTernary {
  type TernaryEnumTernary = String
  val UNKNOWN: TernaryEnumTernary = "UNKNOWN"
  val TRUE: TernaryEnumTernary = "TRUE"
  val FALSE: TernaryEnumTernary = "FALSE"

  def toJson(obj: TernaryEnumTernary): JString = JString(obj)

  def fromJson(v: JValue): TernaryEnumTernary = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
