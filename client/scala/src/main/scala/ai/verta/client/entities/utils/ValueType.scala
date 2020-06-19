package ai.verta.client.entities.utils

import scala.language.implicitConversions

sealed trait ValueType {
  def asBigInt: Option[BigInt] = None
  def asString: Option[String] = None
  def asDouble: Option[Double] = None
}

case class IntValueType(i: BigInt) extends ValueType {
  override def asBigInt = Some(i)
}

case class StringValueType(s: String) extends ValueType {
  override def asString = Some(s)
}

case class DoubleValueType(d: Double) extends ValueType {
  override def asDouble = Some(d)
}

object ValueType {
  implicit def fromInt(i: Int): ValueType = IntValueType(BigInt(i))
  implicit def fromBigInt(i: BigInt): ValueType = IntValueType(i)
  implicit def fromString(s: String): ValueType = StringValueType(s)
  implicit def fromDouble(d: Double): ValueType = DoubleValueType(d)
}
