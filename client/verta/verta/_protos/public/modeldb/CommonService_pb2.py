# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: protos/public/modeldb/CommonService.proto

import sys
_b=sys.version_info[0]<3 and (lambda x:x) or (lambda x:x.encode('latin1'))
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from google.api import annotations_pb2 as google_dot_api_dot_annotations__pb2
from google.protobuf import struct_pb2 as google_dot_protobuf_dot_struct__pb2
from ...public.common import CommonService_pb2 as protos_dot_public_dot_common_dot_CommonService__pb2


DESCRIPTOR = _descriptor.FileDescriptor(
  name='protos/public/modeldb/CommonService.proto',
  package='ai.verta.modeldb',
  syntax='proto3',
  serialized_options=_b('P\001Z>github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb'),
  serialized_pb=_b('\n)protos/public/modeldb/CommonService.proto\x12\x10\x61i.verta.modeldb\x1a\x1cgoogle/api/annotations.proto\x1a\x1cgoogle/protobuf/struct.proto\x1a(protos/public/common/CommonService.proto\"s\n\x10\x41rtifactTypeEnum\"_\n\x0c\x41rtifactType\x12\t\n\x05IMAGE\x10\x00\x12\t\n\x05MODEL\x10\x01\x12\x0f\n\x0bTENSORBOARD\x10\x02\x12\x08\n\x04\x44\x41TA\x10\x03\x12\x08\n\x04\x42LOB\x10\x04\x12\n\n\x06STRING\x10\x05\x12\x08\n\x04\x43ODE\x10\x06\"\xb8\x01\n\x08\x41rtifact\x12\x0b\n\x03key\x18\x01 \x01(\t\x12\x0c\n\x04path\x18\x02 \x01(\t\x12\x11\n\tpath_only\x18\x03 \x01(\x08\x12\x46\n\rartifact_type\x18\x04 \x01(\x0e\x32/.ai.verta.modeldb.ArtifactTypeEnum.ArtifactType\x12\x1a\n\x12linked_artifact_id\x18\x05 \x01(\t\x12\x1a\n\x12\x66ilename_extension\x18\x06 \x01(\t\"\x17\n\x07\x46\x65\x61ture\x12\x0c\n\x04name\x18\x01 \x01(\t\"\x7f\n\rGetAttributes\x12\n\n\x02id\x18\x01 \x01(\t\x12\x16\n\x0e\x61ttribute_keys\x18\x02 \x03(\t\x12\x0f\n\x07get_all\x18\x03 \x01(\x08\x1a\x39\n\x08Response\x12-\n\nattributes\x18\x01 \x03(\x0b\x32\x19.ai.verta.common.KeyValue\"e\n\rAddAttributes\x12\n\n\x02id\x18\x01 \x01(\t\x12,\n\tattribute\x18\x02 \x01(\x0b\x32\x19.ai.verta.common.KeyValue\x1a\x1a\n\x08Response\x12\x0e\n\x06status\x18\x01 \x01(\x08\"/\n\x07GetTags\x12\n\n\x02id\x18\x01 \x01(\t\x1a\x18\n\x08Response\x12\x0c\n\x04tags\x18\x01 \x03(\t\"\x95\x01\n\x0b\x43odeVersion\x12\x35\n\x0cgit_snapshot\x18\x01 \x01(\x0b\x32\x1d.ai.verta.modeldb.GitSnapshotH\x00\x12\x32\n\x0c\x63ode_archive\x18\x02 \x01(\x0b\x32\x1a.ai.verta.modeldb.ArtifactH\x00\x12\x13\n\x0b\x64\x61te_logged\x18\x03 \x01(\x04\x42\x06\n\x04\x63ode\"t\n\x0bGitSnapshot\x12\x11\n\tfilepaths\x18\x01 \x03(\t\x12\x0c\n\x04repo\x18\x02 \x01(\t\x12\x0c\n\x04hash\x18\x03 \x01(\t\x12\x36\n\x08is_dirty\x18\x04 \x01(\x0e\x32$.ai.verta.common.TernaryEnum.Ternary\"\xbc\x01\n\rKeyValueQuery\x12\x0b\n\x03key\x18\x01 \x01(\t\x12%\n\x05value\x18\x02 \x01(\x0b\x32\x16.google.protobuf.Value\x12<\n\nvalue_type\x18\x03 \x01(\x0e\x32(.ai.verta.common.ValueTypeEnum.ValueType\x12\x39\n\x08operator\x18\x04 \x01(\x0e\x32\'.ai.verta.modeldb.OperatorEnum.Operator\"r\n\x0cOperatorEnum\"b\n\x08Operator\x12\x06\n\x02\x45Q\x10\x00\x12\x06\n\x02NE\x10\x01\x12\x06\n\x02GT\x10\x02\x12\x07\n\x03GTE\x10\x03\x12\x06\n\x02LT\x10\x04\x12\x07\n\x03LTE\x10\x05\x12\x0b\n\x07\x43ONTAIN\x10\x06\x12\x0f\n\x0bNOT_CONTAIN\x10\x07\x12\x06\n\x02IN\x10\x08\"\x97\x02\n\x11GetUrlForArtifact\x12\n\n\x02id\x18\x01 \x01(\t\x12\x0b\n\x03key\x18\x02 \x01(\t\x12\x0e\n\x06method\x18\x03 \x01(\t\x12\x46\n\rartifact_type\x18\x04 \x01(\x0e\x32/.ai.verta.modeldb.ArtifactTypeEnum.ArtifactType\x1a\x90\x01\n\x08Response\x12\x0b\n\x03url\x18\x01 \x01(\t\x12H\n\x06\x66ields\x18\x02 \x03(\x0b\x32\x38.ai.verta.modeldb.GetUrlForArtifact.Response.FieldsEntry\x1a-\n\x0b\x46ieldsEntry\x12\x0b\n\x03key\x18\x01 \x01(\t\x12\r\n\x05value\x18\x02 \x01(\t:\x02\x38\x01\"b\n\x0cGetArtifacts\x12\n\n\x02id\x18\x01 \x01(\t\x12\x0b\n\x03key\x18\x02 \x01(\t\x1a\x39\n\x08Response\x12-\n\tartifacts\x18\x01 \x03(\x0b\x32\x1a.ai.verta.modeldb.Artifact\"M\n\x11WorkspaceTypeEnum\"8\n\rWorkspaceType\x12\x0b\n\x07UNKNOWN\x10\x00\x12\x10\n\x0cORGANIZATION\x10\x01\x12\x08\n\x04USER\x10\x02\x42\x42P\x01Z>github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldbb\x06proto3')
  ,
  dependencies=[google_dot_api_dot_annotations__pb2.DESCRIPTOR,google_dot_protobuf_dot_struct__pb2.DESCRIPTOR,protos_dot_public_dot_common_dot_CommonService__pb2.DESCRIPTOR,])



