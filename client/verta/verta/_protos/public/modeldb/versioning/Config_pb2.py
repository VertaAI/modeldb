# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: protos/public/modeldb/versioning/Config.proto

import sys
_b=sys.version_info[0]<3 and (lambda x:x) or (lambda x:x.encode('latin1'))
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from ....public.modeldb.versioning import Enums_pb2 as protos_dot_public_dot_modeldb_dot_versioning_dot_Enums__pb2


DESCRIPTOR = _descriptor.FileDescriptor(
  name='protos/public/modeldb/versioning/Config.proto',
  package='ai.verta.modeldb.versioning',
  syntax='proto3',
  serialized_options=_b('P\001ZIgithub.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb/versioning'),
  serialized_pb=_b('\n-protos/public/modeldb/versioning/Config.proto\x12\x1b\x61i.verta.modeldb.versioning\x1a,protos/public/modeldb/versioning/Enums.proto\"\xb2\x01\n\nConfigBlob\x12T\n\x12hyperparameter_set\x18\x01 \x03(\x0b\x32\x38.ai.verta.modeldb.versioning.HyperparameterSetConfigBlob\x12N\n\x0fhyperparameters\x18\x02 \x03(\x0b\x32\x35.ai.verta.modeldb.versioning.HyperparameterConfigBlob\"t\n\x18HyperparameterConfigBlob\x12\x0c\n\x04name\x18\x01 \x01(\t\x12J\n\x05value\x18\x02 \x01(\x0b\x32;.ai.verta.modeldb.versioning.HyperparameterValuesConfigBlob\"m\n\x1eHyperparameterValuesConfigBlob\x12\x13\n\tint_value\x18\x01 \x01(\x03H\x00\x12\x15\n\x0b\x66loat_value\x18\x02 \x01(\x02H\x00\x12\x16\n\x0cstring_value\x18\x03 \x01(\tH\x00\x42\x07\n\x05value\"\xe4\x01\n\x1bHyperparameterSetConfigBlob\x12\x0c\n\x04name\x18\x01 \x01(\t\x12X\n\ncontinuous\x18\x02 \x01(\x0b\x32\x42.ai.verta.modeldb.versioning.ContinuousHyperparameterSetConfigBlobH\x00\x12T\n\x08\x64iscrete\x18\x03 \x01(\x0b\x32@.ai.verta.modeldb.versioning.DiscreteHyperparameterSetConfigBlobH\x00\x42\x07\n\x05value\"\xa3\x02\n%ContinuousHyperparameterSetConfigBlob\x12S\n\x0einterval_begin\x18\x02 \x01(\x0b\x32;.ai.verta.modeldb.versioning.HyperparameterValuesConfigBlob\x12Q\n\x0cinterval_end\x18\x03 \x01(\x0b\x32;.ai.verta.modeldb.versioning.HyperparameterValuesConfigBlob\x12R\n\rinterval_step\x18\x04 \x01(\x0b\x32;.ai.verta.modeldb.versioning.HyperparameterValuesConfigBlob\"r\n#DiscreteHyperparameterSetConfigBlob\x12K\n\x06values\x18\x04 \x03(\x0b\x32;.ai.verta.modeldb.versioning.HyperparameterValuesConfigBlob\"\xb2\x01\n\nConfigDiff\x12T\n\x12hyperparameter_set\x18\x01 \x03(\x0b\x32\x38.ai.verta.modeldb.versioning.HyperparameterSetConfigDiff\x12N\n\x0fhyperparameters\x18\x02 \x03(\x0b\x32\x35.ai.verta.modeldb.versioning.HyperparameterConfigDiff\"\xef\x01\n\x1bHyperparameterSetConfigDiff\x12\x46\n\x06status\x18\x01 \x01(\x0e\x32\x36.ai.verta.modeldb.versioning.DiffStatusEnum.DiffStatus\x12\x43\n\x01\x41\x18\x02 \x01(\x0b\x32\x38.ai.verta.modeldb.versioning.HyperparameterSetConfigBlob\x12\x43\n\x01\x42\x18\x03 \x01(\x0b\x32\x38.ai.verta.modeldb.versioning.HyperparameterSetConfigBlob\"\xe6\x01\n\x18HyperparameterConfigDiff\x12\x46\n\x06status\x18\x01 \x01(\x0e\x32\x36.ai.verta.modeldb.versioning.DiffStatusEnum.DiffStatus\x12@\n\x01\x41\x18\x02 \x01(\x0b\x32\x35.ai.verta.modeldb.versioning.HyperparameterConfigBlob\x12@\n\x01\x42\x18\x03 \x01(\x0b\x32\x35.ai.verta.modeldb.versioning.HyperparameterConfigBlobBMP\x01ZIgithub.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb/versioningb\x06proto3')
  ,
  dependencies=[protos_dot_public_dot_modeldb_dot_versioning_dot_Enums__pb2.DESCRIPTOR,])




