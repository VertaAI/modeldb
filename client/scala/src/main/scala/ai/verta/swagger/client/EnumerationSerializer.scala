package ai.verta.swagger.client

import net.liftweb.json._

class EnumerationSerializer(enums: Enumeration*) extends Serializer[Enumeration#Value] {
  val EnumerationClass = classOf[Enumeration#Value]
  val formats = Serialization.formats(NoTypeHints)

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), Enumeration#Value] = {
    case (TypeInfo(EnumerationClass, _), json) => json match {
      case JString(value) => enums.find(_.values.exists(_.toString == value)).get.withName(value)
      case value => throw new MappingException("Can't convert " + value + " to " + EnumerationClass)
    }
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case i: Enumeration#Value => JString(i.toString)
  }
}