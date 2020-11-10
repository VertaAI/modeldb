# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: uac/UACService.proto

from google.protobuf.internal import enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from google.api import annotations_pb2 as google_dot_api_dot_annotations__pb2


DESCRIPTOR = _descriptor.FileDescriptor(
  name='uac/UACService.proto',
  package='ai.verta.uac',
  syntax='proto3',
  serialized_options=b'P\001Z:github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac',
  serialized_pb=b'\n\x14uac/UACService.proto\x12\x0c\x61i.verta.uac\x1a\x1cgoogle/api/annotations.proto\"\x07\n\x05\x45mpty\";\n\x07GetUser\x12\x0f\n\x07user_id\x18\x01 \x01(\t\x12\r\n\x05\x65mail\x18\x02 \x01(\t\x12\x10\n\x08username\x18\x03 \x01(\t\"w\n\x08GetUsers\x12\x10\n\x08user_ids\x18\x01 \x03(\t\x12\x0e\n\x06\x65mails\x18\x02 \x03(\t\x12\x11\n\tusernames\x18\x03 \x03(\t\x1a\x36\n\x08Response\x12*\n\nuser_infos\x18\x01 \x03(\x0b\x32\x16.ai.verta.uac.UserInfo\"5\n\nPagination\x12\x13\n\x0bpage_number\x18\x02 \x01(\x05\x12\x12\n\npage_limit\x18\x03 \x01(\x05\"\xad\x01\n\rGetUsersFuzzy\x12\r\n\x05\x65mail\x18\x02 \x01(\t\x12\x10\n\x08username\x18\x03 \x01(\t\x12,\n\npagination\x18\x04 \x01(\x0b\x32\x18.ai.verta.uac.Pagination\x1aM\n\x08Response\x12*\n\nuser_infos\x18\x01 \x03(\x0b\x32\x16.ai.verta.uac.UserInfo\x12\x15\n\rtotal_records\x18\x02 \x01(\x03\"u\n\x15IdServiceProviderEnum\"\\\n\x11IdServiceProvider\x12\x0b\n\x07UNKNOWN\x10\x00\x12\n\n\x06GITHUB\x10\x01\x12\r\n\tBITBUCKET\x10\x02\x12\n\n\x06GOOGLE\x10\x03\x12\t\n\x05VERTA\x10\x04\x12\x08\n\x04SAML\x10\x05\"\'\n\rTrialUserInfo\x12\x16\n\x0e\x64\x61ys_remaining\x18\x01 \x01(\x05\"\xfa\x01\n\rVertaUserInfo\x12\x17\n\x0findividual_user\x18\x01 \x01(\x08\x12\x10\n\x08username\x18\x02 \x01(\t\x12\x19\n\x11refresh_timestamp\x18\x03 \x01(\x04\x12\x1c\n\x14last_login_timestamp\x18\x04 \x01(\x04\x12\x0f\n\x07user_id\x18\x05 \x01(\t\x12-\n\rpublicProfile\x18\x06 \x01(\x0e\x32\x16.ai.verta.uac.FlagEnum\x12\x14\n\x0cworkspace_id\x18\x07 \x01(\t\x12/\n\ntrial_info\x18\x08 \x01(\x0b\x32\x1b.ai.verta.uac.TrialUserInfo\"\xa2\x02\n\x08UserInfo\x12\x0f\n\x07user_id\x18\x01 \x01(\t\x12\x11\n\tfull_name\x18\x02 \x01(\t\x12\x12\n\nfirst_name\x18\x03 \x01(\t\x12\x11\n\tlast_name\x18\x04 \x01(\t\x12\r\n\x05\x65mail\x18\x05 \x01(\t\x12R\n\x13id_service_provider\x18\x06 \x01(\x0e\x32\x35.ai.verta.uac.IdServiceProviderEnum.IdServiceProvider\x12\r\n\x05roles\x18\x07 \x03(\t\x12\x11\n\timage_url\x18\x08 \x01(\t\x12\x0f\n\x07\x64\x65v_key\x18\t \x01(\t\x12/\n\nverta_info\x18\n \x01(\x0b\x32\x1b.ai.verta.uac.VertaUserInfoJ\x04\x08\x0b\x10\x0c\"v\n\nCreateUser\x12$\n\x04info\x18\x01 \x01(\x0b\x32\x16.ai.verta.uac.UserInfo\x12\x10\n\x08password\x18\x02 \x01(\t\x1a\x30\n\x08Response\x12$\n\x04info\x18\x01 \x01(\x0b\x32\x16.ai.verta.uac.UserInfo\"v\n\nUpdateUser\x12$\n\x04info\x18\x01 \x01(\x0b\x32\x16.ai.verta.uac.UserInfo\x12\x10\n\x08password\x18\x02 \x01(\t\x1a\x30\n\x08Response\x12$\n\x04info\x18\x01 \x01(\x0b\x32\x16.ai.verta.uac.UserInfo\"9\n\nDeleteUser\x12\x0f\n\x07user_id\x18\x01 \x01(\t\x1a\x1a\n\x08Response\x12\x0e\n\x06status\x18\x01 \x01(\x08*.\n\x08\x46lagEnum\x12\r\n\tUNDEFINED\x10\x00\x12\x08\n\x04TRUE\x10\x01\x12\t\n\x05\x46\x41LSE\x10\x02\x32\xd4\x05\n\nUACService\x12]\n\x0egetCurrentUser\x12\x13.ai.verta.uac.Empty\x1a\x16.ai.verta.uac.UserInfo\"\x1e\x82\xd3\xe4\x93\x02\x18\x12\x16/v1/uac/getCurrentUser\x12Q\n\x07getUser\x12\x15.ai.verta.uac.GetUser\x1a\x16.ai.verta.uac.UserInfo\"\x17\x82\xd3\xe4\x93\x02\x11\x12\x0f/v1/uac/getUser\x12`\n\x08getUsers\x12\x16.ai.verta.uac.GetUsers\x1a\x1f.ai.verta.uac.GetUsers.Response\"\x1b\x82\xd3\xe4\x93\x02\x15\"\x10/v1/uac/getUsers:\x01*\x12t\n\rgetUsersFuzzy\x12\x1b.ai.verta.uac.GetUsersFuzzy\x1a$.ai.verta.uac.GetUsersFuzzy.Response\" \x82\xd3\xe4\x93\x02\x1a\"\x15/v1/uac/getUsersFuzzy:\x01*\x12h\n\ncreateUser\x12\x18.ai.verta.uac.CreateUser\x1a!.ai.verta.uac.CreateUser.Response\"\x1d\x82\xd3\xe4\x93\x02\x17\"\x12/v1/uac/createUser:\x01*\x12h\n\nupdateUser\x12\x18.ai.verta.uac.UpdateUser\x1a!.ai.verta.uac.UpdateUser.Response\"\x1d\x82\xd3\xe4\x93\x02\x17\"\x12/v1/uac/updateUser:\x01*\x12h\n\ndeleteUser\x12\x18.ai.verta.uac.DeleteUser\x1a!.ai.verta.uac.DeleteUser.Response\"\x1d\x82\xd3\xe4\x93\x02\x17\"\x12/v1/uac/deleteUser:\x01*B>P\x01Z:github.com/VertaAI/modeldb/protos/gen/go/protos/public/uacb\x06proto3'
  ,
  dependencies=[google_dot_api_dot_annotations__pb2.DESCRIPTOR,])