_CONFIGBLOB = _descriptor.Descriptor(
  name='ConfigBlob',
  full_name='ai.verta.modeldb.versioning.ConfigBlob',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='hyperparameter_set', full_name='ai.verta.modeldb.versioning.ConfigBlob.hyperparameter_set', index=0,
      number=1, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='hyperparameters', full_name='ai.verta.modeldb.versioning.ConfigBlob.hyperparameters', index=1,
      number=2, type=11, cpp_type=10, label=3,
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
  serialized_start=125,
  serialized_end=303,
)


_HYPERPARAMETERCONFIGBLOB = _descriptor.Descriptor(
  name='HyperparameterConfigBlob',
  full_name='ai.verta.modeldb.versioning.HyperparameterConfigBlob',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='name', full_name='ai.verta.modeldb.versioning.HyperparameterConfigBlob.name', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='value', full_name='ai.verta.modeldb.versioning.HyperparameterConfigBlob.value', index=1,
      number=2, type=11, cpp_type=10, label=1,
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
  serialized_start=305,
  serialized_end=421,
)


_HYPERPARAMETERVALUESCONFIGBLOB = _descriptor.Descriptor(
  name='HyperparameterValuesConfigBlob',
  full_name='ai.verta.modeldb.versioning.HyperparameterValuesConfigBlob',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='int_value', full_name='ai.verta.modeldb.versioning.HyperparameterValuesConfigBlob.int_value', index=0,
      number=1, type=3, cpp_type=2, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='float_value', full_name='ai.verta.modeldb.versioning.HyperparameterValuesConfigBlob.float_value', index=1,
      number=2, type=2, cpp_type=6, label=1,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='string_value', full_name='ai.verta.modeldb.versioning.HyperparameterValuesConfigBlob.string_value', index=2,
      number=3, type=9, cpp_type=9, label=1,
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
    _descriptor.OneofDescriptor(
      name='value', full_name='ai.verta.modeldb.versioning.HyperparameterValuesConfigBlob.value',
      index=0, containing_type=None, fields=[]),
  ],
  serialized_start=423,
  serialized_end=532,
)


_HYPERPARAMETERSETCONFIGBLOB = _descriptor.Descriptor(
  name='HyperparameterSetConfigBlob',
  full_name='ai.verta.modeldb.versioning.HyperparameterSetConfigBlob',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='name', full_name='ai.verta.modeldb.versioning.HyperparameterSetConfigBlob.name', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='continuous', full_name='ai.verta.modeldb.versioning.HyperparameterSetConfigBlob.continuous', index=1,
      number=2, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='discrete', full_name='ai.verta.modeldb.versioning.HyperparameterSetConfigBlob.discrete', index=2,
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
    _descriptor.OneofDescriptor(
      name='value', full_name='ai.verta.modeldb.versioning.HyperparameterSetConfigBlob.value',
      index=0, containing_type=None, fields=[]),
  ],
  serialized_start=535,
  serialized_end=763,
)


_CONTINUOUSHYPERPARAMETERSETCONFIGBLOB = _descriptor.Descriptor(
  name='ContinuousHyperparameterSetConfigBlob',
  full_name='ai.verta.modeldb.versioning.ContinuousHyperparameterSetConfigBlob',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='interval_begin', full_name='ai.verta.modeldb.versioning.ContinuousHyperparameterSetConfigBlob.interval_begin', index=0,
      number=2, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='interval_end', full_name='ai.verta.modeldb.versioning.ContinuousHyperparameterSetConfigBlob.interval_end', index=1,
      number=3, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='interval_step', full_name='ai.verta.modeldb.versioning.ContinuousHyperparameterSetConfigBlob.interval_step', index=2,
      number=4, type=11, cpp_type=10, label=1,
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
  serialized_start=766,
  serialized_end=1057,
)


