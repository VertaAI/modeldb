package ai.verta.swagger.client.objects

import net.liftweb.json._

object JsonConverter {
  def fromJsonString(x: JValue) = x match {
    case JString(v) => v
    case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")
  }

  def fromJsonInteger(x: JValue) = x match {
    case JInt(v) => v
    case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")
  }

  def fromJsonDouble(x: JValue) = x match {
    case JDouble(v) => v
    case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")
  }

  def fromJsonBoolean(x: JValue) = x match {
    case JBool(v) => v
    case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")
  }
}