_ARTIFACTTYPEENUM_ARTIFACTTYPE = _descriptor.EnumDescriptor(
  name='ArtifactType',
  full_name='ai.verta.modeldb.ArtifactTypeEnum.ArtifactType',
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
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=185,
  serialized_end=280,
)
_sym_db.RegisterEnumDescriptor(_ARTIFACTTYPEENUM_ARTIFACTTYPE)

_OPERATORENUM_OPERATOR = _descriptor.EnumDescriptor(
  name='Operator',
  full_name='ai.verta.modeldb.OperatorEnum.Operator',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='EQ', index=0, number=0,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='NE', index=1, number=1,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='GT', index=2, number=2,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='GTE', index=3, number=3,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='LT', index=4, number=4,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='LTE', index=5, number=5,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='CONTAIN', index=6, number=6,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='NOT_CONTAIN', index=7, number=7,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='IN', index=8, number=8,
      serialized_options=None,
      type=None),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=1252,
  serialized_end=1350,
)
_sym_db.RegisterEnumDescriptor(_OPERATORENUM_OPERATOR)

_WORKSPACETYPEENUM_WORKSPACETYPE = _descriptor.EnumDescriptor(
  name='WorkspaceType',
  full_name='ai.verta.modeldb.WorkspaceTypeEnum.WorkspaceType',
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
  serialized_start=1755,
  serialized_end=1811,
)
_sym_db.RegisterEnumDescriptor(_WORKSPACETYPEENUM_WORKSPACETYPE)


