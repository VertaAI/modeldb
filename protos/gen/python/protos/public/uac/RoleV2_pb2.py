# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: uac/RoleV2.proto

from google.protobuf.internal import enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from google.api import annotations_pb2 as google_dot_api_dot_annotations__pb2
from ..common import CommonService_pb2 as common_dot_CommonService__pb2
from ..uac import UACService_pb2 as uac_dot_UACService__pb2


DESCRIPTOR = _descriptor.FileDescriptor(
  name='uac/RoleV2.proto',
  package='ai.verta.uac',
  syntax='proto3',
  serialized_options=b'P\001Z:github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac',
  serialized_pb=b'\n\x10uac/RoleV2.proto\x12\x0c\x61i.verta.uac\x1a\x1cgoogle/api/annotations.proto\x1a\x1a\x63ommon/CommonService.proto\x1a\x14uac/UACService.proto\"\x8b\x01\n\x13RoleResourceActions\x12\n\n\x02id\x18\x01 \x01(\t\x12\x33\n\rresource_type\x18\x02 \x01(\x0e\x32\x1c.ai.verta.uac.ResourceTypeV2\x12\x33\n\x0f\x61llowed_actions\x18\x03 \x03(\x0e\x32\x1a.ai.verta.uac.ActionTypeV2\"k\n\x11GetEnabledActions\x12\x0e\n\x06org_id\x18\x01 \x01(\t\x1a\x46\n\x08Response\x12:\n\x0f\x65nabled_actions\x18\x01 \x03(\x0b\x32!.ai.verta.uac.RoleResourceActions\"\x96\x01\n\x06RoleV2\x12\x0e\n\x06org_id\x18\x01 \x01(\t\x12\n\n\x02id\x18\x02 \x01(\t\x12\x0c\n\x04name\x18\x03 \x01(\t\x12\x10\n\x08\x62uilt_in\x18\x04 \x01(\x08\x12;\n\x10resource_actions\x18\x05 \x03(\x0b\x32!.ai.verta.uac.RoleResourceActions\x12\x13\n\x0b\x64\x65scription\x18\x06 \x01(\t\"_\n\tSetRoleV2\x12\"\n\x04role\x18\x01 \x01(\x0b\x32\x14.ai.verta.uac.RoleV2\x1a.\n\x08Response\x12\"\n\x04role\x18\x01 \x01(\x0b\x32\x14.ai.verta.uac.RoleV2\";\n\x0c\x44\x65leteRoleV2\x12\x0e\n\x06org_id\x18\x01 \x01(\t\x12\x0f\n\x07role_id\x18\x02 \x01(\t\x1a\n\n\x08Response\"\xc9\x01\n\rSearchRolesV2\x12\x0e\n\x06org_id\x18\x01 \x01(\t\x12/\n\npagination\x18\x02 \x01(\x0b\x32\x1b.ai.verta.common.Pagination\x1aw\n\x08Response\x12#\n\x05roles\x18\x01 \x03(\x0b\x32\x14.ai.verta.uac.RoleV2\x12\x15\n\rtotal_records\x18\x02 \x01(\x03\x12/\n\npagination\x18\x03 \x01(\x0b\x32\x1b.ai.verta.common.Pagination\"\\\n\tGetRoleV2\x12\x0e\n\x06org_id\x18\x01 \x01(\t\x12\x0f\n\x07role_id\x18\x02 \x01(\t\x1a.\n\x08Response\x12\"\n\x04role\x18\x01 \x01(\x0b\x32\x14.ai.verta.uac.RoleV2*\x99\x01\n\x0eResourceTypeV2\x12\x19\n\x15RESOURCE_TYPE_UNKNOWN\x10\x00\x12\x0b\n\x07\x44\x41TASET\x10\x01\x12\x0b\n\x07PROJECT\x10\x02\x12\x14\n\x10REGISTERED_MODEL\x10\x03\x12\x0c\n\x08\x45NDPOINT\x10\x04\x12\x14\n\x10MONITORED_ENTITY\x10\x05\x12\x18\n\x14NOTIFICATION_CHANNEL\x10\x06*\x90\x01\n\x0c\x41\x63tionTypeV2\x12\x17\n\x13\x41\x43TION_TYPE_UNKNOWN\x10\x00\x12\x08\n\x04READ\x10\x01\x12\n\n\x06UPDATE\x10\x02\x12\n\n\x06\x43REATE\x10\x03\x12\n\n\x06\x44\x45LETE\x10\x04\x12\x12\n\x0e\x41PPROVE_REJECT\x10\x05\x12\x0c\n\x08REGISTER\x10\x06\x12\n\n\x06\x44\x45PLOY\x10\x07\x12\x0b\n\x07PREDICT\x10\x08\x32\x93\x05\n\rRoleServiceV2\x12u\n\x07setRole\x12\x17.ai.verta.uac.SetRoleV2\x1a .ai.verta.uac.SetRoleV2.Response\"/\x82\xd3\xe4\x93\x02)\"$/v2/organization/{role.org_id}/roles:\x01*\x12\x80\x01\n\ndeleteRole\x12\x1a.ai.verta.uac.DeleteRoleV2\x1a#.ai.verta.uac.DeleteRoleV2.Response\"1\x82\xd3\xe4\x93\x02+*)/v2/organization/{org_id}/roles/{role_id}\x12y\n\x0bsearchRoles\x12\x1b.ai.verta.uac.SearchRolesV2\x1a$.ai.verta.uac.SearchRolesV2.Response\"\'\x82\xd3\xe4\x93\x02!\x12\x1f/v2/organization/{org_id}/roles\x12w\n\x07getRole\x12\x17.ai.verta.uac.GetRoleV2\x1a .ai.verta.uac.GetRoleV2.Response\"1\x82\xd3\xe4\x93\x02+\x12)/v2/organization/{org_id}/roles/{role_id}\x12\x93\x01\n\x11getEnabledActions\x12\x1f.ai.verta.uac.GetEnabledActions\x1a(.ai.verta.uac.GetEnabledActions.Response\"3\x82\xd3\xe4\x93\x02-\x12+/v2/organization/{org_id}/getEnabledActionsB>P\x01Z:github.com/VertaAI/modeldb/protos/gen/go/protos/public/uacb\x06proto3'
  ,
  dependencies=[google_dot_api_dot_annotations__pb2.DESCRIPTOR,common_dot_CommonService__pb2.DESCRIPTOR,uac_dot_UACService__pb2.DESCRIPTOR,])

