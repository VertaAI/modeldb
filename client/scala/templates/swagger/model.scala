// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger.{{package}}.model

import scala.util.Try

import net.liftweb.json._

{{#__object_flag}}
{{#enums}}
import ai.verta.swagger.{{package}}.model.{{name}}._
{{/enums}}
import ai.verta.swagger.client.objects._

case class {{class_name}} (
{{#properties}}
  {{#required}}
  {{name}}: {{#type}}{{> type}}{{/type}}{{^last}},{{/last}}
  {{/required}}
  {{^required}}
  {{name}}: Option[{{#type}}{{> type}}{{/type}}] = None{{^last}},{{/last}}
  {{/required}}
{{/properties}}
) extends BaseSwagger {
  def toJson(): JValue = {{class_name}}.toJson(this)
}

object {{class_name}} {
  def toJson(obj: {{class_name}}): JObject = {
    new JObject(
      List[Option[JField]](
        {{#properties}}
        {{#required}}
        Some(JField("{{name}}", {{#type}}{{> to_json}}{{/type}}(obj.{{name}}))){{^last}},{{/last}}
        {{/required}}
        {{^required}}
        obj.{{name}}.map(x => JField("{{name}}", {{#type}}{{> to_json}}{{/type}}(x))){{^last}},{{/last}}
        {{/required}}
        {{/properties}}
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): {{class_name}} =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        {{class_name}}(
          // TODO: handle required
          {{#properties}}
          {{^required}}
          {{name}} = fieldsMap.get("{{name}}").map({{#type}}{{> from_json}}{{/type}}){{^last}},{{/last}}
          {{/required}}
          {{/properties}}
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
{{/__object_flag}}
{{#__enum_flag}}
object {{class_name}} {
  type {{class_name}} = String
  {{#enum_values}}
  val {{name}}: {{class_name}} = "{{name}}"
  {{/enum_values}}

  def toJson(obj: {{class_name}}): JString = JString(obj)

  def fromJson(v: JValue): {{class_name}} = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
{{/__enum_flag}}