_FLAGENUM = _descriptor.EnumDescriptor(
  name='FlagEnum',
  full_name='ai.verta.uac.FlagEnum',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='UNDEFINED', index=0, number=0,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='TRUE', index=1, number=1,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='FALSE', index=2, number=2,
      serialized_options=None,
      type=None),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=1495,
  serialized_end=1541,
)
_sym_db.RegisterEnumDescriptor(_FLAGENUM)

FlagEnum = enum_type_wrapper.EnumTypeWrapper(_FLAGENUM)
UNDEFINED = 0
TRUE = 1
FALSE = 2


_IDSERVICEPROVIDERENUM_IDSERVICEPROVIDER = _descriptor.EnumDescriptor(
  name='IdServiceProvider',
  full_name='ai.verta.uac.IdServiceProviderEnum.IdServiceProvider',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='UNKNOWN', index=0, number=0,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='GITHUB', index=1, number=1,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='BITBUCKET', index=2, number=2,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='GOOGLE', index=3, number=3,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='VERTA', index=4, number=4,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='SAML', index=5, number=5,
      serialized_options=None,
      type=None),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=515,
  serialized_end=607,
)
_sym_db.RegisterEnumDescriptor(_IDSERVICEPROVIDERENUM_IDSERVICEPROVIDER)