_ARTIFACTTYPEENUM = _descriptor.Descriptor(
  name='ArtifactTypeEnum',
  full_name='ai.verta.modeldb.ArtifactTypeEnum',
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
  serialized_start=165,
  serialized_end=280,
)


_ARTIFACT = _descriptor.Descriptor(
  name='Artifact',
  full_name='ai.verta.modeldb.Artifact',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='key', full_name='ai.verta.modeldb.Artifact.key', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='path', full_name='ai.verta.modeldb.Artifact.path', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='path_only', full_name='ai.verta.modeldb.Artifact.path_only', index=2,
      number=3, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='artifact_type', full_name='ai.verta.modeldb.Artifact.artifact_type', index=3,
      number=4, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='linked_artifact_id', full_name='ai.verta.modeldb.Artifact.linked_artifact_id', index=4,
      number=5, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='filename_extension', full_name='ai.verta.modeldb.Artifact.filename_extension', index=5,
      number=6, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
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
  serialized_start=283,
  serialized_end=467,
)


_FEATURE = _descriptor.Descriptor(
  name='Feature',
  full_name='ai.verta.modeldb.Feature',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='name', full_name='ai.verta.modeldb.Feature.name', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
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
  serialized_start=469,
  serialized_end=492,
)


_GETATTRIBUTES_RESPONSE = _descriptor.Descriptor(
  name='Response',
  full_name='ai.verta.modeldb.GetAttributes.Response',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='attributes', full_name='ai.verta.modeldb.GetAttributes.Response.attributes', index=0,
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
  serialized_start=564,
  serialized_end=621,
)

_GETATTRIBUTES = _descriptor.Descriptor(
  name='GetAttributes',
  full_name='ai.verta.modeldb.GetAttributes',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='id', full_name='ai.verta.modeldb.GetAttributes.id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='attribute_keys', full_name='ai.verta.modeldb.GetAttributes.attribute_keys', index=1,
      number=2, type=9, cpp_type=9, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='get_all', full_name='ai.verta.modeldb.GetAttributes.get_all', index=2,
      number=3, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_GETATTRIBUTES_RESPONSE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=494,
  serialized_end=621,
)


_ADDATTRIBUTES_RESPONSE = _descriptor.Descriptor(
  name='Response',
  full_name='ai.verta.modeldb.AddAttributes.Response',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='status', full_name='ai.verta.modeldb.AddAttributes.Response.status', index=0,
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
  serialized_start=698,
  serialized_end=724,
)

_ADDATTRIBUTES = _descriptor.Descriptor(
  name='AddAttributes',
  full_name='ai.verta.modeldb.AddAttributes',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='id', full_name='ai.verta.modeldb.AddAttributes.id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='attribute', full_name='ai.verta.modeldb.AddAttributes.attribute', index=1,
      number=2, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_ADDATTRIBUTES_RESPONSE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=623,
  serialized_end=724,
)


_GETTAGS_RESPONSE = _descriptor.Descriptor(
  name='Response',
  full_name='ai.verta.modeldb.GetTags.Response',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='tags', full_name='ai.verta.modeldb.GetTags.Response.tags', index=0,
      number=1, type=9, cpp_type=9, label=3,
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
  serialized_start=749,
  serialized_end=773,
)

_GETTAGS = _descriptor.Descriptor(
  name='GetTags',
  full_name='ai.verta.modeldb.GetTags',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='id', full_name='ai.verta.modeldb.GetTags.id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_GETTAGS_RESPONSE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=726,
  serialized_end=773,
)


