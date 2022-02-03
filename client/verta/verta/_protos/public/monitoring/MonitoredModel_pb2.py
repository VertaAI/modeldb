# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: monitoring/MonitoredModel.proto

from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from ..uac import Collaborator_pb2 as uac_dot_Collaborator__pb2
from google.api import annotations_pb2 as google_dot_api_dot_annotations__pb2
from google.protobuf import struct_pb2 as google_dot_protobuf_dot_struct__pb2
from ..monitoring import MonitoredEntity_pb2 as monitoring_dot_MonitoredEntity__pb2


DESCRIPTOR = _descriptor.FileDescriptor(
  name='monitoring/MonitoredModel.proto',
  package='ai.verta.monitoring',
  syntax='proto3',
  serialized_options=b'P\001ZAgithub.com/VertaAI/modeldb/protos/gen/go/protos/public/monitoring',
  serialized_pb=b'\n\x1fmonitoring/MonitoredModel.proto\x12\x13\x61i.verta.monitoring\x1a\x16uac/Collaborator.proto\x1a\x1cgoogle/api/annotations.proto\x1a\x1cgoogle/protobuf/struct.proto\x1a monitoring/MonitoredEntity.proto\"\xab\x01\n\x0eMonitoredModel\x12\n\n\x02id\x18\x01 \x01(\x04\x12\x0c\n\x04name\x18\x03 \x01(\t\x12\x0c\n\x04json\x18\x04 \x01(\t\x12\x0c\n\x04type\x18\x05 \x01(\t\x12\x0f\n\x07version\x18\x06 \x01(\t\x12\x17\n\x0f\x63reatedAtMillis\x18\x07 \x01(\x04\x12\x15\n\rworkspaceName\x18\x08 \x01(\t\x12\x1c\n\x14monitored_entity_ids\x18\t \x03(\x04J\x04\x08\x02\x10\x03\"\xcc\x01\n\x14\x43reateMonitoredModel\x12\x0c\n\x04name\x18\x02 \x01(\t\x12\x0c\n\x04json\x18\x03 \x01(\t\x12\x0c\n\x04type\x18\x04 \x01(\t\x12\x0f\n\x07version\x18\x05 \x01(\t\x12\x15\n\rworkspaceName\x18\x06 \x01(\t\x12\x1c\n\x14monitored_entity_ids\x18\x07 \x03(\x04\x1a>\n\x08Response\x12\x32\n\x05model\x18\x01 \x01(\x0b\x32#.ai.verta.monitoring.MonitoredModelJ\x04\x08\x01\x10\x02\"\x8a\x01\n\x14UpdateMonitoredModel\x12\x32\n\x05model\x18\x01 \x01(\x0b\x32#.ai.verta.monitoring.MonitoredModel\x1a>\n\x08Response\x12\x32\n\x05model\x18\x01 \x01(\x0b\x32#.ai.verta.monitoring.MonitoredModel\"\xab\x01\n\x13\x46indMonitoredModels\x12\x0b\n\x03ids\x18\x01 \x03(\x04\x12\x46\n\rfind_entities\x18\x02 \x01(\x0b\x32/.ai.verta.monitoring.FindMonitoredEntityRequest\x1a?\n\x08Response\x12\x33\n\x06models\x18\x01 \x03(\x0b\x32#.ai.verta.monitoring.MonitoredModel\"3\n\x14\x44\x65leteMonitoredModel\x12\x0f\n\x07modelId\x18\x01 \x01(\x04\x1a\n\n\x08Response2\xa3\x05\n\x15MonitoredModelService\x12\xa0\x01\n\x14\x63reateMonitoredModel\x12).ai.verta.monitoring.CreateMonitoredModel\x1a#.ai.verta.monitoring.MonitoredModel\"8\x82\xd3\xe4\x93\x02\x32\"-/api/v1/monitoring/model/createMonitoredModel:\x01*\x12\xa0\x01\n\x14updateMonitoredModel\x12).ai.verta.monitoring.UpdateMonitoredModel\x1a#.ai.verta.monitoring.MonitoredModel\"8\x82\xd3\xe4\x93\x02\x32\"-/api/v1/monitoring/model/updateMonitoredModel:\x01*\x12\xa9\x01\n\x12\x66indMonitoredModel\x12(.ai.verta.monitoring.FindMonitoredModels\x1a\x31.ai.verta.monitoring.FindMonitoredModels.Response\"6\x82\xd3\xe4\x93\x02\x30\"+/api/v1/monitoring/model/findMonitoredModel:\x01*\x12\x97\x01\n\x14\x64\x65leteMonitoredModel\x12).ai.verta.monitoring.DeleteMonitoredModel\x1a\x1a.ai.verta.monitoring.Empty\"8\x82\xd3\xe4\x93\x02\x32*-/api/v1/monitoring/model/deleteMonitoredModel:\x01*BEP\x01ZAgithub.com/VertaAI/modeldb/protos/gen/go/protos/public/monitoringb\x06proto3'
  ,
  dependencies=[uac_dot_Collaborator__pb2.DESCRIPTOR,google_dot_api_dot_annotations__pb2.DESCRIPTOR,google_dot_protobuf_dot_struct__pb2.DESCRIPTOR,monitoring_dot_MonitoredEntity__pb2.DESCRIPTOR,])