_EMPTY = _descriptor.Descriptor(
  name='Empty',
  full_name='ai.verta.uac.Empty',
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
  serialized_start=68,
  serialized_end=75,
)


_GETUSER = _descriptor.Descriptor(
  name='GetUser',
  full_name='ai.verta.uac.GetUser',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='user_id', full_name='ai.verta.uac.GetUser.user_id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='email', full_name='ai.verta.uac.GetUser.email', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='username', full_name='ai.verta.uac.GetUser.username', index=2,
      number=3, type=9, cpp_type=9, label=1,
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
  serialized_start=77,
  serialized_end=136,
)


_GETUSERS_RESPONSE = _descriptor.Descriptor(
  name='Response',
  full_name='ai.verta.uac.GetUsers.Response',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='user_infos', full_name='ai.verta.uac.GetUsers.Response.user_infos', index=0,
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
  serialized_start=203,
  serialized_end=257,
)

_GETUSERS = _descriptor.Descriptor(
  name='GetUsers',
  full_name='ai.verta.uac.GetUsers',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='user_ids', full_name='ai.verta.uac.GetUsers.user_ids', index=0,
      number=1, type=9, cpp_type=9, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='emails', full_name='ai.verta.uac.GetUsers.emails', index=1,
      number=2, type=9, cpp_type=9, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='usernames', full_name='ai.verta.uac.GetUsers.usernames', index=2,
      number=3, type=9, cpp_type=9, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_GETUSERS_RESPONSE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=138,
  serialized_end=257,
)


_PAGINATION = _descriptor.Descriptor(
  name='Pagination',
  full_name='ai.verta.uac.Pagination',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='page_number', full_name='ai.verta.uac.Pagination.page_number', index=0,
      number=2, type=5, cpp_type=1, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='page_limit', full_name='ai.verta.uac.Pagination.page_limit', index=1,
      number=3, type=5, cpp_type=1, label=1,
      has_default_value=False, default_value=0,
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
  serialized_start=259,
  serialized_end=312,
)


_GETUSERSFUZZY_RESPONSE = _descriptor.Descriptor(
  name='Response',
  full_name='ai.verta.uac.GetUsersFuzzy.Response',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='user_infos', full_name='ai.verta.uac.GetUsersFuzzy.Response.user_infos', index=0,
      number=1, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='total_records', full_name='ai.verta.uac.GetUsersFuzzy.Response.total_records', index=1,
      number=2, type=3, cpp_type=2, label=1,
      has_default_value=False, default_value=0,
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
  serialized_start=411,
  serialized_end=488,
)

_GETUSERSFUZZY = _descriptor.Descriptor(
  name='GetUsersFuzzy',
  full_name='ai.verta.uac.GetUsersFuzzy',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='email', full_name='ai.verta.uac.GetUsersFuzzy.email', index=0,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='username', full_name='ai.verta.uac.GetUsersFuzzy.username', index=1,
      number=3, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='pagination', full_name='ai.verta.uac.GetUsersFuzzy.pagination', index=2,
      number=4, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_GETUSERSFUZZY_RESPONSE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=315,
  serialized_end=488,
)


_IDSERVICEPROVIDERENUM = _descriptor.Descriptor(
  name='IdServiceProviderEnum',
  full_name='ai.verta.uac.IdServiceProviderEnum',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
    _IDSERVICEPROVIDERENUM_IDSERVICEPROVIDER,
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=490,
  serialized_end=607,
)


