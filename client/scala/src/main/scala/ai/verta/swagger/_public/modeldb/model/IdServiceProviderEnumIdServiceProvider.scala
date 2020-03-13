// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

object IdServiceProviderEnumIdServiceProvider {
  type IdServiceProviderEnumIdServiceProvider = String
  val UNKNOWN: IdServiceProviderEnumIdServiceProvider = "UNKNOWN"
  val GITHUB: IdServiceProviderEnumIdServiceProvider = "GITHUB"
  val BITBUCKET: IdServiceProviderEnumIdServiceProvider = "BITBUCKET"
  val GOOGLE: IdServiceProviderEnumIdServiceProvider = "GOOGLE"
  val VERTA: IdServiceProviderEnumIdServiceProvider = "VERTA"

  def toJson(obj: IdServiceProviderEnumIdServiceProvider): JString = JString(obj)

  def fromJson(v: JValue): IdServiceProviderEnumIdServiceProvider = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
