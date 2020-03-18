// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class UacAddTeamUser (
  share_with: Option[String] = None,
  team_id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = UacAddTeamUser.toJson(this)
}

object UacAddTeamUser {
  def toJson(obj: UacAddTeamUser): JObject = {
    new JObject(
      List[Option[JField]](
        obj.share_with.map(x => JField("share_with", JString(x))),
        obj.team_id.map(x => JField("team_id", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacAddTeamUser =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacAddTeamUser(
          // TODO: handle required
          share_with = fieldsMap.get("share_with").map(JsonConverter.fromJsonString),
          team_id = fieldsMap.get("team_id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