_TRIALUSERINFO = _descriptor.Descriptor(
  name='TrialUserInfo',
  full_name='ai.verta.uac.TrialUserInfo',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='days_remaining', full_name='ai.verta.uac.TrialUserInfo.days_remaining', index=0,
      number=1, type=5, cpp_type=1, label=1,
      has_default_value=False, default_value=0,
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
  serialized_start=609,
  serialized_end=648,
)


_VERTAUSERINFO = _descriptor.Descriptor(
  name='VertaUserInfo',
  full_name='ai.verta.uac.VertaUserInfo',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='individual_user', full_name='ai.verta.uac.VertaUserInfo.individual_user', index=0,
      number=1, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='username', full_name='ai.verta.uac.VertaUserInfo.username', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='refresh_timestamp', full_name='ai.verta.uac.VertaUserInfo.refresh_timestamp', index=2,
      number=3, type=4, cpp_type=4, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='last_login_timestamp', full_name='ai.verta.uac.VertaUserInfo.last_login_timestamp', index=3,
      number=4, type=4, cpp_type=4, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='user_id', full_name='ai.verta.uac.VertaUserInfo.user_id', index=4,
      number=5, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='publicProfile', full_name='ai.verta.uac.VertaUserInfo.publicProfile', index=5,
      number=6, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='workspace_id', full_name='ai.verta.uac.VertaUserInfo.workspace_id', index=6,
      number=7, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='trial_info', full_name='ai.verta.uac.VertaUserInfo.trial_info', index=7,
      number=8, type=11, cpp_type=10, label=1,
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
  serialized_start=651,
  serialized_end=901,
)


_USERINFO = _descriptor.Descriptor(
  name='UserInfo',
  full_name='ai.verta.uac.UserInfo',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='user_id', full_name='ai.verta.uac.UserInfo.user_id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='full_name', full_name='ai.verta.uac.UserInfo.full_name', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='first_name', full_name='ai.verta.uac.UserInfo.first_name', index=2,
      number=3, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='last_name', full_name='ai.verta.uac.UserInfo.last_name', index=3,
      number=4, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='email', full_name='ai.verta.uac.UserInfo.email', index=4,
      number=5, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='id_service_provider', full_name='ai.verta.uac.UserInfo.id_service_provider', index=5,
      number=6, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='roles', full_name='ai.verta.uac.UserInfo.roles', index=6,
      number=7, type=9, cpp_type=9, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='image_url', full_name='ai.verta.uac.UserInfo.image_url', index=7,
      number=8, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='dev_key', full_name='ai.verta.uac.UserInfo.dev_key', index=8,
      number=9, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='verta_info', full_name='ai.verta.uac.UserInfo.verta_info', index=9,
      number=10, type=11, cpp_type=10, label=1,
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
  serialized_start=904,
  serialized_end=1194,
)


_CREATEUSER_RESPONSE = _descriptor.Descriptor(
  name='Response',
  full_name='ai.verta.uac.CreateUser.Response',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='info', full_name='ai.verta.uac.CreateUser.Response.info', index=0,
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
  serialized_start=1266,
  serialized_end=1314,
)

_CREATEUSER = _descriptor.Descriptor(
  name='CreateUser',
  full_name='ai.verta.uac.CreateUser',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='info', full_name='ai.verta.uac.CreateUser.info', index=0,
      number=1, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='password', full_name='ai.verta.uac.CreateUser.password', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_CREATEUSER_RESPONSE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=1196,
  serialized_end=1314,
)


_UPDATEUSER_RESPONSE = _descriptor.Descriptor(
  name='Response',
  full_name='ai.verta.uac.UpdateUser.Response',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='info', full_name='ai.verta.uac.UpdateUser.Response.info', index=0,
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
  serialized_start=1266,
  serialized_end=1314,
)