_RESOURCETYPEV2 = _descriptor.EnumDescriptor(
  name='ResourceTypeV2',
  full_name='ai.verta.uac.ResourceTypeV2',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='RESOURCE_TYPE_UNKNOWN', index=0, number=0,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='DATASET', index=1, number=1,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='PROJECT', index=2, number=2,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='REGISTERED_MODEL', index=3, number=3,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='ENDPOINT', index=4, number=4,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='MONITORED_ENTITY', index=5, number=5,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='NOTIFICATION_CHANNEL', index=6, number=6,
      serialized_options=None,
      type=None),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=975,
  serialized_end=1128,
)
_sym_db.RegisterEnumDescriptor(_RESOURCETYPEV2)

ResourceTypeV2 = enum_type_wrapper.EnumTypeWrapper(_RESOURCETYPEV2)
_ACTIONTYPEV2 = _descriptor.EnumDescriptor(
  name='ActionTypeV2',
  full_name='ai.verta.uac.ActionTypeV2',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='ACTION_TYPE_UNKNOWN', index=0, number=0,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='READ', index=1, number=1,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='UPDATE', index=2, number=2,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='CREATE', index=3, number=3,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='DELETE', index=4, number=4,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='APPROVE_REJECT', index=5, number=5,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='REGISTER', index=6, number=6,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='DEPLOY', index=7, number=7,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='PREDICT', index=8, number=8,
      serialized_options=None,
      type=None),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=1131,
  serialized_end=1275,
)
_sym_db.RegisterEnumDescriptor(_ACTIONTYPEV2)

ActionTypeV2 = enum_type_wrapper.EnumTypeWrapper(_ACTIONTYPEV2)
RESOURCE_TYPE_UNKNOWN = 0
DATASET = 1
PROJECT = 2
REGISTERED_MODEL = 3
ENDPOINT = 4
MONITORED_ENTITY = 5
NOTIFICATION_CHANNEL = 6
ACTION_TYPE_UNKNOWN = 0
READ = 1
UPDATE = 2
CREATE = 3
DELETE = 4
APPROVE_REJECT = 5
REGISTER = 6
DEPLOY = 7
PREDICT = 8



_ROLERESOURCEACTIONS = _descriptor.Descriptor(
  name='RoleResourceActions',
  full_name='ai.verta.uac.RoleResourceActions',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='id', full_name='ai.verta.uac.RoleResourceActions.id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='resource_type', full_name='ai.verta.uac.RoleResourceActions.resource_type', index=1,
      number=2, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='allowed_actions', full_name='ai.verta.uac.RoleResourceActions.allowed_actions', index=2,
      number=3, type=14, cpp_type=8, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=115,
  serialized_end=254,
)


