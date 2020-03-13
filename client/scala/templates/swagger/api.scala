// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger.{{package}}.api

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger.{{package}}.model._

class {{api_name}}Api(client: HttpClient, val basePath: String = "{{base_path}}") {
{{#operations}}
  def {{operation_id}}Async({{#parameters}}{{safe_name}}: {{#type}}{{> type}}{{/type}}{{^last}}, {{/last}}{{/parameters}})(implicit ec: ExecutionContext): Future[Try[{{#success_type}}{{> type}}{{/success_type}}]] = {
    val __query = Map[String,String](
      {{#query}}
      "{{name}}" -> client.toQuery({{safe_name}}){{^last}},{{/last}}
      {{/query}}
    )
    {{#required}}
    if ({{safe_name}} == null) throw new Exception("Missing required parameter \"{{safe_name}}\"")
    {{/required}}
    {{^body_present}}
    val body: String = null
    {{/body_present}}
    return client.request[{{#body_type}}{{> type}}{{/body_type}}, {{#success_type}}{{> type}}{{/success_type}}]("{{op}}", basePath + s"{{path}}", __query, body, {{#success_type}}{{> type}}{{/success_type}}.fromJson)
  }

  def {{operation_id}}({{#parameters}}{{safe_name}}: {{#type}}{{> type}}{{/type}}{{^last}}, {{/last}}{{/parameters}})(implicit ec: ExecutionContext): Try[{{#success_type}}{{> type}}{{/success_type}}] = Await.result({{operation_id}}Async({{#parameters}}{{safe_name}}{{^last}}, {{/last}}{{/parameters}}), Duration.Inf)

{{/operations}}
}