_UPDATEUSER = _descriptor.Descriptor(
  name='UpdateUser',
  full_name='ai.verta.uac.UpdateUser',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='info', full_name='ai.verta.uac.UpdateUser.info', index=0,
      number=1, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='password', full_name='ai.verta.uac.UpdateUser.password', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_UPDATEUSER_RESPONSE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=1316,
  serialized_end=1434,
)


_DELETEUSER_RESPONSE = _descriptor.Descriptor(
  name='Response',
  full_name='ai.verta.uac.DeleteUser.Response',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='status', full_name='ai.verta.uac.DeleteUser.Response.status', index=0,
      number=1, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
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
  serialized_start=1467,
  serialized_end=1493,
)

_DELETEUSER = _descriptor.Descriptor(
  name='DeleteUser',
  full_name='ai.verta.uac.DeleteUser',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='user_id', full_name='ai.verta.uac.DeleteUser.user_id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_DELETEUSER_RESPONSE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=1436,
  serialized_end=1493,
)

_GETUSERS_RESPONSE.fields_by_name['user_infos'].message_type = _USERINFO
_GETUSERS_RESPONSE.containing_type = _GETUSERS
_GETUSERSFUZZY_RESPONSE.fields_by_name['user_infos'].message_type = _USERINFO
_GETUSERSFUZZY_RESPONSE.containing_type = _GETUSERSFUZZY
_GETUSERSFUZZY.fields_by_name['pagination'].message_type = _PAGINATION
_IDSERVICEPROVIDERENUM_IDSERVICEPROVIDER.containing_type = _IDSERVICEPROVIDERENUM
_VERTAUSERINFO.fields_by_name['publicProfile'].enum_type = _FLAGENUM
_VERTAUSERINFO.fields_by_name['trial_info'].message_type = _TRIALUSERINFO
_USERINFO.fields_by_name['id_service_provider'].enum_type = _IDSERVICEPROVIDERENUM_IDSERVICEPROVIDER
_USERINFO.fields_by_name['verta_info'].message_type = _VERTAUSERINFO
_CREATEUSER_RESPONSE.fields_by_name['info'].message_type = _USERINFO
_CREATEUSER_RESPONSE.containing_type = _CREATEUSER
_CREATEUSER.fields_by_name['info'].message_type = _USERINFO
_UPDATEUSER_RESPONSE.fields_by_name['info'].message_type = _USERINFO
_UPDATEUSER_RESPONSE.containing_type = _UPDATEUSER
_UPDATEUSER.fields_by_name['info'].message_type = _USERINFO
_DELETEUSER_RESPONSE.containing_type = _DELETEUSER
DESCRIPTOR.message_types_by_name['Empty'] = _EMPTY
DESCRIPTOR.message_types_by_name['GetUser'] = _GETUSER
DESCRIPTOR.message_types_by_name['GetUsers'] = _GETUSERS
DESCRIPTOR.message_types_by_name['Pagination'] = _PAGINATION
DESCRIPTOR.message_types_by_name['GetUsersFuzzy'] = _GETUSERSFUZZY
DESCRIPTOR.message_types_by_name['IdServiceProviderEnum'] = _IDSERVICEPROVIDERENUM
DESCRIPTOR.message_types_by_name['TrialUserInfo'] = _TRIALUSERINFO
DESCRIPTOR.message_types_by_name['VertaUserInfo'] = _VERTAUSERINFO
DESCRIPTOR.message_types_by_name['UserInfo'] = _USERINFO
DESCRIPTOR.message_types_by_name['CreateUser'] = _CREATEUSER
DESCRIPTOR.message_types_by_name['UpdateUser'] = _UPDATEUSER
DESCRIPTOR.message_types_by_name['DeleteUser'] = _DELETEUSER
DESCRIPTOR.enum_types_by_name['FlagEnum'] = _FLAGENUM
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

Empty = _reflection.GeneratedProtocolMessageType('Empty', (_message.Message,), {
  'DESCRIPTOR' : _EMPTY,
  '__module__' : 'uac.UACService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.uac.Empty)
  })
_sym_db.RegisterMessage(Empty)

