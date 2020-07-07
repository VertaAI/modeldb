package ai.verta.client.entities.utils

import ai.verta.swagger._public.modeldb.model.CommonKeyValue
import ai.verta.swagger.client.objects.GenericObject

import scala.util.{Failure, Success, Try}

object KVHandler {
  def convertToValueType(v: GenericObject, err: String): Try[ValueType] = {
    if (v.double_value.isDefined)
      Success(v.double_value.get)
    else if (v.int_value.isDefined)
      Success(v.int_value.get)
    else if (v.string_value.isDefined)
      Success(v.string_value.get)
    else
      Failure(new IllegalArgumentException(err))
  }

  def convertFromValueType(v: ValueType, err: String): Try[GenericObject] = {
    v match {
      case IntValueType(x) => Success(new GenericObject(int_value = Some(x)))
      case DoubleValueType(x) => Success(new GenericObject(double_value = Some(x)))
      case StringValueType(x) => Success(new GenericObject(string_value = Some(x)))
    }
  }

  def mapToKVList(vals: Map[String, ValueType]): Try[List[CommonKeyValue]] = {
    Try({
      vals.toList.map(arg => {
        val k = arg._1
        val v = arg._2
        convertFromValueType(v, s"unknown type for key ${k}: ${v.toString} (${v.getClass.toString})")
          .map(v => CommonKeyValue(key = Some(k), value = Some(v)))
          .get
      })
    })
  }

  def kvListToMap(vals: List[CommonKeyValue]): Try[Map[String, ValueType]] = {
    Try({
      val keys = vals.map(_.key.get)
      val values = vals.map(v => convertToValueType(v.value.get, s"unknown type for key ${v.key.get}: ${v.value.get.toString} (${v.value.get.getClass.toString})").get)
      (keys zip values).toMap
    })
  }
}
