// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger.{{package}}.api

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger.{{package}}.model._

class {{api_name}}Api(client: HttpClient, val basePath: String = "{{base_path}}") {
{{#operations}}
  def {{operation_id}}Async({{#parameters}}{{safe_name}}: {{^required}}Option[{{/required}}{{#type}}{{> type}}{{/type}}{{^required}}]=None{{/required}}{{^last}}, {{/last}}{{/parameters}})(implicit ec: ExecutionContext): Future[Try[{{#success_type}}{{> type}}{{/success_type}}]] = {
    var __query = new mutable.HashMap[String,List[String]]
    {{#query}}
    {{^required}}
    if ({{safe_name}}.isDefined) __query.update("{{name}}", client.toQuery({{safe_name}}.get))
    {{/required}}
    {{#required}}
    __query.update("{{name}}", client.toQuery({{safe_name}}))
    {{/required}}
    {{/query}}
    {{#required_parameters}}
    if ({{safe_name}} == null) throw new Exception("Missing required parameter \"{{safe_name}}\"")
    {{/required_parameters}}
    {{^body_present}}
    val body: String = null
    {{/body_present}}
    return client.request[{{#body_type}}{{> type}}{{/body_type}}, {{#success_type}}{{> type}}{{/success_type}}]("{{op}}", basePath + s"{{path}}", __query.toMap, body, {{#success_type}}{{> type}}{{/success_type}}.fromJson)
  }

  def {{operation_id}}({{#parameters}}{{safe_name}}: {{^required}}Option[{{/required}}{{#type}}{{> type}}{{/type}}{{^required}}]=None{{/required}}{{^last}}, {{/last}}{{/parameters}})(implicit ec: ExecutionContext): Try[{{#success_type}}{{> type}}{{/success_type}}] = Await.result({{operation_id}}Async({{#parameters}}{{safe_name}}{{^last}}, {{/last}}{{/parameters}}), Duration.Inf)

{{/operations}}
}
