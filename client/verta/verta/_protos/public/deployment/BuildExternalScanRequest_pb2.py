# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: deployment/BuildExternalScanRequest.proto

from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()




DESCRIPTOR = _descriptor.FileDescriptor(
  name='deployment/BuildExternalScanRequest.proto',
  package='ai.verta.deployment',
  syntax='proto3',
  serialized_options=b'ZAgithub.com/VertaAI/modeldb/protos/gen/go/protos/public/deployment',
  serialized_pb=b'\n)deployment/BuildExternalScanRequest.proto\x12\x13\x61i.verta.deployment\"=\n\x18\x42uildExternalScanRequest\x12\x0f\n\x07\x62uildId\x18\x01 \x01(\x03\x12\x10\n\x08imageUrl\x18\x02 \x01(\tBCZAgithub.com/VertaAI/modeldb/protos/gen/go/protos/public/deploymentb\x06proto3'
)




_BUILDEXTERNALSCANREQUEST = _descriptor.Descriptor(
  name='BuildExternalScanRequest',
  full_name='ai.verta.deployment.BuildExternalScanRequest',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='buildId', full_name='ai.verta.deployment.BuildExternalScanRequest.buildId', index=0,
      number=1, type=3, cpp_type=2, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='imageUrl', full_name='ai.verta.deployment.BuildExternalScanRequest.imageUrl', index=1,
      number=2, type=9, cpp_type=9, label=1,
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
  serialized_start=66,
  serialized_end=127,
)

DESCRIPTOR.message_types_by_name['BuildExternalScanRequest'] = _BUILDEXTERNALSCANREQUEST
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

BuildExternalScanRequest = _reflection.GeneratedProtocolMessageType('BuildExternalScanRequest', (_message.Message,), {
  'DESCRIPTOR' : _BUILDEXTERNALSCANREQUEST,
  '__module__' : 'deployment.BuildExternalScanRequest_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.deployment.BuildExternalScanRequest)
  })
_sym_db.RegisterMessage(BuildExternalScanRequest)


DESCRIPTOR._options = None
# @@protoc_insertion_point(module_scope)
