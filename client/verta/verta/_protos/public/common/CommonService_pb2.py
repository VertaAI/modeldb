# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: common/CommonService.proto

from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from google.protobuf import struct_pb2 as google_dot_protobuf_dot_struct__pb2


DESCRIPTOR = _descriptor.FileDescriptor(
  name='common/CommonService.proto',
  package='ai.verta.common',
  syntax='proto3',
  serialized_options=b'P\001Z=github.com/VertaAI/modeldb/protos/gen/go/protos/public/common',
  serialized_pb=b'\n\x1a\x63ommon/CommonService.proto\x12\x0f\x61i.verta.common\x1a\x1cgoogle/protobuf/struct.proto\":\n\x0bTernaryEnum\"+\n\x07Ternary\x12\x0b\n\x07UNKNOWN\x10\x00\x12\x08\n\x04TRUE\x10\x01\x12\t\n\x05\x46\x41LSE\x10\x02\"|\n\x08KeyValue\x12\x0b\n\x03key\x18\x01 \x01(\t\x12%\n\x05value\x18\x02 \x01(\x0b\x32\x16.google.protobuf.Value\x12<\n\nvalue_type\x18\x03 \x01(\x0e\x32(.ai.verta.common.ValueTypeEnum.ValueType\"H\n\rValueTypeEnum\"7\n\tValueType\x12\n\n\x06STRING\x10\x00\x12\n\n\x06NUMBER\x10\x01\x12\x08\n\x04LIST\x10\x02\x12\x08\n\x04\x42LOB\x10\x03\"I\n\x14\x43ollaboratorTypeEnum\"1\n\x10\x43ollaboratorType\x12\r\n\tREAD_ONLY\x10\x00\x12\x0e\n\nREAD_WRITE\x10\x01\"R\n\x0c\x45ntitiesEnum\"B\n\rEntitiesTypes\x12\x0b\n\x07UNKNOWN\x10\x00\x12\x10\n\x0cORGANIZATION\x10\x01\x12\x08\n\x04TEAM\x10\x02\x12\x08\n\x04USER\x10\x03\"\xbd\x01\n\x13ModelDBResourceEnum\"\xa5\x01\n\x1bModelDBServiceResourceTypes\x12\x0b\n\x07UNKNOWN\x10\x00\x12\x07\n\x03\x41LL\x10\x01\x12\x0b\n\x07PROJECT\x10\x02\x12\x0e\n\nEXPERIMENT\x10\x03\x12\x12\n\x0e\x45XPERIMENT_RUN\x10\x04\x12\x0b\n\x07\x44\x41TASET\x10\x05\x12\x13\n\x0f\x44\x41TASET_VERSION\x10\x06\x12\r\n\tDASHBOARD\x10\x07\x12\x0e\n\nREPOSITORY\x10\x08\"5\n\nPagination\x12\x13\n\x0bpage_number\x18\x02 \x01(\x05\x12\x12\n\npage_limit\x18\x03 \x01(\x05\"M\n\x11WorkspaceTypeEnum\"8\n\rWorkspaceType\x12\x0b\n\x07UNKNOWN\x10\x00\x12\x10\n\x0cORGANIZATION\x10\x01\x12\x08\n\x04USER\x10\x02\"\x82\x01\n\x10\x41rtifactTypeEnum\"n\n\x0c\x41rtifactType\x12\t\n\x05IMAGE\x10\x00\x12\t\n\x05MODEL\x10\x01\x12\x0f\n\x0bTENSORBOARD\x10\x02\x12\x08\n\x04\x44\x41TA\x10\x03\x12\x08\n\x04\x42LOB\x10\x04\x12\n\n\x06STRING\x10\x05\x12\x08\n\x04\x43ODE\x10\x06\x12\r\n\tCONTAINER\x10\x07\"\xb7\x01\n\x08\x41rtifact\x12\x0b\n\x03key\x18\x01 \x01(\t\x12\x0c\n\x04path\x18\x02 \x01(\t\x12\x11\n\tpath_only\x18\x03 \x01(\x08\x12\x45\n\rartifact_type\x18\x04 \x01(\x0e\x32..ai.verta.common.ArtifactTypeEnum.ArtifactType\x12\x1a\n\x12linked_artifact_id\x18\x05 \x01(\t\x12\x1a\n\x12\x66ilename_extension\x18\x06 \x01(\tBAP\x01Z=github.com/VertaAI/modeldb/protos/gen/go/protos/public/commonb\x06proto3'
  ,
  dependencies=[google_dot_protobuf_dot_struct__pb2.DESCRIPTOR,])



