package ai.verta.client.entities.utils

import ai.verta.swagger._public.modeldb.model.CommonKeyValue
import ai.verta.swagger.client.objects.GenericObject

import scala.util.{Failure, Success, Try}
import scala.language.implicitConversions

sealed trait ValueType
case class IntValueType(i: BigInt) extends ValueType
case class StringValueType(s: String) extends ValueType
case class DoubleValueType(d: Double) extends ValueType

object ValueType {
  implicit def fromInt(i: Int): ValueType = IntValueType(BigInt(i))
  implicit def fromBigInt(i: BigInt): ValueType = IntValueType(i)
  implicit def fromString(s: String): ValueType = StringValueType(s)
  implicit def fromDouble(d: Double): ValueType = DoubleValueType(d)
}
