// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

object ValueTypeEnumValueType {
  type ValueTypeEnumValueType = String
  val STRING: ValueTypeEnumValueType = "STRING"
  val NUMBER: ValueTypeEnumValueType = "NUMBER"
  val LIST: ValueTypeEnumValueType = "LIST"
  val BLOB: ValueTypeEnumValueType = "BLOB"

  def toJson(obj: ValueTypeEnumValueType): JString = JString(obj)

  def fromJson(v: JValue): ValueTypeEnumValueType = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