_TERNARYENUM_TERNARY = _descriptor.EnumDescriptor(
  name='Ternary',
  full_name='ai.verta.common.TernaryEnum.Ternary',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='UNKNOWN', index=0, number=0,
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
  serialized_start=92,
  serialized_end=135,
)
_sym_db.RegisterEnumDescriptor(_TERNARYENUM_TERNARY)

_VALUETYPEENUM_VALUETYPE = _descriptor.EnumDescriptor(
  name='ValueType',
  full_name='ai.verta.common.ValueTypeEnum.ValueType',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='STRING', index=0, number=0,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='NUMBER', index=1, number=1,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='LIST', index=2, number=2,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='BLOB', index=3, number=3,
      serialized_options=None,
      type=None),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=280,
  serialized_end=335,
)
_sym_db.RegisterEnumDescriptor(_VALUETYPEENUM_VALUETYPE)

_COLLABORATORTYPEENUM_COLLABORATORTYPE = _descriptor.EnumDescriptor(
  name='CollaboratorType',
  full_name='ai.verta.common.CollaboratorTypeEnum.CollaboratorType',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='READ_ONLY', index=0, number=0,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='READ_WRITE', index=1, number=1,
      serialized_options=None,
      type=None),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=361,
  serialized_end=410,
)
_sym_db.RegisterEnumDescriptor(_COLLABORATORTYPEENUM_COLLABORATORTYPE)

_ENTITIESENUM_ENTITIESTYPES = _descriptor.EnumDescriptor(
  name='EntitiesTypes',
  full_name='ai.verta.common.EntitiesEnum.EntitiesTypes',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='UNKNOWN', index=0, number=0,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='ORGANIZATION', index=1, number=1,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='TEAM', index=2, number=2,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='USER', index=3, number=3,
      serialized_options=None,
      type=None),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=428,
  serialized_end=494,
)
_sym_db.RegisterEnumDescriptor(_ENTITIESENUM_ENTITIESTYPES)

_MODELDBRESOURCEENUM_MODELDBSERVICERESOURCETYPES = _descriptor.EnumDescriptor(
  name='ModelDBServiceResourceTypes',
  full_name='ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='UNKNOWN', index=0, number=0,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='ALL', index=1, number=1,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='PROJECT', index=2, number=2,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='EXPERIMENT', index=3, number=3,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='EXPERIMENT_RUN', index=4, number=4,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='DATASET', index=5, number=5,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='DATASET_VERSION', index=6, number=6,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='DASHBOARD', index=7, number=7,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='REPOSITORY', index=8, number=8,
      serialized_options=None,
      type=None),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=521,
  serialized_end=686,
)
_sym_db.RegisterEnumDescriptor(_MODELDBRESOURCEENUM_MODELDBSERVICERESOURCETYPES)

_WORKSPACETYPEENUM_WORKSPACETYPE = _descriptor.EnumDescriptor(
  name='WorkspaceType',
  full_name='ai.verta.common.WorkspaceTypeEnum.WorkspaceType',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='UNKNOWN', index=0, number=0,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='ORGANIZATION', index=1, number=1,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='USER', index=2, number=2,
      serialized_options=None,
      type=None),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=764,
  serialized_end=820,
)
_sym_db.RegisterEnumDescriptor(_WORKSPACETYPEENUM_WORKSPACETYPE)

_ARTIFACTTYPEENUM_ARTIFACTTYPE = _descriptor.EnumDescriptor(
  name='ArtifactType',
  full_name='ai.verta.common.ArtifactTypeEnum.ArtifactType',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='IMAGE', index=0, number=0,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='MODEL', index=1, number=1,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='TENSORBOARD', index=2, number=2,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='DATA', index=3, number=3,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='BLOB', index=4, number=4,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='STRING', index=5, number=5,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='CODE', index=6, number=6,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='CONTAINER', index=7, number=7,
      serialized_options=None,
      type=None),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=843,
  serialized_end=953,
)
_sym_db.RegisterEnumDescriptor(_ARTIFACTTYPEENUM_ARTIFACTTYPE)


_TERNARYENUM = _descriptor.Descriptor(
  name='TernaryEnum',
  full_name='ai.verta.common.TernaryEnum',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
    _TERNARYENUM_TERNARY,
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=77,
  serialized_end=135,
)