_MONITOREDMODEL = _descriptor.Descriptor(
  name='MonitoredModel',
  full_name='ai.verta.monitoring.MonitoredModel',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='id', full_name='ai.verta.monitoring.MonitoredModel.id', index=0,
      number=1, type=4, cpp_type=4, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='name', full_name='ai.verta.monitoring.MonitoredModel.name', index=1,
      number=3, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='json', full_name='ai.verta.monitoring.MonitoredModel.json', index=2,
      number=4, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='type', full_name='ai.verta.monitoring.MonitoredModel.type', index=3,
      number=5, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='version', full_name='ai.verta.monitoring.MonitoredModel.version', index=4,
      number=6, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='createdAtMillis', full_name='ai.verta.monitoring.MonitoredModel.createdAtMillis', index=5,
      number=7, type=4, cpp_type=4, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='workspaceName', full_name='ai.verta.monitoring.MonitoredModel.workspaceName', index=6,
      number=8, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='monitored_entity_ids', full_name='ai.verta.monitoring.MonitoredModel.monitored_entity_ids', index=7,
      number=9, type=4, cpp_type=4, label=3,
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
  serialized_start=175,
  serialized_end=346,
)


_CREATEMONITOREDMODEL_RESPONSE = _descriptor.Descriptor(
  name='Response',
  full_name='ai.verta.monitoring.CreateMonitoredModel.Response',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='model', full_name='ai.verta.monitoring.CreateMonitoredModel.Response.model', index=0,
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
  serialized_start=485,
  serialized_end=547,
)

_CREATEMONITOREDMODEL = _descriptor.Descriptor(
  name='CreateMonitoredModel',
  full_name='ai.verta.monitoring.CreateMonitoredModel',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='name', full_name='ai.verta.monitoring.CreateMonitoredModel.name', index=0,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='json', full_name='ai.verta.monitoring.CreateMonitoredModel.json', index=1,
      number=3, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='type', full_name='ai.verta.monitoring.CreateMonitoredModel.type', index=2,
      number=4, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='version', full_name='ai.verta.monitoring.CreateMonitoredModel.version', index=3,
      number=5, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='workspaceName', full_name='ai.verta.monitoring.CreateMonitoredModel.workspaceName', index=4,
      number=6, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='monitored_entity_ids', full_name='ai.verta.monitoring.CreateMonitoredModel.monitored_entity_ids', index=5,
      number=7, type=4, cpp_type=4, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_CREATEMONITOREDMODEL_RESPONSE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=349,
  serialized_end=553,
)


_UPDATEMONITOREDMODEL_RESPONSE = _descriptor.Descriptor(
  name='Response',
  full_name='ai.verta.monitoring.UpdateMonitoredModel.Response',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='model', full_name='ai.verta.monitoring.UpdateMonitoredModel.Response.model', index=0,
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
  serialized_start=485,
  serialized_end=547,
)

_UPDATEMONITOREDMODEL = _descriptor.Descriptor(
  name='UpdateMonitoredModel',
  full_name='ai.verta.monitoring.UpdateMonitoredModel',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='model', full_name='ai.verta.monitoring.UpdateMonitoredModel.model', index=0,
      number=1, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_UPDATEMONITOREDMODEL_RESPONSE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=556,
  serialized_end=694,
)


_FINDMONITOREDMODELS_RESPONSE = _descriptor.Descriptor(
  name='Response',
  full_name='ai.verta.monitoring.FindMonitoredModels.Response',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='models', full_name='ai.verta.monitoring.FindMonitoredModels.Response.models', index=0,
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
  serialized_start=805,
  serialized_end=868,
)

