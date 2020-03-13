// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

object OperatorEnumOperator {
  type OperatorEnumOperator = String
  val EQ: OperatorEnumOperator = "EQ"
  val NE: OperatorEnumOperator = "NE"
  val GT: OperatorEnumOperator = "GT"
  val GTE: OperatorEnumOperator = "GTE"
  val LT: OperatorEnumOperator = "LT"
  val LTE: OperatorEnumOperator = "LTE"
  val CONTAIN: OperatorEnumOperator = "CONTAIN"
  val NOT_CONTAIN: OperatorEnumOperator = "NOT_CONTAIN"
  val IN: OperatorEnumOperator = "IN"

  def toJson(obj: OperatorEnumOperator): JString = JString(obj)

  def fromJson(v: JValue): OperatorEnumOperator = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
