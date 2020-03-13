# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

{{#__object_flag}}
class {{class_name}}(BaseType):
  def __init__(self, {{#properties}}{{name}}=None{{^last}}, {{/last}}{{/properties}}):
    required = {
      {{#properties}}
      "{{name}}": {{required}},
      {{/properties}}
    }
    {{#properties}}
    {{#required}}

    {{/required}}
    self.{{name}} = {{name}}
    {{/properties}}

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
{{#properties}}
    {{#type}}{{> from_json_name}}{{/type}}
{{/properties}}

    {{#properties}}
    tmp = d.get('{{name}}', None)
    if tmp is not None:
      d['{{name}}'] = {{#type}}{{> from_json}}{{/type}}
    {{/properties}}

    return {{class_name}}(**d)
{{/__object_flag}}
{{#__enum_flag}}
class {{class_name}}(BaseType):
  _valid_values = [
    {{#enum_values}}
    "{{name}}",
    {{/enum_values}}
  ]

  def __init__(self, val):
    if val not in {{class_name}}._valid_values:
      raise ValueError('{} is not a valid value for {{class_name}}'.format(val))
    self.value = val

  def to_json(self):
    return self.value

  def from_json(v):
    if isinstance(v, str):
      return {{class_name}}(v)
    else:
      return {{class_name}}({{class_name}}._valid_values[v])

{{/__enum_flag}}
