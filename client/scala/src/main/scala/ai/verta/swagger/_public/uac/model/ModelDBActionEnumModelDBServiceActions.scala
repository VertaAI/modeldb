// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

object ModelDBActionEnumModelDBServiceActions {
  type ModelDBActionEnumModelDBServiceActions = String
  val UNKNOWN: ModelDBActionEnumModelDBServiceActions = "UNKNOWN"
  val ALL: ModelDBActionEnumModelDBServiceActions = "ALL"
  val CREATE: ModelDBActionEnumModelDBServiceActions = "CREATE"
  val READ: ModelDBActionEnumModelDBServiceActions = "READ"
  val UPDATE: ModelDBActionEnumModelDBServiceActions = "UPDATE"
  val DELETE: ModelDBActionEnumModelDBServiceActions = "DELETE"
  val DEPLOY: ModelDBActionEnumModelDBServiceActions = "DEPLOY"
  val PUBLIC_READ: ModelDBActionEnumModelDBServiceActions = "PUBLIC_READ"

  def toJson(obj: ModelDBActionEnumModelDBServiceActions): JString = JString(obj)

  def fromJson(v: JValue): ModelDBActionEnumModelDBServiceActions = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