_GETENABLEDACTIONS_RESPONSE = _descriptor.Descriptor(
  name='Response',
  full_name='ai.verta.uac.GetEnabledActions.Response',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='enabled_actions', full_name='ai.verta.uac.GetEnabledActions.Response.enabled_actions', index=0,
      number=1, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=293,
  serialized_end=363,
)

_GETENABLEDACTIONS = _descriptor.Descriptor(
  name='GetEnabledActions',
  full_name='ai.verta.uac.GetEnabledActions',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='org_id', full_name='ai.verta.uac.GetEnabledActions.org_id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_GETENABLEDACTIONS_RESPONSE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=256,
  serialized_end=363,
)


_ROLEV2 = _descriptor.Descriptor(
  name='RoleV2',
  full_name='ai.verta.uac.RoleV2',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='org_id', full_name='ai.verta.uac.RoleV2.org_id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='id', full_name='ai.verta.uac.RoleV2.id', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='name', full_name='ai.verta.uac.RoleV2.name', index=2,
      number=3, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='built_in', full_name='ai.verta.uac.RoleV2.built_in', index=3,
      number=4, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='resource_actions', full_name='ai.verta.uac.RoleV2.resource_actions', index=4,
      number=5, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='description', full_name='ai.verta.uac.RoleV2.description', index=5,
      number=6, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=366,
  serialized_end=516,
)


_SETROLEV2_RESPONSE = _descriptor.Descriptor(
  name='Response',
  full_name='ai.verta.uac.SetRoleV2.Response',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='role', full_name='ai.verta.uac.SetRoleV2.Response.role', index=0,
      number=1, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=567,
  serialized_end=613,
)

_SETROLEV2 = _descriptor.Descriptor(
  name='SetRoleV2',
  full_name='ai.verta.uac.SetRoleV2',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='role', full_name='ai.verta.uac.SetRoleV2.role', index=0,
      number=1, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_SETROLEV2_RESPONSE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=518,
  serialized_end=613,
)


_DELETEROLEV2_RESPONSE = _descriptor.Descriptor(
  name='Response',
  full_name='ai.verta.uac.DeleteRoleV2.Response',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=293,
  serialized_end=303,
)

_DELETEROLEV2 = _descriptor.Descriptor(
  name='DeleteRoleV2',
  full_name='ai.verta.uac.DeleteRoleV2',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='org_id', full_name='ai.verta.uac.DeleteRoleV2.org_id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='role_id', full_name='ai.verta.uac.DeleteRoleV2.role_id', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_DELETEROLEV2_RESPONSE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=615,
  serialized_end=674,
)


_SEARCHROLESV2_RESPONSE = _descriptor.Descriptor(
  name='Response',
  full_name='ai.verta.uac.SearchRolesV2.Response',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='roles', full_name='ai.verta.uac.SearchRolesV2.Response.roles', index=0,
      number=1, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='total_records', full_name='ai.verta.uac.SearchRolesV2.Response.total_records', index=1,
      number=2, type=3, cpp_type=2, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='pagination', full_name='ai.verta.uac.SearchRolesV2.Response.pagination', index=2,
      number=3, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=759,
  serialized_end=878,
)

_SEARCHROLESV2 = _descriptor.Descriptor(
  name='SearchRolesV2',
  full_name='ai.verta.uac.SearchRolesV2',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='org_id', full_name='ai.verta.uac.SearchRolesV2.org_id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='pagination', full_name='ai.verta.uac.SearchRolesV2.pagination', index=1,
      number=2, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_SEARCHROLESV2_RESPONSE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=677,
  serialized_end=878,
)


_GETROLEV2_RESPONSE = _descriptor.Descriptor(
  name='Response',
  full_name='ai.verta.uac.GetRoleV2.Response',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='role', full_name='ai.verta.uac.GetRoleV2.Response.role', index=0,
      number=1, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=567,
  serialized_end=613,
)

_GETROLEV2 = _descriptor.Descriptor(
  name='GetRoleV2',
  full_name='ai.verta.uac.GetRoleV2',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='org_id', full_name='ai.verta.uac.GetRoleV2.org_id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='role_id', full_name='ai.verta.uac.GetRoleV2.role_id', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_GETROLEV2_RESPONSE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=880,
  serialized_end=972,
)

