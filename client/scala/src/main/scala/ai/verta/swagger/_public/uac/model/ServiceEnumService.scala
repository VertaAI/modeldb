// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

object ServiceEnumService {
  type ServiceEnumService = String
  val UNKNOWN: ServiceEnumService = "UNKNOWN"
  val ALL: ServiceEnumService = "ALL"
  val ROLE_SERVICE: ServiceEnumService = "ROLE_SERVICE"
  val AUTHZ_SERVICE: ServiceEnumService = "AUTHZ_SERVICE"
  val MODELDB_SERVICE: ServiceEnumService = "MODELDB_SERVICE"

  def toJson(obj: ServiceEnumService): JString = JString(obj)

  def fromJson(v: JValue): ServiceEnumService = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