GetUser = _reflection.GeneratedProtocolMessageType('GetUser', (_message.Message,), {
  'DESCRIPTOR' : _GETUSER,
  '__module__' : 'uac.UACService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.uac.GetUser)
  })
_sym_db.RegisterMessage(GetUser)

GetUsers = _reflection.GeneratedProtocolMessageType('GetUsers', (_message.Message,), {

  'Response' : _reflection.GeneratedProtocolMessageType('Response', (_message.Message,), {
    'DESCRIPTOR' : _GETUSERS_RESPONSE,
    '__module__' : 'uac.UACService_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.uac.GetUsers.Response)
    })
  ,
  'DESCRIPTOR' : _GETUSERS,
  '__module__' : 'uac.UACService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.uac.GetUsers)
  })
_sym_db.RegisterMessage(GetUsers)
_sym_db.RegisterMessage(GetUsers.Response)

Pagination = _reflection.GeneratedProtocolMessageType('Pagination', (_message.Message,), {
  'DESCRIPTOR' : _PAGINATION,
  '__module__' : 'uac.UACService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.uac.Pagination)
  })
_sym_db.RegisterMessage(Pagination)

GetUsersFuzzy = _reflection.GeneratedProtocolMessageType('GetUsersFuzzy', (_message.Message,), {

  'Response' : _reflection.GeneratedProtocolMessageType('Response', (_message.Message,), {
    'DESCRIPTOR' : _GETUSERSFUZZY_RESPONSE,
    '__module__' : 'uac.UACService_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.uac.GetUsersFuzzy.Response)
    })
  ,
  'DESCRIPTOR' : _GETUSERSFUZZY,
  '__module__' : 'uac.UACService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.uac.GetUsersFuzzy)
  })
_sym_db.RegisterMessage(GetUsersFuzzy)
_sym_db.RegisterMessage(GetUsersFuzzy.Response)

IdServiceProviderEnum = _reflection.GeneratedProtocolMessageType('IdServiceProviderEnum', (_message.Message,), {
  'DESCRIPTOR' : _IDSERVICEPROVIDERENUM,
  '__module__' : 'uac.UACService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.uac.IdServiceProviderEnum)
  })
_sym_db.RegisterMessage(IdServiceProviderEnum)

TrialUserInfo = _reflection.GeneratedProtocolMessageType('TrialUserInfo', (_message.Message,), {
  'DESCRIPTOR' : _TRIALUSERINFO,
  '__module__' : 'uac.UACService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.uac.TrialUserInfo)
  })
_sym_db.RegisterMessage(TrialUserInfo)

VertaUserInfo = _reflection.GeneratedProtocolMessageType('VertaUserInfo', (_message.Message,), {
  'DESCRIPTOR' : _VERTAUSERINFO,
  '__module__' : 'uac.UACService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.uac.VertaUserInfo)
  })
_sym_db.RegisterMessage(VertaUserInfo)

UserInfo = _reflection.GeneratedProtocolMessageType('UserInfo', (_message.Message,), {
  'DESCRIPTOR' : _USERINFO,
  '__module__' : 'uac.UACService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.uac.UserInfo)
  })
_sym_db.RegisterMessage(UserInfo)

CreateUser = _reflection.GeneratedProtocolMessageType('CreateUser', (_message.Message,), {

  'Response' : _reflection.GeneratedProtocolMessageType('Response', (_message.Message,), {
    'DESCRIPTOR' : _CREATEUSER_RESPONSE,
    '__module__' : 'uac.UACService_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.uac.CreateUser.Response)
    })
  ,
  'DESCRIPTOR' : _CREATEUSER,
  '__module__' : 'uac.UACService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.uac.CreateUser)
  })
_sym_db.RegisterMessage(CreateUser)
_sym_db.RegisterMessage(CreateUser.Response)

UpdateUser = _reflection.GeneratedProtocolMessageType('UpdateUser', (_message.Message,), {

  'Response' : _reflection.GeneratedProtocolMessageType('Response', (_message.Message,), {
    'DESCRIPTOR' : _UPDATEUSER_RESPONSE,
    '__module__' : 'uac.UACService_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.uac.UpdateUser.Response)
    })
  ,
  'DESCRIPTOR' : _UPDATEUSER,
  '__module__' : 'uac.UACService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.uac.UpdateUser)
  })