_DISCRETEHYPERPARAMETERSETCONFIGBLOB = _descriptor.Descriptor(
  name='DiscreteHyperparameterSetConfigBlob',
  full_name='ai.verta.modeldb.versioning.DiscreteHyperparameterSetConfigBlob',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='values', full_name='ai.verta.modeldb.versioning.DiscreteHyperparameterSetConfigBlob.values', index=0,
      number=4, type=11, cpp_type=10, label=3,
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
  serialized_start=1059,
  serialized_end=1173,
)


_CONFIGDIFF = _descriptor.Descriptor(
  name='ConfigDiff',
  full_name='ai.verta.modeldb.versioning.ConfigDiff',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='hyperparameter_set', full_name='ai.verta.modeldb.versioning.ConfigDiff.hyperparameter_set', index=0,
      number=1, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='hyperparameters', full_name='ai.verta.modeldb.versioning.ConfigDiff.hyperparameters', index=1,
      number=2, type=11, cpp_type=10, label=3,
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
  serialized_start=1176,
  serialized_end=1354,
)


_HYPERPARAMETERSETCONFIGDIFF = _descriptor.Descriptor(
  name='HyperparameterSetConfigDiff',
  full_name='ai.verta.modeldb.versioning.HyperparameterSetConfigDiff',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='status', full_name='ai.verta.modeldb.versioning.HyperparameterSetConfigDiff.status', index=0,
      number=1, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='A', full_name='ai.verta.modeldb.versioning.HyperparameterSetConfigDiff.A', index=1,
      number=2, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='B', full_name='ai.verta.modeldb.versioning.HyperparameterSetConfigDiff.B', index=2,
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
  serialized_start=1357,
  serialized_end=1596,
)


_HYPERPARAMETERCONFIGDIFF = _descriptor.Descriptor(
  name='HyperparameterConfigDiff',
  full_name='ai.verta.modeldb.versioning.HyperparameterConfigDiff',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='status', full_name='ai.verta.modeldb.versioning.HyperparameterConfigDiff.status', index=0,
      number=1, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='A', full_name='ai.verta.modeldb.versioning.HyperparameterConfigDiff.A', index=1,
      number=2, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='B', full_name='ai.verta.modeldb.versioning.HyperparameterConfigDiff.B', index=2,
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
  serialized_start=1599,
  serialized_end=1829,
)

_CONFIGBLOB.fields_by_name['hyperparameter_set'].message_type = _HYPERPARAMETERSETCONFIGBLOB
_CONFIGBLOB.fields_by_name['hyperparameters'].message_type = _HYPERPARAMETERCONFIGBLOB
_HYPERPARAMETERCONFIGBLOB.fields_by_name['value'].message_type = _HYPERPARAMETERVALUESCONFIGBLOB
_HYPERPARAMETERVALUESCONFIGBLOB.oneofs_by_name['value'].fields.append(
  _HYPERPARAMETERVALUESCONFIGBLOB.fields_by_name['int_value'])
_HYPERPARAMETERVALUESCONFIGBLOB.fields_by_name['int_value'].containing_oneof = _HYPERPARAMETERVALUESCONFIGBLOB.oneofs_by_name['value']
_HYPERPARAMETERVALUESCONFIGBLOB.oneofs_by_name['value'].fields.append(
  _HYPERPARAMETERVALUESCONFIGBLOB.fields_by_name['float_value'])
_HYPERPARAMETERVALUESCONFIGBLOB.fields_by_name['float_value'].containing_oneof = _HYPERPARAMETERVALUESCONFIGBLOB.oneofs_by_name['value']
_HYPERPARAMETERVALUESCONFIGBLOB.oneofs_by_name['value'].fields.append(
  _HYPERPARAMETERVALUESCONFIGBLOB.fields_by_name['string_value'])
