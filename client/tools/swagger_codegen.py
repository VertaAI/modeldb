#!/usr/bin/env python3

import functools
import json
import os
import sys
import collections
import pystache
import click

FieldType = collections.namedtuple('FieldType', ['safe_name', 'name', 'type'])

@click.command()
@click.option('--output-dir')
@click.option('--input')
@click.option('--templates')
@click.option('--file-suffix')
@click.option('--case')
def main(output_dir, input, templates, file_suffix, case):
    with open(input) as f:
        content = json.load(f)

    def basedirReducer(init, val):
        if val[1] != 'protos':
            return init
        return val[0]

    protos_index = functools.reduce(basedirReducer, enumerate(input.split('/')), None)
    basedir = input.split('/')[protos_index+1:-1]
    api_name = input.split('/')[-1].split('.')[0]
    basedir[0] = "_"+basedir[0]
    result_dir = os.path.join(output_dir, *basedir)
    result_package = '.'.join(basedir)

    create_models(result_dir, result_package, content, templates, file_suffix, case)
    create_api(result_dir, result_package, api_name, content, templates, file_suffix, case)

def create_api(result_dir, result_package, api_name, content, templates, file_suffix, case):
    if 'paths' not in content:
        return

    paths = []
    operations = []
    for path, p_content in content['paths'].items():
        for op, op_content in p_content.items():
            operation_id = op_content['operationId']
            operations.append((operation_id, path+"", op+""))
    operations = sorted(operations, key=lambda x: x[0])

    operations_info = []

    for (operation_id, path, op) in operations:
            op_content = content['paths'][path][op]
            parameters = []
            query = []
            path_components = []
            got_body_parameter = False
            body_type = create_typedef(any=True)
            required = []

            for param_def in op_content.get('parameters', []):
                name = param_def['name']
                safe_name = to_language_case(name.replace('.', '_'), case)
                parameters.append(FieldType(safe_name, name, resolve_type(param_def)))

                if param_def.get('required', False):
                    required.append(parameters[-1])

                if param_def['in'] == 'body':
                    got_body_parameter = True
                    body_type = resolve_type(param_def)
                elif param_def['in'] == 'query':
                    query.append(FieldType(safe_name, name, resolve_type(param_def)))
                elif param_def['in'] == 'path':
                    path = path.replace('{%s}' % name, "$%s" % safe_name)
                else:
                    # print(path)
                    raise ValueError(param_def['in'])


            # TODO
            success_response = None
            code_mapping = {}
            for code, definition in op_content['responses'].items():
                if code == "200":
                    success_response = resolve_type(definition)
                else:
                    code_mapping[code] = resolve_type(definition)

            operations_info.append({
                'operation_id': operation_id,
                'success_type': success_response,
                'query': [{
                    'name': q.name,
                    'safe_name': q.safe_name,
                    'type': q.type,
                    'last': i == len(query)-1
                } for i, q in enumerate(query)],
                'body_present': got_body_parameter,
                'body_type': body_type,
                'parameters': [{
                    'name': p.name,
                    'safe_name': p.safe_name,
                    'type': p.type,
                    'last': i == len(parameters)-1
                } for i, p in enumerate(parameters)],
                'required': [{
                    'name': q.name,
                    'safe_name': q.safe_name,
                    'type': q.type,
                    'last': i == len(required)-1
                } for i, q in enumerate(required)],
                'op': op.upper(),
                'path': path,
            })

    os.makedirs(os.path.join(result_dir, 'api'), exist_ok=True)

    with open(os.path.join(templates, 'api.'+file_suffix)) as f:
        template = f.read()

    info = {
        'package': result_package,
        'api_name': api_name,
        'base_path': content.get('basePath', ''),
        'operations': operations_info,
    }

    with open(os.path.join(result_dir, 'api', api_name+'Api.'+file_suffix), 'w') as f:
        f.write(pystache.Renderer(partials=load_partials(templates)).render(template, info))


def load_partials(templates_folder):
    ret = {}
    for _, _, files in os.walk(templates_folder):
        for f in files:
            parts = f.split('.')
            if parts[1] == 'mustache':
                ret[parts[0]] = open(os.path.join(templates_folder, f)).read()

    return ret

def keyword_safe(s):
    if s in ['type']:
        return '`%s`' % s
    return s