_CODEVERSION = _descriptor.Descriptor(
  name='CodeVersion',
  full_name='ai.verta.modeldb.CodeVersion',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='git_snapshot', full_name='ai.verta.modeldb.CodeVersion.git_snapshot', index=0,
      number=1, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='code_archive', full_name='ai.verta.modeldb.CodeVersion.code_archive', index=1,
      number=2, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='date_logged', full_name='ai.verta.modeldb.CodeVersion.date_logged', index=2,
      number=3, type=4, cpp_type=4, label=1,
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
    _descriptor.OneofDescriptor(
      name='code', full_name='ai.verta.modeldb.CodeVersion.code',
      index=0, containing_type=None, fields=[]),
  ],
  serialized_start=776,
  serialized_end=925,
)


_GITSNAPSHOT = _descriptor.Descriptor(
  name='GitSnapshot',
  full_name='ai.verta.modeldb.GitSnapshot',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='filepaths', full_name='ai.verta.modeldb.GitSnapshot.filepaths', index=0,
      number=1, type=9, cpp_type=9, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='repo', full_name='ai.verta.modeldb.GitSnapshot.repo', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='hash', full_name='ai.verta.modeldb.GitSnapshot.hash', index=2,
      number=3, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='is_dirty', full_name='ai.verta.modeldb.GitSnapshot.is_dirty', index=3,
      number=4, type=14, cpp_type=8, label=1,
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
  serialized_start=927,
  serialized_end=1043,
)


_KEYVALUEQUERY = _descriptor.Descriptor(
  name='KeyValueQuery',
  full_name='ai.verta.modeldb.KeyValueQuery',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='key', full_name='ai.verta.modeldb.KeyValueQuery.key', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='value', full_name='ai.verta.modeldb.KeyValueQuery.value', index=1,
      number=2, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='value_type', full_name='ai.verta.modeldb.KeyValueQuery.value_type', index=2,
      number=3, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='operator', full_name='ai.verta.modeldb.KeyValueQuery.operator', index=3,
      number=4, type=14, cpp_type=8, label=1,
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
  serialized_start=1046,
  serialized_end=1234,
)


_OPERATORENUM = _descriptor.Descriptor(
  name='OperatorEnum',
  full_name='ai.verta.modeldb.OperatorEnum',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
    _OPERATORENUM_OPERATOR,
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=1236,
  serialized_end=1350,
)


_GETURLFORARTIFACT_RESPONSE_FIELDSENTRY = _descriptor.Descriptor(
  name='FieldsEntry',
  full_name='ai.verta.modeldb.GetUrlForArtifact.Response.FieldsEntry',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='key', full_name='ai.verta.modeldb.GetUrlForArtifact.Response.FieldsEntry.key', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='value', full_name='ai.verta.modeldb.GetUrlForArtifact.Response.FieldsEntry.value', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=_b('8\001'),
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=1587,
  serialized_end=1632,
)

_GETURLFORARTIFACT_RESPONSE = _descriptor.Descriptor(
  name='Response',
  full_name='ai.verta.modeldb.GetUrlForArtifact.Response',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='url', full_name='ai.verta.modeldb.GetUrlForArtifact.Response.url', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='fields', full_name='ai.verta.modeldb.GetUrlForArtifact.Response.fields', index=1,
      number=2, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_GETURLFORARTIFACT_RESPONSE_FIELDSENTRY, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=1488,
  serialized_end=1632,
)

_GETURLFORARTIFACT = _descriptor.Descriptor(
  name='GetUrlForArtifact',
  full_name='ai.verta.modeldb.GetUrlForArtifact',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='id', full_name='ai.verta.modeldb.GetUrlForArtifact.id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='key', full_name='ai.verta.modeldb.GetUrlForArtifact.key', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='method', full_name='ai.verta.modeldb.GetUrlForArtifact.method', index=2,
      number=3, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='artifact_type', full_name='ai.verta.modeldb.GetUrlForArtifact.artifact_type', index=3,
      number=4, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_GETURLFORARTIFACT_RESPONSE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=1353,
  serialized_end=1632,
)


