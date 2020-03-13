package ai.verta.swagger.client.objects

import net.liftweb.json._

case class GenericObject(
                          null_value: Option[Unit] = None,
                          int_value: Option[BigInt] = None,
                          double_value: Option[Double] = None,
                          string_value: Option[String] = None,
                          bool_value: Option[Boolean] = None,
                          struct_value: Option[Map[String, GenericObject]] = None,
                          list_value: Option[List[GenericObject]] = None
                        ) {
  def toJson(): JValue = GenericObject.toJson(this)
}

object GenericObject {
  def toJson(obj: GenericObject): JValue = {
    if (obj.string_value.isDefined)
      JString(obj.string_value.get)
    else if (obj.double_value.isDefined)
      JDouble(obj.double_value.get)
    else if (obj.int_value.isDefined)
      JInt(obj.int_value.get)
    else
      throw new Exception(s"unknown type for ${obj.toString} (${obj.getClass.toString})")
  }

  def fromJson(v: JValue): GenericObject =
    v match {
      case JString(x) => GenericObject(string_value = Some(x))
      case JDouble(x) => GenericObject(double_value = Some(x))
      case JInt(x) => GenericObject(int_value = Some(x))
      case _ => throw new Exception(s"unknown type for ${v.toString} (${v.getClass.toString})")
    }
}