_KEYVALUE = _descriptor.Descriptor(
  name='KeyValue',
  full_name='ai.verta.common.KeyValue',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='key', full_name='ai.verta.common.KeyValue.key', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='value', full_name='ai.verta.common.KeyValue.value', index=1,
      number=2, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='value_type', full_name='ai.verta.common.KeyValue.value_type', index=2,
      number=3, type=14, cpp_type=8, label=1,
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
  serialized_start=137,
  serialized_end=261,
)


_VALUETYPEENUM = _descriptor.Descriptor(
  name='ValueTypeEnum',
  full_name='ai.verta.common.ValueTypeEnum',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
    _VALUETYPEENUM_VALUETYPE,
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=263,
  serialized_end=335,
)


_COLLABORATORTYPEENUM = _descriptor.Descriptor(
  name='CollaboratorTypeEnum',
  full_name='ai.verta.common.CollaboratorTypeEnum',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
    _COLLABORATORTYPEENUM_COLLABORATORTYPE,
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=337,
  serialized_end=410,
)


_ENTITIESENUM = _descriptor.Descriptor(
  name='EntitiesEnum',
  full_name='ai.verta.common.EntitiesEnum',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
    _ENTITIESENUM_ENTITIESTYPES,
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=412,
  serialized_end=494,
)


_MODELDBRESOURCEENUM = _descriptor.Descriptor(
  name='ModelDBResourceEnum',
  full_name='ai.verta.common.ModelDBResourceEnum',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
    _MODELDBRESOURCEENUM_MODELDBSERVICERESOURCETYPES,
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=497,
  serialized_end=686,
)


_PAGINATION = _descriptor.Descriptor(
  name='Pagination',
  full_name='ai.verta.common.Pagination',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='page_number', full_name='ai.verta.common.Pagination.page_number', index=0,
      number=2, type=5, cpp_type=1, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='page_limit', full_name='ai.verta.common.Pagination.page_limit', index=1,
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
  serialized_start=688,
  serialized_end=741,
)


_WORKSPACETYPEENUM = _descriptor.Descriptor(
  name='WorkspaceTypeEnum',
  full_name='ai.verta.common.WorkspaceTypeEnum',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
    _WORKSPACETYPEENUM_WORKSPACETYPE,
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=743,
  serialized_end=820,
)


_ARTIFACTTYPEENUM = _descriptor.Descriptor(
  name='ArtifactTypeEnum',
  full_name='ai.verta.common.ArtifactTypeEnum',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
    _ARTIFACTTYPEENUM_ARTIFACTTYPE,
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=823,
  serialized_end=953,
)


_ARTIFACT = _descriptor.Descriptor(
  name='Artifact',
  full_name='ai.verta.common.Artifact',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='key', full_name='ai.verta.common.Artifact.key', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='path', full_name='ai.verta.common.Artifact.path', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='path_only', full_name='ai.verta.common.Artifact.path_only', index=2,
      number=3, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='artifact_type', full_name='ai.verta.common.Artifact.artifact_type', index=3,
      number=4, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='linked_artifact_id', full_name='ai.verta.common.Artifact.linked_artifact_id', index=4,
      number=5, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='filename_extension', full_name='ai.verta.common.Artifact.filename_extension', index=5,
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
  serialized_start=956,
  serialized_end=1139,
)