_GETARTIFACTS_RESPONSE = _descriptor.Descriptor(
  name='Response',
  full_name='ai.verta.modeldb.GetArtifacts.Response',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='artifacts', full_name='ai.verta.modeldb.GetArtifacts.Response.artifacts', index=0,
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
  serialized_start=1675,
  serialized_end=1732,
)

_GETARTIFACTS = _descriptor.Descriptor(
  name='GetArtifacts',
  full_name='ai.verta.modeldb.GetArtifacts',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='id', full_name='ai.verta.modeldb.GetArtifacts.id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='key', full_name='ai.verta.modeldb.GetArtifacts.key', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_GETARTIFACTS_RESPONSE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=1634,
  serialized_end=1732,
)


_WORKSPACETYPEENUM = _descriptor.Descriptor(
  name='WorkspaceTypeEnum',
  full_name='ai.verta.modeldb.WorkspaceTypeEnum',
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
  serialized_start=1734,
  serialized_end=1811,
)

_ARTIFACTTYPEENUM_ARTIFACTTYPE.containing_type = _ARTIFACTTYPEENUM
_ARTIFACT.fields_by_name['artifact_type'].enum_type = _ARTIFACTTYPEENUM_ARTIFACTTYPE
_GETATTRIBUTES_RESPONSE.fields_by_name['attributes'].message_type = protos_dot_public_dot_common_dot_CommonService__pb2._KEYVALUE
_GETATTRIBUTES_RESPONSE.containing_type = _GETATTRIBUTES
_ADDATTRIBUTES_RESPONSE.containing_type = _ADDATTRIBUTES
_ADDATTRIBUTES.fields_by_name['attribute'].message_type = protos_dot_public_dot_common_dot_CommonService__pb2._KEYVALUE
_GETTAGS_RESPONSE.containing_type = _GETTAGS
_CODEVERSION.fields_by_name['git_snapshot'].message_type = _GITSNAPSHOT
_CODEVERSION.fields_by_name['code_archive'].message_type = _ARTIFACT
_CODEVERSION.oneofs_by_name['code'].fields.append(
  _CODEVERSION.fields_by_name['git_snapshot'])
_CODEVERSION.fields_by_name['git_snapshot'].containing_oneof = _CODEVERSION.oneofs_by_name['code']
_CODEVERSION.oneofs_by_name['code'].fields.append(
  _CODEVERSION.fields_by_name['code_archive'])
_CODEVERSION.fields_by_name['code_archive'].containing_oneof = _CODEVERSION.oneofs_by_name['code']
_GITSNAPSHOT.fields_by_name['is_dirty'].enum_type = protos_dot_public_dot_common_dot_CommonService__pb2._TERNARYENUM_TERNARY
_KEYVALUEQUERY.fields_by_name['value'].message_type = google_dot_protobuf_dot_struct__pb2._VALUE
_KEYVALUEQUERY.fields_by_name['value_type'].enum_type = protos_dot_public_dot_common_dot_CommonService__pb2._VALUETYPEENUM_VALUETYPE
_KEYVALUEQUERY.fields_by_name['operator'].enum_type = _OPERATORENUM_OPERATOR
_OPERATORENUM_OPERATOR.containing_type = _OPERATORENUM
_GETURLFORARTIFACT_RESPONSE_FIELDSENTRY.containing_type = _GETURLFORARTIFACT_RESPONSE
_GETURLFORARTIFACT_RESPONSE.fields_by_name['fields'].message_type = _GETURLFORARTIFACT_RESPONSE_FIELDSENTRY
_GETURLFORARTIFACT_RESPONSE.containing_type = _GETURLFORARTIFACT
_GETURLFORARTIFACT.fields_by_name['artifact_type'].enum_type = _ARTIFACTTYPEENUM_ARTIFACTTYPE
_GETARTIFACTS_RESPONSE.fields_by_name['artifacts'].message_type = _ARTIFACT
_GETARTIFACTS_RESPONSE.containing_type = _GETARTIFACTS
_WORKSPACETYPEENUM_WORKSPACETYPE.containing_type = _WORKSPACETYPEENUM
DESCRIPTOR.message_types_by_name['ArtifactTypeEnum'] = _ARTIFACTTYPEENUM
DESCRIPTOR.message_types_by_name['Artifact'] = _ARTIFACT
DESCRIPTOR.message_types_by_name['Feature'] = _FEATURE
DESCRIPTOR.message_types_by_name['GetAttributes'] = _GETATTRIBUTES
DESCRIPTOR.message_types_by_name['AddAttributes'] = _ADDATTRIBUTES
DESCRIPTOR.message_types_by_name['GetTags'] = _GETTAGS
DESCRIPTOR.message_types_by_name['CodeVersion'] = _CODEVERSION
DESCRIPTOR.message_types_by_name['GitSnapshot'] = _GITSNAPSHOT
DESCRIPTOR.message_types_by_name['KeyValueQuery'] = _KEYVALUEQUERY
DESCRIPTOR.message_types_by_name['OperatorEnum'] = _OPERATORENUM
DESCRIPTOR.message_types_by_name['GetUrlForArtifact'] = _GETURLFORARTIFACT
DESCRIPTOR.message_types_by_name['GetArtifacts'] = _GETARTIFACTS
DESCRIPTOR.message_types_by_name['WorkspaceTypeEnum'] = _WORKSPACETYPEENUM
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