_FINDMONITOREDMODELS = _descriptor.Descriptor(
  name='FindMonitoredModels',
  full_name='ai.verta.monitoring.FindMonitoredModels',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='ids', full_name='ai.verta.monitoring.FindMonitoredModels.ids', index=0,
      number=1, type=4, cpp_type=4, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='find_entities', full_name='ai.verta.monitoring.FindMonitoredModels.find_entities', index=1,
      number=2, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_FINDMONITOREDMODELS_RESPONSE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=697,
  serialized_end=868,
)


_DELETEMONITOREDMODEL_RESPONSE = _descriptor.Descriptor(
  name='Response',
  full_name='ai.verta.monitoring.DeleteMonitoredModel.Response',
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
  serialized_start=485,
  serialized_end=495,
)

_DELETEMONITOREDMODEL = _descriptor.Descriptor(
  name='DeleteMonitoredModel',
  full_name='ai.verta.monitoring.DeleteMonitoredModel',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='modelId', full_name='ai.verta.monitoring.DeleteMonitoredModel.modelId', index=0,
      number=1, type=4, cpp_type=4, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_DELETEMONITOREDMODEL_RESPONSE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=870,
  serialized_end=921,
)

_CREATEMONITOREDMODEL_RESPONSE.fields_by_name['model'].message_type = _MONITOREDMODEL
_CREATEMONITOREDMODEL_RESPONSE.containing_type = _CREATEMONITOREDMODEL
_UPDATEMONITOREDMODEL_RESPONSE.fields_by_name['model'].message_type = _MONITOREDMODEL
_UPDATEMONITOREDMODEL_RESPONSE.containing_type = _UPDATEMONITOREDMODEL
_UPDATEMONITOREDMODEL.fields_by_name['model'].message_type = _MONITOREDMODEL
_FINDMONITOREDMODELS_RESPONSE.fields_by_name['models'].message_type = _MONITOREDMODEL
_FINDMONITOREDMODELS_RESPONSE.containing_type = _FINDMONITOREDMODELS
_FINDMONITOREDMODELS.fields_by_name['find_entities'].message_type = monitoring_dot_MonitoredEntity__pb2._FINDMONITOREDENTITYREQUEST
_DELETEMONITOREDMODEL_RESPONSE.containing_type = _DELETEMONITOREDMODEL
DESCRIPTOR.message_types_by_name['MonitoredModel'] = _MONITOREDMODEL
DESCRIPTOR.message_types_by_name['CreateMonitoredModel'] = _CREATEMONITOREDMODEL
DESCRIPTOR.message_types_by_name['UpdateMonitoredModel'] = _UPDATEMONITOREDMODEL
DESCRIPTOR.message_types_by_name['FindMonitoredModels'] = _FINDMONITOREDMODELS
DESCRIPTOR.message_types_by_name['DeleteMonitoredModel'] = _DELETEMONITOREDMODEL
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

MonitoredModel = _reflection.GeneratedProtocolMessageType('MonitoredModel', (_message.Message,), {
  'DESCRIPTOR' : _MONITOREDMODEL,
  '__module__' : 'monitoring.MonitoredModel_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.monitoring.MonitoredModel)
  })
_sym_db.RegisterMessage(MonitoredModel)

CreateMonitoredModel = _reflection.GeneratedProtocolMessageType('CreateMonitoredModel', (_message.Message,), {

  'Response' : _reflection.GeneratedProtocolMessageType('Response', (_message.Message,), {
    'DESCRIPTOR' : _CREATEMONITOREDMODEL_RESPONSE,
    '__module__' : 'monitoring.MonitoredModel_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.monitoring.CreateMonitoredModel.Response)
    })
  ,
  'DESCRIPTOR' : _CREATEMONITOREDMODEL,
  '__module__' : 'monitoring.MonitoredModel_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.monitoring.CreateMonitoredModel)
  })
_sym_db.RegisterMessage(CreateMonitoredModel)
_sym_db.RegisterMessage(CreateMonitoredModel.Response)

UpdateMonitoredModel = _reflection.GeneratedProtocolMessageType('UpdateMonitoredModel', (_message.Message,), {

  'Response' : _reflection.GeneratedProtocolMessageType('Response', (_message.Message,), {
    'DESCRIPTOR' : _UPDATEMONITOREDMODEL_RESPONSE,
    '__module__' : 'monitoring.MonitoredModel_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.monitoring.UpdateMonitoredModel.Response)
    })
  ,
  'DESCRIPTOR' : _UPDATEMONITOREDMODEL,
  '__module__' : 'monitoring.MonitoredModel_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.monitoring.UpdateMonitoredModel)
  })
