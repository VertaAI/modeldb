# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class {{api_name}}Api:
  def __init__(self, client, base_path = "{{base_path}}"):
    self.client = client
    self.base_path = base_path
{{#operations}}

  def {{operation_id}}(self, {{#parameters}}{{safe_name}}=None{{^last}}, {{/last}}{{/parameters}}):
    __query = {
      {{#query}}
      "{{name}}": client.to_query({{safe_name}}){{^last}},{{/last}}
      {{/query}}
    }
    {{#required}}
    if {{safe_name}} is None:
      raise Exception("Missing required parameter \"{{safe_name}}\"")
    {{/required}}
    {{^body_present}}
    body = None
    {{/body_present}}

    format_args = {}
    path = "{{path}}"
    {{#parameters}}
    if "${{safe_name}}" in path:
      path = path.replace("${{safe_name}}", "%({{safe_name}})s")
      format_args["{{safe_name}}"] = {{safe_name}}
    {{/parameters}}
    ret = self.client.request("{{op}}", self.base_path + path % format_args, __query, body)
    if ret is not None:
      {{#success_type}}
      {{#custom}}
      from ..model.{{name}} import {{name}}
      ret = {{name}}.from_json(ret)
      {{/custom}}
      {{^custom}}
      pass
      {{/custom}}
      {{/success_type}}

    return ret
{{/operations}}
