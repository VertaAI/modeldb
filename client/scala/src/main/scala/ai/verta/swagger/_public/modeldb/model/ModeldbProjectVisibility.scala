// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

object ModeldbProjectVisibility {
  type ModeldbProjectVisibility = String
  val PRIVATE: ModeldbProjectVisibility = "PRIVATE"
  val PUBLIC: ModeldbProjectVisibility = "PUBLIC"
  val ORG_SCOPED_PUBLIC: ModeldbProjectVisibility = "ORG_SCOPED_PUBLIC"

  def toJson(obj: ModeldbProjectVisibility): JString = JString(obj)

  def fromJson(v: JValue): ModeldbProjectVisibility = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
