// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.registry.model

import scala.util.Try

import net.liftweb.json._

object ProtobufNullValue {
  type ProtobufNullValue = String
  val NULL_VALUE: ProtobufNullValue = "NULL_VALUE"

  def toJson(obj: ProtobufNullValue): JString = JString(obj)

  def fromJson(v: JValue): ProtobufNullValue = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