def create_models(result_dir, result_package, content, templates, file_suffix, case):
    enums = []
    os.makedirs(os.path.join(result_dir, 'model'), exist_ok=True)
    for k, v in content['definitions'].items():
        if 'enum' in v:
            enums.append(capitalize_first(k))
    for k, v in content['definitions'].items():
        create_model(result_dir, result_package, k, v, enums, templates, file_suffix, case)
    if len(content['definitions']) == 0:
        filename = os.path.join(result_dir, 'model', 'dummy.'+file_suffix)
        with open(filename, 'w') as f:
            f.write('''
// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger.%s.model
case class dummy()
''' % result_package)

def create_model(result_dir, result_package, definition_name, definition, enums, templates, file_suffix, case):
    capitalized_definition_name = capitalize_first(definition_name)
    filename = os.path.join(result_dir, 'model', capitalized_definition_name+'.'+file_suffix)

    with open(os.path.join(templates, 'model.'+file_suffix)) as f:
        template = f.read()

    required = definition.get('required', list())
    properties = definition.get('properties', dict())
    properties = [{'name': keyword_safe(to_language_case(k, case)),
                   'required': k in required,
                   'type': resolve_type(v)
                  } for k, v in properties.items()]
    for i, p in enumerate(properties):
        p.update({'last': i == len(properties)-1})

    info = {
        '__object_flag': False,
        '__enum_flag': False,
        'package': result_package,
        'class_name': capitalized_definition_name,
        'properties': properties,
        'object': True,
        'enums': [{'name': e} for e in enums],
        'enum_values': [{'name': e} for e in definition.get('enum', list())],
    }

    type_flag = "__"+definition['type']+"_flag"

    if definition['type'] == 'object':
        info['__object_flag'] = True
    elif 'enum' in definition:
        info['__enum_flag'] = True
    else:
        raise ValueError(definition['type'])

    with open(filename, 'w') as f:
        f.write(pystache.Renderer(partials=load_partials(templates)).render(template, info))

def create_typedef(**kwargs):
    return dict(kwargs, **{
        'is_list': kwargs.get('is_list', False),
        'is_basic': kwargs.get('is_basic', False),
        'is_map': kwargs.get('is_map', False),
        'string': kwargs.get('string', False),
        'integer': kwargs.get('integer', False),
        'double': kwargs.get('double', False),
        'any': kwargs.get('any', False),
        'is_custom': kwargs.get('custom', None) is not None,
    })


# TODO: generalize to types besides Scala
def resolve_type(typedef):
    if '$ref' in typedef:
        ref = typedef['$ref'].split('/')[-1]
        return create_typedef(custom={'name': capitalize_first(ref)})

    if 'schema' in typedef:
        return resolve_type(typedef['schema'])

    if typedef['type'] == 'string':
        return create_typedef(string=True, is_basic=True)
    elif typedef['type'] == 'boolean':
        return create_typedef(boolean=True, is_basic=True)
    elif typedef['type'] == 'integer':
        if typedef['format'] == 'int32':
            return create_typedef(integer=True, is_basic=True)
        else:
            raise ValueError(typedef['format'])
    elif typedef['type'] == 'number':
        if typedef['format'] == 'double':
            return create_typedef(double=True, is_basic=True)
        elif typedef['format'] == 'float':
            return create_typedef(double=True, is_basic=True)
        else:
            raise ValueError(typedef['format'])
    elif typedef['type'] == 'array':
        return create_typedef(is_list=True, list_type=resolve_type(typedef['items']))
    elif typedef['type'] == 'object' and len(typedef) == 1:
        return create_typedef(generic=True)
    elif typedef['type'] == 'object' and 'additionalProperties' in typedef and 'properties' not in typedef:
        return create_typedef(is_map=True, map_key_type=resolve_type({'type': 'string'}), map_val_type=resolve_type(typedef['additionalProperties']))
    else:
        raise ValueError(typedef['type'])

def capitalize_first(s):
    return s[0].upper() + s[1:]

def to_language_case(base, case):
    if case == 'camel':
        return to_camel_case(base)
    if case == 'snake':
        return base
    raise ValueError(case)

def to_camel_case(snake_str):
    components = snake_str.split('_')
    # We capitalize the first letter of each component except the first one
    # with the 'title' method and join them together.
    return components[0] + ''.join(x.title() for x in components[1:])

if __name__ == "__main__":
    main()