_ROLERESOURCEACTIONS.fields_by_name['resource_type'].enum_type = _RESOURCETYPEV2
_ROLERESOURCEACTIONS.fields_by_name['allowed_actions'].enum_type = _ACTIONTYPEV2
_GETENABLEDACTIONS_RESPONSE.fields_by_name['enabled_actions'].message_type = _ROLERESOURCEACTIONS
_GETENABLEDACTIONS_RESPONSE.containing_type = _GETENABLEDACTIONS
_ROLEV2.fields_by_name['resource_actions'].message_type = _ROLERESOURCEACTIONS
_SETROLEV2_RESPONSE.fields_by_name['role'].message_type = _ROLEV2
_SETROLEV2_RESPONSE.containing_type = _SETROLEV2
_SETROLEV2.fields_by_name['role'].message_type = _ROLEV2
_DELETEROLEV2_RESPONSE.containing_type = _DELETEROLEV2
_SEARCHROLESV2_RESPONSE.fields_by_name['roles'].message_type = _ROLEV2
_SEARCHROLESV2_RESPONSE.fields_by_name['pagination'].message_type = common_dot_CommonService__pb2._PAGINATION
_SEARCHROLESV2_RESPONSE.containing_type = _SEARCHROLESV2
_SEARCHROLESV2.fields_by_name['pagination'].message_type = common_dot_CommonService__pb2._PAGINATION
_GETROLEV2_RESPONSE.fields_by_name['role'].message_type = _ROLEV2
_GETROLEV2_RESPONSE.containing_type = _GETROLEV2
DESCRIPTOR.message_types_by_name['RoleResourceActions'] = _ROLERESOURCEACTIONS
DESCRIPTOR.message_types_by_name['GetEnabledActions'] = _GETENABLEDACTIONS
DESCRIPTOR.message_types_by_name['RoleV2'] = _ROLEV2
DESCRIPTOR.message_types_by_name['SetRoleV2'] = _SETROLEV2
DESCRIPTOR.message_types_by_name['DeleteRoleV2'] = _DELETEROLEV2
DESCRIPTOR.message_types_by_name['SearchRolesV2'] = _SEARCHROLESV2
DESCRIPTOR.message_types_by_name['GetRoleV2'] = _GETROLEV2
DESCRIPTOR.enum_types_by_name['ResourceTypeV2'] = _RESOURCETYPEV2
DESCRIPTOR.enum_types_by_name['ActionTypeV2'] = _ACTIONTYPEV2
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

RoleResourceActions = _reflection.GeneratedProtocolMessageType('RoleResourceActions', (_message.Message,), {
  'DESCRIPTOR' : _ROLERESOURCEACTIONS,
  '__module__' : 'uac.RoleV2_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.uac.RoleResourceActions)
  })
_sym_db.RegisterMessage(RoleResourceActions)

GetEnabledActions = _reflection.GeneratedProtocolMessageType('GetEnabledActions', (_message.Message,), {

  'Response' : _reflection.GeneratedProtocolMessageType('Response', (_message.Message,), {
    'DESCRIPTOR' : _GETENABLEDACTIONS_RESPONSE,
    '__module__' : 'uac.RoleV2_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.uac.GetEnabledActions.Response)
    })
  ,
  'DESCRIPTOR' : _GETENABLEDACTIONS,
  '__module__' : 'uac.RoleV2_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.uac.GetEnabledActions)
  })
_sym_db.RegisterMessage(GetEnabledActions)
_sym_db.RegisterMessage(GetEnabledActions.Response)

RoleV2 = _reflection.GeneratedProtocolMessageType('RoleV2', (_message.Message,), {
  'DESCRIPTOR' : _ROLEV2,
  '__module__' : 'uac.RoleV2_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.uac.RoleV2)
  })
_sym_db.RegisterMessage(RoleV2)

SetRoleV2 = _reflection.GeneratedProtocolMessageType('SetRoleV2', (_message.Message,), {

  'Response' : _reflection.GeneratedProtocolMessageType('Response', (_message.Message,), {
    'DESCRIPTOR' : _SETROLEV2_RESPONSE,
    '__module__' : 'uac.RoleV2_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.uac.SetRoleV2.Response)
    })
  ,
  'DESCRIPTOR' : _SETROLEV2,
  '__module__' : 'uac.RoleV2_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.uac.SetRoleV2)
  })
_sym_db.RegisterMessage(SetRoleV2)
_sym_db.RegisterMessage(SetRoleV2.Response)

