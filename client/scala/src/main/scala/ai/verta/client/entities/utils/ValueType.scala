package ai.verta.client.entities.utils

import scala.language.implicitConversions

/** Union type of (Big)Int, String, Double, and List.
 *  Represent the possible type of observations, metrics, hyperparameters, attribute values, etc.
 *  Instances of subtypes of this trait should be instantiated via implicit conversion.
 *  Use asBigInt, asString, asDouble, asList to get the actual value.
 */
sealed trait ValueType {
  def asBigInt: Option[BigInt] = None
  def asString: Option[String] = None
  def asDouble: Option[Double] = None
  def asList: Option[List[ValueType]] = None
}

/** Represent (Big) integer observations, metrics, hyperparameters, etc.
 *  User should not instantiate this class themselves but rely on implicit conversion
 */
case class IntValueType(private val i: BigInt) extends ValueType {
  override def asBigInt = Some(i)
}

/** Represent string observations, metrics, hyperparameters, etc.
 *  User should not instantiate this class themselves but rely on implicit conversion
 */
case class StringValueType(private val s: String) extends ValueType {
  override def asString = Some(s)
}

/** Represent double observations, metrics, hyperparameters, etc.
 *  User should not instantiate this class themselves but rely on implicit conversion
 */
case class DoubleValueType(private val d: Double) extends ValueType {
  override def asDouble = Some(d)
}

/** Represent list values.
 *  User should not instantiate this class themselves but rely on implicit conversion
 */
case class ListValueType(private val l: List[ValueType]) extends ValueType {
  override def asList: Option[List[ValueType]] = Some(l)
}

object ValueType {
  /** Implicit conversion method to convert an integer to ValueType
   *  @param i the integer
   *  @return the equivalent ValueType instance
   */
  implicit def fromInt(i: Int): ValueType = IntValueType(BigInt(i))

  /** Implicit conversion method to convert a big integer to ValueType
   *  @param i the big integer
   *  @return the equivalent ValueType instance
   */
  implicit def fromBigInt(i: BigInt): ValueType = IntValueType(i)

  /** Implicit conversion method to convert a string to ValueType
   *  @param s the string
   *  @return the equivalent ValueType instance
   */
  implicit def fromString(s: String): ValueType = StringValueType(s)

  /** Implicit conversion method to convert a double to ValueType
   *  @param d the double
   *  @return the equivalent ValueType instance
   */
  implicit def fromDouble(d: Double): ValueType = DoubleValueType(d)

  /** Implicit conversion method to convert a list to ValueType
   *  @param l the list
   *  @return the equivalent ValueType instance
   */
  implicit def fromList(l: List[ValueType]): ValueType = ListValueType(l)
}