ArtifactTypeEnum = _reflection.GeneratedProtocolMessageType('ArtifactTypeEnum', (_message.Message,), dict(
  DESCRIPTOR = _ARTIFACTTYPEENUM,
  __module__ = 'protos.public.modeldb.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.ArtifactTypeEnum)
  ))
_sym_db.RegisterMessage(ArtifactTypeEnum)

Artifact = _reflection.GeneratedProtocolMessageType('Artifact', (_message.Message,), dict(
  DESCRIPTOR = _ARTIFACT,
  __module__ = 'protos.public.modeldb.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.Artifact)
  ))
_sym_db.RegisterMessage(Artifact)

Feature = _reflection.GeneratedProtocolMessageType('Feature', (_message.Message,), dict(
  DESCRIPTOR = _FEATURE,
  __module__ = 'protos.public.modeldb.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.Feature)
  ))
_sym_db.RegisterMessage(Feature)

GetAttributes = _reflection.GeneratedProtocolMessageType('GetAttributes', (_message.Message,), dict(

  Response = _reflection.GeneratedProtocolMessageType('Response', (_message.Message,), dict(
    DESCRIPTOR = _GETATTRIBUTES_RESPONSE,
    __module__ = 'protos.public.modeldb.CommonService_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.modeldb.GetAttributes.Response)
    ))
  ,
  DESCRIPTOR = _GETATTRIBUTES,
  __module__ = 'protos.public.modeldb.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.GetAttributes)
  ))
_sym_db.RegisterMessage(GetAttributes)
_sym_db.RegisterMessage(GetAttributes.Response)

AddAttributes = _reflection.GeneratedProtocolMessageType('AddAttributes', (_message.Message,), dict(

  Response = _reflection.GeneratedProtocolMessageType('Response', (_message.Message,), dict(
    DESCRIPTOR = _ADDATTRIBUTES_RESPONSE,
    __module__ = 'protos.public.modeldb.CommonService_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.modeldb.AddAttributes.Response)
    ))
  ,
  DESCRIPTOR = _ADDATTRIBUTES,
  __module__ = 'protos.public.modeldb.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.AddAttributes)
  ))
_sym_db.RegisterMessage(AddAttributes)
_sym_db.RegisterMessage(AddAttributes.Response)

GetTags = _reflection.GeneratedProtocolMessageType('GetTags', (_message.Message,), dict(

  Response = _reflection.GeneratedProtocolMessageType('Response', (_message.Message,), dict(
    DESCRIPTOR = _GETTAGS_RESPONSE,
    __module__ = 'protos.public.modeldb.CommonService_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.modeldb.GetTags.Response)
    ))
  ,
  DESCRIPTOR = _GETTAGS,
  __module__ = 'protos.public.modeldb.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.GetTags)
  ))