_HYPERPARAMETERVALUESCONFIGBLOB.fields_by_name['string_value'].containing_oneof = _HYPERPARAMETERVALUESCONFIGBLOB.oneofs_by_name['value']
_HYPERPARAMETERSETCONFIGBLOB.fields_by_name['continuous'].message_type = _CONTINUOUSHYPERPARAMETERSETCONFIGBLOB
_HYPERPARAMETERSETCONFIGBLOB.fields_by_name['discrete'].message_type = _DISCRETEHYPERPARAMETERSETCONFIGBLOB
_HYPERPARAMETERSETCONFIGBLOB.oneofs_by_name['value'].fields.append(
  _HYPERPARAMETERSETCONFIGBLOB.fields_by_name['continuous'])
_HYPERPARAMETERSETCONFIGBLOB.fields_by_name['continuous'].containing_oneof = _HYPERPARAMETERSETCONFIGBLOB.oneofs_by_name['value']
_HYPERPARAMETERSETCONFIGBLOB.oneofs_by_name['value'].fields.append(
  _HYPERPARAMETERSETCONFIGBLOB.fields_by_name['discrete'])
_HYPERPARAMETERSETCONFIGBLOB.fields_by_name['discrete'].containing_oneof = _HYPERPARAMETERSETCONFIGBLOB.oneofs_by_name['value']
_CONTINUOUSHYPERPARAMETERSETCONFIGBLOB.fields_by_name['interval_begin'].message_type = _HYPERPARAMETERVALUESCONFIGBLOB
_CONTINUOUSHYPERPARAMETERSETCONFIGBLOB.fields_by_name['interval_end'].message_type = _HYPERPARAMETERVALUESCONFIGBLOB
_CONTINUOUSHYPERPARAMETERSETCONFIGBLOB.fields_by_name['interval_step'].message_type = _HYPERPARAMETERVALUESCONFIGBLOB
_DISCRETEHYPERPARAMETERSETCONFIGBLOB.fields_by_name['values'].message_type = _HYPERPARAMETERVALUESCONFIGBLOB
_CONFIGDIFF.fields_by_name['hyperparameter_set'].message_type = _HYPERPARAMETERSETCONFIGDIFF
_CONFIGDIFF.fields_by_name['hyperparameters'].message_type = _HYPERPARAMETERCONFIGDIFF
_HYPERPARAMETERSETCONFIGDIFF.fields_by_name['status'].enum_type = protos_dot_public_dot_modeldb_dot_versioning_dot_Enums__pb2._DIFFSTATUSENUM_DIFFSTATUS
_HYPERPARAMETERSETCONFIGDIFF.fields_by_name['A'].message_type = _HYPERPARAMETERSETCONFIGBLOB
_HYPERPARAMETERSETCONFIGDIFF.fields_by_name['B'].message_type = _HYPERPARAMETERSETCONFIGBLOB
_HYPERPARAMETERCONFIGDIFF.fields_by_name['status'].enum_type = protos_dot_public_dot_modeldb_dot_versioning_dot_Enums__pb2._DIFFSTATUSENUM_DIFFSTATUS
_HYPERPARAMETERCONFIGDIFF.fields_by_name['A'].message_type = _HYPERPARAMETERCONFIGBLOB
_HYPERPARAMETERCONFIGDIFF.fields_by_name['B'].message_type = _HYPERPARAMETERCONFIGBLOB
DESCRIPTOR.message_types_by_name['ConfigBlob'] = _CONFIGBLOB
DESCRIPTOR.message_types_by_name['HyperparameterConfigBlob'] = _HYPERPARAMETERCONFIGBLOB
DESCRIPTOR.message_types_by_name['HyperparameterValuesConfigBlob'] = _HYPERPARAMETERVALUESCONFIGBLOB
DESCRIPTOR.message_types_by_name['HyperparameterSetConfigBlob'] = _HYPERPARAMETERSETCONFIGBLOB
DESCRIPTOR.message_types_by_name['ContinuousHyperparameterSetConfigBlob'] = _CONTINUOUSHYPERPARAMETERSETCONFIGBLOB
DESCRIPTOR.message_types_by_name['DiscreteHyperparameterSetConfigBlob'] = _DISCRETEHYPERPARAMETERSETCONFIGBLOB
DESCRIPTOR.message_types_by_name['ConfigDiff'] = _CONFIGDIFF
DESCRIPTOR.message_types_by_name['HyperparameterSetConfigDiff'] = _HYPERPARAMETERSETCONFIGDIFF
DESCRIPTOR.message_types_by_name['HyperparameterConfigDiff'] = _HYPERPARAMETERCONFIGDIFF
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

