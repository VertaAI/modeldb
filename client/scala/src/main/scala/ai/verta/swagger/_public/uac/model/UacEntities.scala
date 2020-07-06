// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.ModelDBActionEnumModelDBServiceActions._
import ai.verta.swagger._public.uac.model.ModelDBResourceEnumModelDBServiceResourceTypes._
import ai.verta.swagger._public.uac.model.ServiceEnumService._
import ai.verta.swagger.client.objects._

case class UacEntities (
  org_ids: Option[List[String]] = None,
  team_ids: Option[List[String]] = None,
  user_ids: Option[List[String]] = None
) extends BaseSwagger {
  def toJson(): JValue = UacEntities.toJson(this)
}

object UacEntities {
  def toJson(obj: UacEntities): JObject = {
    new JObject(
      List[Option[JField]](
        obj.org_ids.map(x => JField("org_ids", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.team_ids.map(x => JField("team_ids", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.user_ids.map(x => JField("user_ids", ((x: List[String]) => JArray(x.map(JString)))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacEntities =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacEntities(
          // TODO: handle required
          org_ids = fieldsMap.get("org_ids").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          team_ids = fieldsMap.get("team_ids").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          user_ids = fieldsMap.get("user_ids").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