_sym_db.RegisterMessage(GetTags)
_sym_db.RegisterMessage(GetTags.Response)

CodeVersion = _reflection.GeneratedProtocolMessageType('CodeVersion', (_message.Message,), dict(
  DESCRIPTOR = _CODEVERSION,
  __module__ = 'protos.public.modeldb.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.CodeVersion)
  ))
_sym_db.RegisterMessage(CodeVersion)

GitSnapshot = _reflection.GeneratedProtocolMessageType('GitSnapshot', (_message.Message,), dict(
  DESCRIPTOR = _GITSNAPSHOT,
  __module__ = 'protos.public.modeldb.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.GitSnapshot)
  ))
_sym_db.RegisterMessage(GitSnapshot)

KeyValueQuery = _reflection.GeneratedProtocolMessageType('KeyValueQuery', (_message.Message,), dict(
  DESCRIPTOR = _KEYVALUEQUERY,
  __module__ = 'protos.public.modeldb.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.KeyValueQuery)
  ))
_sym_db.RegisterMessage(KeyValueQuery)

OperatorEnum = _reflection.GeneratedProtocolMessageType('OperatorEnum', (_message.Message,), dict(
  DESCRIPTOR = _OPERATORENUM,
  __module__ = 'protos.public.modeldb.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.OperatorEnum)
  ))
_sym_db.RegisterMessage(OperatorEnum)

GetUrlForArtifact = _reflection.GeneratedProtocolMessageType('GetUrlForArtifact', (_message.Message,), dict(

  Response = _reflection.GeneratedProtocolMessageType('Response', (_message.Message,), dict(

    FieldsEntry = _reflection.GeneratedProtocolMessageType('FieldsEntry', (_message.Message,), dict(
      DESCRIPTOR = _GETURLFORARTIFACT_RESPONSE_FIELDSENTRY,
      __module__ = 'protos.public.modeldb.CommonService_pb2'
      # @@protoc_insertion_point(class_scope:ai.verta.modeldb.GetUrlForArtifact.Response.FieldsEntry)
      ))
    ,
    DESCRIPTOR = _GETURLFORARTIFACT_RESPONSE,
    __module__ = 'protos.public.modeldb.CommonService_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.modeldb.GetUrlForArtifact.Response)
    ))
  ,
  DESCRIPTOR = _GETURLFORARTIFACT,
  __module__ = 'protos.public.modeldb.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.GetUrlForArtifact)
  ))
_sym_db.RegisterMessage(GetUrlForArtifact)
_sym_db.RegisterMessage(GetUrlForArtifact.Response)
_sym_db.RegisterMessage(GetUrlForArtifact.Response.FieldsEntry)

GetArtifacts = _reflection.GeneratedProtocolMessageType('GetArtifacts', (_message.Message,), dict(

  Response = _reflection.GeneratedProtocolMessageType('Response', (_message.Message,), dict(
    DESCRIPTOR = _GETARTIFACTS_RESPONSE,
    __module__ = 'protos.public.modeldb.CommonService_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.modeldb.GetArtifacts.Response)
    ))
  ,
  DESCRIPTOR = _GETARTIFACTS,
  __module__ = 'protos.public.modeldb.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.GetArtifacts)
  ))
_sym_db.RegisterMessage(GetArtifacts)
_sym_db.RegisterMessage(GetArtifacts.Response)

WorkspaceTypeEnum = _reflection.GeneratedProtocolMessageType('WorkspaceTypeEnum', (_message.Message,), dict(
  DESCRIPTOR = _WORKSPACETYPEENUM,
  __module__ = 'protos.public.modeldb.CommonService_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.WorkspaceTypeEnum)
  ))
_sym_db.RegisterMessage(WorkspaceTypeEnum)


DESCRIPTOR._options = None
_GETURLFORARTIFACT_RESPONSE_FIELDSENTRY._options = None
# @@protoc_insertion_point(module_scope)
