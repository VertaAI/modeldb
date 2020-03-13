// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

object EntitiesEnumEntitiesTypes {
  type EntitiesEnumEntitiesTypes = String
  val UNKNOWN: EntitiesEnumEntitiesTypes = "UNKNOWN"
  val ORGANIZATION: EntitiesEnumEntitiesTypes = "ORGANIZATION"
  val TEAM: EntitiesEnumEntitiesTypes = "TEAM"
  val USER: EntitiesEnumEntitiesTypes = "USER"

  def toJson(obj: EntitiesEnumEntitiesTypes): JString = JString(obj)

  def fromJson(v: JValue): EntitiesEnumEntitiesTypes = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