_sym_db.RegisterMessage(UpdateUser)
_sym_db.RegisterMessage(UpdateUser.Response)

DeleteUser = _reflection.GeneratedProtocolMessageType('DeleteUser', (_message.Message,), {

  'Response' : _reflection.GeneratedProtocolMessageType('Response', (_message.Message,), {
    'DESCRIPTOR' : _DELETEUSER_RESPONSE,
    '__module__' : 'uac.UACService_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.uac.DeleteUser.Response)
    })
  ,
  'DESCRIPTOR' : _DELETEUSER,
  '__module__' : 'uac.UACService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.uac.DeleteUser)
  })
_sym_db.RegisterMessage(DeleteUser)
_sym_db.RegisterMessage(DeleteUser.Response)


DESCRIPTOR._options = None

_UACSERVICE = _descriptor.ServiceDescriptor(
  name='UACService',
  full_name='ai.verta.uac.UACService',
  file=DESCRIPTOR,
  index=0,
  serialized_options=None,
  serialized_start=1544,
  serialized_end=2268,
  methods=[
  _descriptor.MethodDescriptor(
    name='getCurrentUser',
    full_name='ai.verta.uac.UACService.getCurrentUser',
    index=0,
    containing_service=None,
    input_type=_EMPTY,
    output_type=_USERINFO,
    serialized_options=b'\202\323\344\223\002\030\022\026/v1/uac/getCurrentUser',
  ),
  _descriptor.MethodDescriptor(
    name='getUser',
    full_name='ai.verta.uac.UACService.getUser',
    index=1,
    containing_service=None,
    input_type=_GETUSER,
    output_type=_USERINFO,
    serialized_options=b'\202\323\344\223\002\021\022\017/v1/uac/getUser',
  ),
  _descriptor.MethodDescriptor(
    name='getUsers',
    full_name='ai.verta.uac.UACService.getUsers',
    index=2,
    containing_service=None,
    input_type=_GETUSERS,
    output_type=_GETUSERS_RESPONSE,
    serialized_options=b'\202\323\344\223\002\025\"\020/v1/uac/getUsers:\001*',
  ),
  _descriptor.MethodDescriptor(
    name='getUsersFuzzy',
    full_name='ai.verta.uac.UACService.getUsersFuzzy',
    index=3,
    containing_service=None,
    input_type=_GETUSERSFUZZY,
    output_type=_GETUSERSFUZZY_RESPONSE,
    serialized_options=b'\202\323\344\223\002\032\"\025/v1/uac/getUsersFuzzy:\001*',
  ),
  _descriptor.MethodDescriptor(
    name='createUser',
    full_name='ai.verta.uac.UACService.createUser',
    index=4,
    containing_service=None,
    input_type=_CREATEUSER,
    output_type=_CREATEUSER_RESPONSE,
    serialized_options=b'\202\323\344\223\002\027\"\022/v1/uac/createUser:\001*',
  ),
  _descriptor.MethodDescriptor(
    name='updateUser',
    full_name='ai.verta.uac.UACService.updateUser',
    index=5,
    containing_service=None,
    input_type=_UPDATEUSER,
    output_type=_UPDATEUSER_RESPONSE,
    serialized_options=b'\202\323\344\223\002\027\"\022/v1/uac/updateUser:\001*',
  ),
  _descriptor.MethodDescriptor(
    name='deleteUser',
    full_name='ai.verta.uac.UACService.deleteUser',
    index=6,
    containing_service=None,
    input_type=_DELETEUSER,
    output_type=_DELETEUSER_RESPONSE,
    serialized_options=b'\202\323\344\223\002\027\"\022/v1/uac/deleteUser:\001*',
  ),
])
_sym_db.RegisterServiceDescriptor(_UACSERVICE)

DESCRIPTOR.services_by_name['UACService'] = _UACSERVICE

# @@protoc_insertion_point(module_scope)