ConfigBlob = _reflection.GeneratedProtocolMessageType('ConfigBlob', (_message.Message,), dict(
  DESCRIPTOR = _CONFIGBLOB,
  __module__ = 'protos.public.modeldb.versioning.Config_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.versioning.ConfigBlob)
  ))
_sym_db.RegisterMessage(ConfigBlob)

HyperparameterConfigBlob = _reflection.GeneratedProtocolMessageType('HyperparameterConfigBlob', (_message.Message,), dict(
  DESCRIPTOR = _HYPERPARAMETERCONFIGBLOB,
  __module__ = 'protos.public.modeldb.versioning.Config_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.versioning.HyperparameterConfigBlob)
  ))
_sym_db.RegisterMessage(HyperparameterConfigBlob)

HyperparameterValuesConfigBlob = _reflection.GeneratedProtocolMessageType('HyperparameterValuesConfigBlob', (_message.Message,), dict(
  DESCRIPTOR = _HYPERPARAMETERVALUESCONFIGBLOB,
  __module__ = 'protos.public.modeldb.versioning.Config_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.versioning.HyperparameterValuesConfigBlob)
  ))
_sym_db.RegisterMessage(HyperparameterValuesConfigBlob)

HyperparameterSetConfigBlob = _reflection.GeneratedProtocolMessageType('HyperparameterSetConfigBlob', (_message.Message,), dict(
  DESCRIPTOR = _HYPERPARAMETERSETCONFIGBLOB,
  __module__ = 'protos.public.modeldb.versioning.Config_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.versioning.HyperparameterSetConfigBlob)
  ))
_sym_db.RegisterMessage(HyperparameterSetConfigBlob)

ContinuousHyperparameterSetConfigBlob = _reflection.GeneratedProtocolMessageType('ContinuousHyperparameterSetConfigBlob', (_message.Message,), dict(
  DESCRIPTOR = _CONTINUOUSHYPERPARAMETERSETCONFIGBLOB,
  __module__ = 'protos.public.modeldb.versioning.Config_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.versioning.ContinuousHyperparameterSetConfigBlob)
  ))
_sym_db.RegisterMessage(ContinuousHyperparameterSetConfigBlob)

DiscreteHyperparameterSetConfigBlob = _reflection.GeneratedProtocolMessageType('DiscreteHyperparameterSetConfigBlob', (_message.Message,), dict(
  DESCRIPTOR = _DISCRETEHYPERPARAMETERSETCONFIGBLOB,
  __module__ = 'protos.public.modeldb.versioning.Config_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.versioning.DiscreteHyperparameterSetConfigBlob)
  ))
_sym_db.RegisterMessage(DiscreteHyperparameterSetConfigBlob)

ConfigDiff = _reflection.GeneratedProtocolMessageType('ConfigDiff', (_message.Message,), dict(
  DESCRIPTOR = _CONFIGDIFF,
  __module__ = 'protos.public.modeldb.versioning.Config_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.versioning.ConfigDiff)
  ))
_sym_db.RegisterMessage(ConfigDiff)

HyperparameterSetConfigDiff = _reflection.GeneratedProtocolMessageType('HyperparameterSetConfigDiff', (_message.Message,), dict(
  DESCRIPTOR = _HYPERPARAMETERSETCONFIGDIFF,
  __module__ = 'protos.public.modeldb.versioning.Config_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.versioning.HyperparameterSetConfigDiff)
  ))
_sym_db.RegisterMessage(HyperparameterSetConfigDiff)

HyperparameterConfigDiff = _reflection.GeneratedProtocolMessageType('HyperparameterConfigDiff', (_message.Message,), dict(
  DESCRIPTOR = _HYPERPARAMETERCONFIGDIFF,
  __module__ = 'protos.public.modeldb.versioning.Config_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.modeldb.versioning.HyperparameterConfigDiff)
  ))
_sym_db.RegisterMessage(HyperparameterConfigDiff)


DESCRIPTOR._options = None
# @@protoc_insertion_point(module_scope)
