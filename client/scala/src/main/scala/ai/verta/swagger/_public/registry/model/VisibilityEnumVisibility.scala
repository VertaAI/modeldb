// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.registry.model

import scala.util.Try

import net.liftweb.json._

object VisibilityEnumVisibility {
  type VisibilityEnumVisibility = String
  val PRIVATE: VisibilityEnumVisibility = "PRIVATE"
  val PUBLIC: VisibilityEnumVisibility = "PUBLIC"
  val ORG_SCOPED_PUBLIC: VisibilityEnumVisibility = "ORG_SCOPED_PUBLIC"

  def toJson(obj: VisibilityEnumVisibility): JString = JString(obj)

  def fromJson(v: JValue): VisibilityEnumVisibility = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
