// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.ModelDBActionEnumModelDBServiceActions._
import ai.verta.swagger._public.uac.model.ModelDBResourceEnumModelDBServiceResourceTypes._
import ai.verta.swagger._public.uac.model.ServiceEnumService._
import ai.verta.swagger.client.objects._

case class UacDeleteRoleBindings (
  roleBindingNames: Option[List[String]] = None
) extends BaseSwagger {
  def toJson(): JValue = UacDeleteRoleBindings.toJson(this)
}

object UacDeleteRoleBindings {
  def toJson(obj: UacDeleteRoleBindings): JObject = {
    new JObject(
      List[Option[JField]](
        obj.roleBindingNames.map(x => JField("roleBindingNames", ((x: List[String]) => JArray(x.map(JString)))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacDeleteRoleBindings =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacDeleteRoleBindings(
          // TODO: handle required
          roleBindingNames = fieldsMap.get("roleBindingNames").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