DeleteRoleV2 = _reflection.GeneratedProtocolMessageType('DeleteRoleV2', (_message.Message,), {

  'Response' : _reflection.GeneratedProtocolMessageType('Response', (_message.Message,), {
    'DESCRIPTOR' : _DELETEROLEV2_RESPONSE,
    '__module__' : 'uac.RoleV2_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.uac.DeleteRoleV2.Response)
    })
  ,
  'DESCRIPTOR' : _DELETEROLEV2,
  '__module__' : 'uac.RoleV2_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.uac.DeleteRoleV2)
  })
_sym_db.RegisterMessage(DeleteRoleV2)
_sym_db.RegisterMessage(DeleteRoleV2.Response)

SearchRolesV2 = _reflection.GeneratedProtocolMessageType('SearchRolesV2', (_message.Message,), {

  'Response' : _reflection.GeneratedProtocolMessageType('Response', (_message.Message,), {
    'DESCRIPTOR' : _SEARCHROLESV2_RESPONSE,
    '__module__' : 'uac.RoleV2_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.uac.SearchRolesV2.Response)
    })
  ,
  'DESCRIPTOR' : _SEARCHROLESV2,
  '__module__' : 'uac.RoleV2_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.uac.SearchRolesV2)
  })
_sym_db.RegisterMessage(SearchRolesV2)
_sym_db.RegisterMessage(SearchRolesV2.Response)

GetRoleV2 = _reflection.GeneratedProtocolMessageType('GetRoleV2', (_message.Message,), {

  'Response' : _reflection.GeneratedProtocolMessageType('Response', (_message.Message,), {
    'DESCRIPTOR' : _GETROLEV2_RESPONSE,
    '__module__' : 'uac.RoleV2_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.uac.GetRoleV2.Response)
    })
  ,
  'DESCRIPTOR' : _GETROLEV2,
  '__module__' : 'uac.RoleV2_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.uac.GetRoleV2)
  })
_sym_db.RegisterMessage(GetRoleV2)
_sym_db.RegisterMessage(GetRoleV2.Response)


DESCRIPTOR._options = None

_ROLESERVICEV2 = _descriptor.ServiceDescriptor(
  name='RoleServiceV2',
  full_name='ai.verta.uac.RoleServiceV2',
  file=DESCRIPTOR,
  index=0,
  serialized_options=None,
  serialized_start=1278,
  serialized_end=1937,
  methods=[
  _descriptor.MethodDescriptor(
    name='setRole',
    full_name='ai.verta.uac.RoleServiceV2.setRole',
    index=0,
    containing_service=None,
    input_type=_SETROLEV2,
    output_type=_SETROLEV2_RESPONSE,
    serialized_options=b'\202\323\344\223\002)\"$/v2/organization/{role.org_id}/roles:\001*',
  ),
  _descriptor.MethodDescriptor(
    name='deleteRole',
    full_name='ai.verta.uac.RoleServiceV2.deleteRole',
    index=1,
    containing_service=None,
    input_type=_DELETEROLEV2,
    output_type=_DELETEROLEV2_RESPONSE,
    serialized_options=b'\202\323\344\223\002+*)/v2/organization/{org_id}/roles/{role_id}',
  ),
  _descriptor.MethodDescriptor(
    name='searchRoles',
    full_name='ai.verta.uac.RoleServiceV2.searchRoles',
    index=2,
    containing_service=None,
    input_type=_SEARCHROLESV2,
    output_type=_SEARCHROLESV2_RESPONSE,
    serialized_options=b'\202\323\344\223\002!\022\037/v2/organization/{org_id}/roles',
  ),
  _descriptor.MethodDescriptor(
    name='getRole',
    full_name='ai.verta.uac.RoleServiceV2.getRole',
    index=3,
    containing_service=None,
    input_type=_GETROLEV2,
    output_type=_GETROLEV2_RESPONSE,
    serialized_options=b'\202\323\344\223\002+\022)/v2/organization/{org_id}/roles/{role_id}',
  ),
  _descriptor.MethodDescriptor(
    name='getEnabledActions',
    full_name='ai.verta.uac.RoleServiceV2.getEnabledActions',
    index=4,
    containing_service=None,
    input_type=_GETENABLEDACTIONS,
    output_type=_GETENABLEDACTIONS_RESPONSE,
    serialized_options=b'\202\323\344\223\002-\022+/v2/organization/{org_id}/getEnabledActions',
  ),
])
_sym_db.RegisterServiceDescriptor(_ROLESERVICEV2)

DESCRIPTOR.services_by_name['RoleServiceV2'] = _ROLESERVICEV2

# @@protoc_insertion_point(module_scope)