_sym_db.RegisterMessage(UpdateMonitoredModel)
_sym_db.RegisterMessage(UpdateMonitoredModel.Response)

FindMonitoredModels = _reflection.GeneratedProtocolMessageType('FindMonitoredModels', (_message.Message,), {

  'Response' : _reflection.GeneratedProtocolMessageType('Response', (_message.Message,), {
    'DESCRIPTOR' : _FINDMONITOREDMODELS_RESPONSE,
    '__module__' : 'monitoring.MonitoredModel_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.monitoring.FindMonitoredModels.Response)
    })
  ,
  'DESCRIPTOR' : _FINDMONITOREDMODELS,
  '__module__' : 'monitoring.MonitoredModel_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.monitoring.FindMonitoredModels)
  })
_sym_db.RegisterMessage(FindMonitoredModels)
_sym_db.RegisterMessage(FindMonitoredModels.Response)

DeleteMonitoredModel = _reflection.GeneratedProtocolMessageType('DeleteMonitoredModel', (_message.Message,), {

  'Response' : _reflection.GeneratedProtocolMessageType('Response', (_message.Message,), {
    'DESCRIPTOR' : _DELETEMONITOREDMODEL_RESPONSE,
    '__module__' : 'monitoring.MonitoredModel_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.monitoring.DeleteMonitoredModel.Response)
    })
  ,
  'DESCRIPTOR' : _DELETEMONITOREDMODEL,
  '__module__' : 'monitoring.MonitoredModel_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.monitoring.DeleteMonitoredModel)
  })
_sym_db.RegisterMessage(DeleteMonitoredModel)
_sym_db.RegisterMessage(DeleteMonitoredModel.Response)


DESCRIPTOR._options = None

_MONITOREDMODELSERVICE = _descriptor.ServiceDescriptor(
  name='MonitoredModelService',
  full_name='ai.verta.monitoring.MonitoredModelService',
  file=DESCRIPTOR,
  index=0,
  serialized_options=None,
  serialized_start=924,
  serialized_end=1599,
  methods=[
  _descriptor.MethodDescriptor(
    name='createMonitoredModel',
    full_name='ai.verta.monitoring.MonitoredModelService.createMonitoredModel',
    index=0,
    containing_service=None,
    input_type=_CREATEMONITOREDMODEL,
    output_type=_MONITOREDMODEL,
    serialized_options=b'\202\323\344\223\0022\"-/api/v1/monitoring/model/createMonitoredModel:\001*',
  ),
  _descriptor.MethodDescriptor(
    name='updateMonitoredModel',
    full_name='ai.verta.monitoring.MonitoredModelService.updateMonitoredModel',
    index=1,
    containing_service=None,
    input_type=_UPDATEMONITOREDMODEL,
    output_type=_MONITOREDMODEL,
    serialized_options=b'\202\323\344\223\0022\"-/api/v1/monitoring/model/updateMonitoredModel:\001*',
  ),
  _descriptor.MethodDescriptor(
    name='findMonitoredModel',
    full_name='ai.verta.monitoring.MonitoredModelService.findMonitoredModel',
    index=2,
    containing_service=None,
    input_type=_FINDMONITOREDMODELS,
    output_type=_FINDMONITOREDMODELS_RESPONSE,
    serialized_options=b'\202\323\344\223\0020\"+/api/v1/monitoring/model/findMonitoredModel:\001*',
  ),
  _descriptor.MethodDescriptor(
    name='deleteMonitoredModel',
    full_name='ai.verta.monitoring.MonitoredModelService.deleteMonitoredModel',
    index=3,
    containing_service=None,
    input_type=_DELETEMONITOREDMODEL,
    output_type=monitoring_dot_MonitoredEntity__pb2._EMPTY,
    serialized_options=b'\202\323\344\223\0022*-/api/v1/monitoring/model/deleteMonitoredModel:\001*',
  ),
])
_sym_db.RegisterServiceDescriptor(_MONITOREDMODELSERVICE)

DESCRIPTOR.services_by_name['MonitoredModelService'] = _MONITOREDMODELSERVICE

# @@protoc_insertion_point(module_scope)