_TERNARYENUM_TERNARY.containing_type = _TERNARYENUM
_KEYVALUE.fields_by_name['value'].message_type = google_dot_protobuf_dot_struct__pb2._VALUE
_KEYVALUE.fields_by_name['value_type'].enum_type = _VALUETYPEENUM_VALUETYPE
_VALUETYPEENUM_VALUETYPE.containing_type = _VALUETYPEENUM
_COLLABORATORTYPEENUM_COLLABORATORTYPE.containing_type = _COLLABORATORTYPEENUM
_ENTITIESENUM_ENTITIESTYPES.containing_type = _ENTITIESENUM
_MODELDBRESOURCEENUM_MODELDBSERVICERESOURCETYPES.containing_type = _MODELDBRESOURCEENUM
_WORKSPACETYPEENUM_WORKSPACETYPE.containing_type = _WORKSPACETYPEENUM
_ARTIFACTTYPEENUM_ARTIFACTTYPE.containing_type = _ARTIFACTTYPEENUM
_ARTIFACT.fields_by_name['artifact_type'].enum_type = _ARTIFACTTYPEENUM_ARTIFACTTYPE
DESCRIPTOR.message_types_by_name['TernaryEnum'] = _TERNARYENUM
DESCRIPTOR.message_types_by_name['KeyValue'] = _KEYVALUE
DESCRIPTOR.message_types_by_name['ValueTypeEnum'] = _VALUETYPEENUM
DESCRIPTOR.message_types_by_name['CollaboratorTypeEnum'] = _COLLABORATORTYPEENUM
DESCRIPTOR.message_types_by_name['EntitiesEnum'] = _ENTITIESENUM
DESCRIPTOR.message_types_by_name['ModelDBResourceEnum'] = _MODELDBRESOURCEENUM
DESCRIPTOR.message_types_by_name['Pagination'] = _PAGINATION
DESCRIPTOR.message_types_by_name['WorkspaceTypeEnum'] = _WORKSPACETYPEENUM
DESCRIPTOR.message_types_by_name['ArtifactTypeEnum'] = _ARTIFACTTYPEENUM
DESCRIPTOR.message_types_by_name['Artifact'] = _ARTIFACT
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

TernaryEnum = _reflection.GeneratedProtocolMessageType('TernaryEnum', (_message.Message,), {
  'DESCRIPTOR' : _TERNARYENUM,
  '__module__' : 'common.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.common.TernaryEnum)
  })
_sym_db.RegisterMessage(TernaryEnum)

KeyValue = _reflection.GeneratedProtocolMessageType('KeyValue', (_message.Message,), {
  'DESCRIPTOR' : _KEYVALUE,
  '__module__' : 'common.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.common.KeyValue)
  })
_sym_db.RegisterMessage(KeyValue)

ValueTypeEnum = _reflection.GeneratedProtocolMessageType('ValueTypeEnum', (_message.Message,), {
  'DESCRIPTOR' : _VALUETYPEENUM,
  '__module__' : 'common.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.common.ValueTypeEnum)
  })
_sym_db.RegisterMessage(ValueTypeEnum)

CollaboratorTypeEnum = _reflection.GeneratedProtocolMessageType('CollaboratorTypeEnum', (_message.Message,), {
  'DESCRIPTOR' : _COLLABORATORTYPEENUM,
  '__module__' : 'common.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.common.CollaboratorTypeEnum)
  })
_sym_db.RegisterMessage(CollaboratorTypeEnum)

EntitiesEnum = _reflection.GeneratedProtocolMessageType('EntitiesEnum', (_message.Message,), {
  'DESCRIPTOR' : _ENTITIESENUM,
  '__module__' : 'common.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.common.EntitiesEnum)
  })
_sym_db.RegisterMessage(EntitiesEnum)

ModelDBResourceEnum = _reflection.GeneratedProtocolMessageType('ModelDBResourceEnum', (_message.Message,), {
  'DESCRIPTOR' : _MODELDBRESOURCEENUM,
  '__module__' : 'common.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.common.ModelDBResourceEnum)
  })
_sym_db.RegisterMessage(ModelDBResourceEnum)

Pagination = _reflection.GeneratedProtocolMessageType('Pagination', (_message.Message,), {
  'DESCRIPTOR' : _PAGINATION,
  '__module__' : 'common.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.common.Pagination)
  })
_sym_db.RegisterMessage(Pagination)

WorkspaceTypeEnum = _reflection.GeneratedProtocolMessageType('WorkspaceTypeEnum', (_message.Message,), {
  'DESCRIPTOR' : _WORKSPACETYPEENUM,
  '__module__' : 'common.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.common.WorkspaceTypeEnum)
  })
_sym_db.RegisterMessage(WorkspaceTypeEnum)

ArtifactTypeEnum = _reflection.GeneratedProtocolMessageType('ArtifactTypeEnum', (_message.Message,), {
  'DESCRIPTOR' : _ARTIFACTTYPEENUM,
  '__module__' : 'common.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.common.ArtifactTypeEnum)
  })
_sym_db.RegisterMessage(ArtifactTypeEnum)

Artifact = _reflection.GeneratedProtocolMessageType('Artifact', (_message.Message,), {
  'DESCRIPTOR' : _ARTIFACT,
  '__module__' : 'common.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.common.Artifact)
  })
_sym_db.RegisterMessage(Artifact)


DESCRIPTOR._options = None
# @@protoc_insertion_point(module_scope)
