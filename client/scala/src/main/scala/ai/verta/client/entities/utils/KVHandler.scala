package ai.verta.client.entities.utils

import ai.verta.swagger._public.modeldb.model.CommonKeyValue
import ai.verta.swagger.client.objects.GenericObject

import scala.util.{Failure, Success, Try}

object KVHandler {
  def convertToAny(v: GenericObject, err: String): Try[Any] = {
    if (v.double_value.isDefined)
      Success(v.double_value.get)
    else if (v.int_value.isDefined)
      Success(v.int_value.get)
    else if (v.string_value.isDefined)
      Success(v.string_value.get)
    else
      Failure(new IllegalArgumentException(err))
  }

  def convertFromAny(v: Any, err: String): Try[GenericObject] = {
    v match {
      case x: Int => Success(new GenericObject(int_value = Some(x)))
      case x: Double => Success(new GenericObject(double_value = Some(x)))
      case x: String => Success(new GenericObject(string_value = Some(x)))
      case _ => Failure(new IllegalArgumentException(err))
    }
  }

  def mapToKVList(vals: Map[String, Any]): Try[List[CommonKeyValue]] = {
    Try({
      vals.toList.map(arg => {
        val k = arg._1
        val v = arg._2
        convertFromAny(v, s"unknown type for key ${k}: ${v.toString} (${v.getClass.toString})")
          .map(v => CommonKeyValue(key = Some(k), value = Some(v)))
          .get
      })
    })
  }

  def kvListToMap(vals: List[CommonKeyValue]): Try[Map[String, Any]] = {
    Try({
      val keys = vals.map(_.key.get)
      val values = vals.map(v => convertToAny(v.value.get, s"unknown type for key ${v.key.get}: ${v.value.get.toString} (${v.value.get.getClass.toString})").get)
      (keys zip values).toMap
    })
  }
}
