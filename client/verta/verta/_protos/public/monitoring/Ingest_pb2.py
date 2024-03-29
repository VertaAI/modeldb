# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: monitoring/Ingest.proto

from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from google.api import annotations_pb2 as google_dot_api_dot_annotations__pb2
from ..modeldb.versioning import Dataset_pb2 as modeldb_dot_versioning_dot_Dataset__pb2


DESCRIPTOR = _descriptor.FileDescriptor(
  name='monitoring/Ingest.proto',
  package='ai.verta.monitoring',
  syntax='proto3',
  serialized_options=b'P\001ZAgithub.com/VertaAI/modeldb/protos/gen/go/protos/public/monitoring',
  serialized_pb=b'\n\x17monitoring/Ingest.proto\x12\x13\x61i.verta.monitoring\x1a\x1cgoogle/api/annotations.proto\x1a modeldb/versioning/Dataset.proto\"\\\n\x0eSourceTypeEnum\"J\n\nSourceType\x12\x0b\n\x07UNKNOWN\x10\x00\x12\r\n\tREFERENCE\x10\x01\x12\x10\n\x0cGROUND_TRUTH\x10\x02\x12\x0e\n\nPREDICTION\x10\x03\"\xe9\x01\n\nColumnSpec\x12\x17\n\x0fmonitoring_name\x18\x01 \x01(\t\x12\x42\n\x07io_type\x18\x02 \x01(\x0e\x32\x31.ai.verta.monitoring.ColumnSpec.IOTypeEnum.IOType\x12\x16\n\x0e\x63onfidence_for\x18\x03 \x01(\t\x12\x18\n\x10ground_truth_for\x18\x04 \x01(\t\x1aL\n\nIOTypeEnum\">\n\x06IOType\x12\x0b\n\x07UNKNOWN\x10\x00\x12\t\n\x05INPUT\x10\x01\x12\n\n\x06OUTPUT\x10\x02\x12\x10\n\x0cGROUND_TRUTH\x10\x03\"~\n\nIngestData\x12\x0c\n\x04name\x18\x01 \x01(\t\x12\x16\n\x0cstring_value\x18\x02 \x01(\tH\x00\x12\x14\n\nlong_value\x18\x03 \x01(\x04H\x00\x12\x15\n\x0b\x66loat_value\x18\x04 \x01(\x02H\x00\x12\x14\n\nbool_value\x18\x05 \x01(\x08H\x00\x42\x07\n\x05value\"\xc1\x03\n\rIngestRequest\x12\x43\n\x0bsource_type\x18\x01 \x01(\x0e\x32..ai.verta.monitoring.SourceTypeEnum.SourceType\x12\x15\n\x0b\x65ndpoint_id\x18\x02 \x01(\x04H\x00\x12\x1d\n\x13monitored_entity_id\x18\x03 \x01(\x04H\x00\x12\x16\n\x0eid_column_name\x18\x04 \x01(\t\x12\x11\n\ttimestamp\x18\x05 \x01(\x04\x12M\n\x0eingest_columns\x18\x06 \x03(\x0b\x32\x35.ai.verta.monitoring.IngestRequest.IngestColumnsEntry\x12\x34\n\x0bingest_data\x18\x07 \x03(\x0b\x32\x1f.ai.verta.monitoring.IngestData\x12\x18\n\x10model_version_id\x18\x08 \x01(\x04\x1aU\n\x12IngestColumnsEntry\x12\x0b\n\x03key\x18\x01 \x01(\t\x12.\n\x05value\x18\x02 \x01(\x0b\x32\x1f.ai.verta.monitoring.ColumnSpec:\x02\x38\x01\x1a\n\n\x08ResponseB\x08\n\x06origin\"\x8b\x07\n\x12\x42\x61tchIngestRequest\x12\x43\n\x0bsource_type\x18\x01 \x01(\x0e\x32..ai.verta.monitoring.SourceTypeEnum.SourceType\x12\x16\n\x0eid_column_name\x18\x02 \x01(\t\x12\x15\n\x0b\x63olumn_name\x18\x03 \x01(\tH\x00\x12\x16\n\x0crfc3339_nano\x18\x04 \x01(\tH\x00\x12\x17\n\runix_utc_nano\x18\x05 \x01(\x04H\x00\x12\x1c\n\x12\x64\x61taset_version_id\x18\x06 \x01(\tH\x01\x12:\n\x06\x63onfig\x18\x07 \x01(\x0b\x32(.ai.verta.modeldb.versioning.DatasetBlobH\x01\x12R\n\x0eingest_columns\x18\x08 \x03(\x0b\x32:.ai.verta.monitoring.BatchIngestRequest.IngestColumnsEntry\x12\x10\n\x08\x62\x61tch_id\x18\t \x01(\t\x12\\\n\rencoding_type\x18\n \x01(\x0e\x32\x45.ai.verta.monitoring.BatchIngestRequest.EncodingTypeEnum.EncodingType\x12\x65\n\x10\x63ompression_type\x18\x0b \x01(\x0e\x32K.ai.verta.monitoring.BatchIngestRequest.CompressionTypeEnum.CompressionType\x12\x1b\n\x13monitored_entity_id\x18\x0c \x01(\x04\x12\x18\n\x10model_version_id\x18\r \x01(\x04\x1aU\n\x12IngestColumnsEntry\x12\x0b\n\x03key\x18\x01 \x01(\t\x12.\n\x05value\x18\x02 \x01(\x0b\x32\x1f.ai.verta.monitoring.ColumnSpec:\x02\x38\x01\x1aO\n\x10\x45ncodingTypeEnum\";\n\x0c\x45ncodingType\x12\x0b\n\x07UNKNOWN\x10\x00\x12\x07\n\x03\x43SV\x10\x01\x12\x08\n\x04JSON\x10\x02\x12\x0b\n\x07PARQUET\x10\x03\x1aH\n\x13\x43ompressionTypeEnum\"1\n\x0f\x43ompressionType\x12\x0b\n\x07UNKNOWN\x10\x00\x12\x07\n\x03RAW\x10\x01\x12\x08\n\x04GZIP\x10\x02\x1a\n\n\x08ResponseB\x0b\n\ttimestampB\t\n\x07\x64\x61taset2\xad\x02\n\rIngestService\x12\x7f\n\x06ingest\x12\".ai.verta.monitoring.IngestRequest\x1a+.ai.verta.monitoring.IngestRequest.Response\"$\x82\xd3\xe4\x93\x02\x1e\"\x19/api/v1/monitoring/ingest:\x01*\x12\x9a\x01\n\x0b\x62\x61tchIngest\x12\'.ai.verta.monitoring.BatchIngestRequest\x1a\x30.ai.verta.monitoring.BatchIngestRequest.Response\"0\x82\xd3\xe4\x93\x02*\"%/api/v1/monitoring/ingest/batchIngest:\x01*BEP\x01ZAgithub.com/VertaAI/modeldb/protos/gen/go/protos/public/monitoringb\x06proto3'
  ,
  dependencies=[google_dot_api_dot_annotations__pb2.DESCRIPTOR,modeldb_dot_versioning_dot_Dataset__pb2.DESCRIPTOR,])



_SOURCETYPEENUM_SOURCETYPE = _descriptor.EnumDescriptor(
  name='SourceType',
  full_name='ai.verta.monitoring.SourceTypeEnum.SourceType',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='UNKNOWN', index=0, number=0,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='REFERENCE', index=1, number=1,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='GROUND_TRUTH', index=2, number=2,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='PREDICTION', index=3, number=3,
      serialized_options=None,
      type=None),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=130,
  serialized_end=204,
)
_sym_db.RegisterEnumDescriptor(_SOURCETYPEENUM_SOURCETYPE)

_COLUMNSPEC_IOTYPEENUM_IOTYPE = _descriptor.EnumDescriptor(
  name='IOType',
  full_name='ai.verta.monitoring.ColumnSpec.IOTypeEnum.IOType',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='UNKNOWN', index=0, number=0,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='INPUT', index=1, number=1,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='OUTPUT', index=2, number=2,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='GROUND_TRUTH', index=3, number=3,
      serialized_options=None,
      type=None),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=378,
  serialized_end=440,
)
_sym_db.RegisterEnumDescriptor(_COLUMNSPEC_IOTYPEENUM_IOTYPE)

_BATCHINGESTREQUEST_ENCODINGTYPEENUM_ENCODINGTYPE = _descriptor.EnumDescriptor(
  name='EncodingType',
  full_name='ai.verta.monitoring.BatchIngestRequest.EncodingTypeEnum.EncodingType',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='UNKNOWN', index=0, number=0,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='CSV', index=1, number=1,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='JSON', index=2, number=2,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='PARQUET', index=3, number=3,
      serialized_options=None,
      type=None),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=1761,
  serialized_end=1820,
)
_sym_db.RegisterEnumDescriptor(_BATCHINGESTREQUEST_ENCODINGTYPEENUM_ENCODINGTYPE)

_BATCHINGESTREQUEST_COMPRESSIONTYPEENUM_COMPRESSIONTYPE = _descriptor.EnumDescriptor(
  name='CompressionType',
  full_name='ai.verta.monitoring.BatchIngestRequest.CompressionTypeEnum.CompressionType',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='UNKNOWN', index=0, number=0,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='RAW', index=1, number=1,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='GZIP', index=2, number=2,
      serialized_options=None,
      type=None),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=1845,
  serialized_end=1894,
)
_sym_db.RegisterEnumDescriptor(_BATCHINGESTREQUEST_COMPRESSIONTYPEENUM_COMPRESSIONTYPE)


_SOURCETYPEENUM = _descriptor.Descriptor(
  name='SourceTypeEnum',
  full_name='ai.verta.monitoring.SourceTypeEnum',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
    _SOURCETYPEENUM_SOURCETYPE,
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=112,
  serialized_end=204,
)


_COLUMNSPEC_IOTYPEENUM = _descriptor.Descriptor(
  name='IOTypeEnum',
  full_name='ai.verta.monitoring.ColumnSpec.IOTypeEnum',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
    _COLUMNSPEC_IOTYPEENUM_IOTYPE,
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=364,
  serialized_end=440,
)

_COLUMNSPEC = _descriptor.Descriptor(
  name='ColumnSpec',
  full_name='ai.verta.monitoring.ColumnSpec',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='monitoring_name', full_name='ai.verta.monitoring.ColumnSpec.monitoring_name', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='io_type', full_name='ai.verta.monitoring.ColumnSpec.io_type', index=1,
      number=2, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='confidence_for', full_name='ai.verta.monitoring.ColumnSpec.confidence_for', index=2,
      number=3, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='ground_truth_for', full_name='ai.verta.monitoring.ColumnSpec.ground_truth_for', index=3,
      number=4, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_COLUMNSPEC_IOTYPEENUM, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=207,
  serialized_end=440,
)


_INGESTDATA = _descriptor.Descriptor(
  name='IngestData',
  full_name='ai.verta.monitoring.IngestData',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='name', full_name='ai.verta.monitoring.IngestData.name', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='string_value', full_name='ai.verta.monitoring.IngestData.string_value', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='long_value', full_name='ai.verta.monitoring.IngestData.long_value', index=2,
      number=3, type=4, cpp_type=4, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='float_value', full_name='ai.verta.monitoring.IngestData.float_value', index=3,
      number=4, type=2, cpp_type=6, label=1,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='bool_value', full_name='ai.verta.monitoring.IngestData.bool_value', index=4,
      number=5, type=8, cpp_type=7, label=1,
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
    _descriptor.OneofDescriptor(
      name='value', full_name='ai.verta.monitoring.IngestData.value',
      index=0, containing_type=None, fields=[]),
  ],
  serialized_start=442,
  serialized_end=568,
)


_INGESTREQUEST_INGESTCOLUMNSENTRY = _descriptor.Descriptor(
  name='IngestColumnsEntry',
  full_name='ai.verta.monitoring.IngestRequest.IngestColumnsEntry',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='key', full_name='ai.verta.monitoring.IngestRequest.IngestColumnsEntry.key', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='value', full_name='ai.verta.monitoring.IngestRequest.IngestColumnsEntry.value', index=1,
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
  serialized_options=b'8\001',
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=913,
  serialized_end=998,
)

_INGESTREQUEST_RESPONSE = _descriptor.Descriptor(
  name='Response',
  full_name='ai.verta.monitoring.IngestRequest.Response',
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
  serialized_start=1000,
  serialized_end=1010,
)

_INGESTREQUEST = _descriptor.Descriptor(
  name='IngestRequest',
  full_name='ai.verta.monitoring.IngestRequest',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='source_type', full_name='ai.verta.monitoring.IngestRequest.source_type', index=0,
      number=1, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='endpoint_id', full_name='ai.verta.monitoring.IngestRequest.endpoint_id', index=1,
      number=2, type=4, cpp_type=4, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='monitored_entity_id', full_name='ai.verta.monitoring.IngestRequest.monitored_entity_id', index=2,
      number=3, type=4, cpp_type=4, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='id_column_name', full_name='ai.verta.monitoring.IngestRequest.id_column_name', index=3,
      number=4, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='timestamp', full_name='ai.verta.monitoring.IngestRequest.timestamp', index=4,
      number=5, type=4, cpp_type=4, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='ingest_columns', full_name='ai.verta.monitoring.IngestRequest.ingest_columns', index=5,
      number=6, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='ingest_data', full_name='ai.verta.monitoring.IngestRequest.ingest_data', index=6,
      number=7, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='model_version_id', full_name='ai.verta.monitoring.IngestRequest.model_version_id', index=7,
      number=8, type=4, cpp_type=4, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_INGESTREQUEST_INGESTCOLUMNSENTRY, _INGESTREQUEST_RESPONSE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
    _descriptor.OneofDescriptor(
      name='origin', full_name='ai.verta.monitoring.IngestRequest.origin',
      index=0, containing_type=None, fields=[]),
  ],
  serialized_start=571,
  serialized_end=1020,
)


_BATCHINGESTREQUEST_INGESTCOLUMNSENTRY = _descriptor.Descriptor(
  name='IngestColumnsEntry',
  full_name='ai.verta.monitoring.BatchIngestRequest.IngestColumnsEntry',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='key', full_name='ai.verta.monitoring.BatchIngestRequest.IngestColumnsEntry.key', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='value', full_name='ai.verta.monitoring.BatchIngestRequest.IngestColumnsEntry.value', index=1,
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
  serialized_options=b'8\001',
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=913,
  serialized_end=998,
)

_BATCHINGESTREQUEST_ENCODINGTYPEENUM = _descriptor.Descriptor(
  name='EncodingTypeEnum',
  full_name='ai.verta.monitoring.BatchIngestRequest.EncodingTypeEnum',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
    _BATCHINGESTREQUEST_ENCODINGTYPEENUM_ENCODINGTYPE,
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=1741,
  serialized_end=1820,
)

_BATCHINGESTREQUEST_COMPRESSIONTYPEENUM = _descriptor.Descriptor(
  name='CompressionTypeEnum',
  full_name='ai.verta.monitoring.BatchIngestRequest.CompressionTypeEnum',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
    _BATCHINGESTREQUEST_COMPRESSIONTYPEENUM_COMPRESSIONTYPE,
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=1822,
  serialized_end=1894,
)

_BATCHINGESTREQUEST_RESPONSE = _descriptor.Descriptor(
  name='Response',
  full_name='ai.verta.monitoring.BatchIngestRequest.Response',
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
  serialized_start=1000,
  serialized_end=1010,
)

_BATCHINGESTREQUEST = _descriptor.Descriptor(
  name='BatchIngestRequest',
  full_name='ai.verta.monitoring.BatchIngestRequest',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='source_type', full_name='ai.verta.monitoring.BatchIngestRequest.source_type', index=0,
      number=1, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='id_column_name', full_name='ai.verta.monitoring.BatchIngestRequest.id_column_name', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='column_name', full_name='ai.verta.monitoring.BatchIngestRequest.column_name', index=2,
      number=3, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='rfc3339_nano', full_name='ai.verta.monitoring.BatchIngestRequest.rfc3339_nano', index=3,
      number=4, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='unix_utc_nano', full_name='ai.verta.monitoring.BatchIngestRequest.unix_utc_nano', index=4,
      number=5, type=4, cpp_type=4, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='dataset_version_id', full_name='ai.verta.monitoring.BatchIngestRequest.dataset_version_id', index=5,
      number=6, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='config', full_name='ai.verta.monitoring.BatchIngestRequest.config', index=6,
      number=7, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='ingest_columns', full_name='ai.verta.monitoring.BatchIngestRequest.ingest_columns', index=7,
      number=8, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='batch_id', full_name='ai.verta.monitoring.BatchIngestRequest.batch_id', index=8,
      number=9, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='encoding_type', full_name='ai.verta.monitoring.BatchIngestRequest.encoding_type', index=9,
      number=10, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='compression_type', full_name='ai.verta.monitoring.BatchIngestRequest.compression_type', index=10,
      number=11, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='monitored_entity_id', full_name='ai.verta.monitoring.BatchIngestRequest.monitored_entity_id', index=11,
      number=12, type=4, cpp_type=4, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='model_version_id', full_name='ai.verta.monitoring.BatchIngestRequest.model_version_id', index=12,
      number=13, type=4, cpp_type=4, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_BATCHINGESTREQUEST_INGESTCOLUMNSENTRY, _BATCHINGESTREQUEST_ENCODINGTYPEENUM, _BATCHINGESTREQUEST_COMPRESSIONTYPEENUM, _BATCHINGESTREQUEST_RESPONSE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
    _descriptor.OneofDescriptor(
      name='timestamp', full_name='ai.verta.monitoring.BatchIngestRequest.timestamp',
      index=0, containing_type=None, fields=[]),
    _descriptor.OneofDescriptor(
      name='dataset', full_name='ai.verta.monitoring.BatchIngestRequest.dataset',
      index=1, containing_type=None, fields=[]),
  ],
  serialized_start=1023,
  serialized_end=1930,
)

_SOURCETYPEENUM_SOURCETYPE.containing_type = _SOURCETYPEENUM
_COLUMNSPEC_IOTYPEENUM.containing_type = _COLUMNSPEC
_COLUMNSPEC_IOTYPEENUM_IOTYPE.containing_type = _COLUMNSPEC_IOTYPEENUM
_COLUMNSPEC.fields_by_name['io_type'].enum_type = _COLUMNSPEC_IOTYPEENUM_IOTYPE
_INGESTDATA.oneofs_by_name['value'].fields.append(
  _INGESTDATA.fields_by_name['string_value'])
_INGESTDATA.fields_by_name['string_value'].containing_oneof = _INGESTDATA.oneofs_by_name['value']
_INGESTDATA.oneofs_by_name['value'].fields.append(
  _INGESTDATA.fields_by_name['long_value'])
_INGESTDATA.fields_by_name['long_value'].containing_oneof = _INGESTDATA.oneofs_by_name['value']
_INGESTDATA.oneofs_by_name['value'].fields.append(
  _INGESTDATA.fields_by_name['float_value'])
_INGESTDATA.fields_by_name['float_value'].containing_oneof = _INGESTDATA.oneofs_by_name['value']
_INGESTDATA.oneofs_by_name['value'].fields.append(
  _INGESTDATA.fields_by_name['bool_value'])
_INGESTDATA.fields_by_name['bool_value'].containing_oneof = _INGESTDATA.oneofs_by_name['value']
_INGESTREQUEST_INGESTCOLUMNSENTRY.fields_by_name['value'].message_type = _COLUMNSPEC
_INGESTREQUEST_INGESTCOLUMNSENTRY.containing_type = _INGESTREQUEST
_INGESTREQUEST_RESPONSE.containing_type = _INGESTREQUEST
_INGESTREQUEST.fields_by_name['source_type'].enum_type = _SOURCETYPEENUM_SOURCETYPE
_INGESTREQUEST.fields_by_name['ingest_columns'].message_type = _INGESTREQUEST_INGESTCOLUMNSENTRY
_INGESTREQUEST.fields_by_name['ingest_data'].message_type = _INGESTDATA
_INGESTREQUEST.oneofs_by_name['origin'].fields.append(
  _INGESTREQUEST.fields_by_name['endpoint_id'])
_INGESTREQUEST.fields_by_name['endpoint_id'].containing_oneof = _INGESTREQUEST.oneofs_by_name['origin']
_INGESTREQUEST.oneofs_by_name['origin'].fields.append(
  _INGESTREQUEST.fields_by_name['monitored_entity_id'])
_INGESTREQUEST.fields_by_name['monitored_entity_id'].containing_oneof = _INGESTREQUEST.oneofs_by_name['origin']
_BATCHINGESTREQUEST_INGESTCOLUMNSENTRY.fields_by_name['value'].message_type = _COLUMNSPEC
_BATCHINGESTREQUEST_INGESTCOLUMNSENTRY.containing_type = _BATCHINGESTREQUEST
_BATCHINGESTREQUEST_ENCODINGTYPEENUM.containing_type = _BATCHINGESTREQUEST
_BATCHINGESTREQUEST_ENCODINGTYPEENUM_ENCODINGTYPE.containing_type = _BATCHINGESTREQUEST_ENCODINGTYPEENUM
_BATCHINGESTREQUEST_COMPRESSIONTYPEENUM.containing_type = _BATCHINGESTREQUEST
_BATCHINGESTREQUEST_COMPRESSIONTYPEENUM_COMPRESSIONTYPE.containing_type = _BATCHINGESTREQUEST_COMPRESSIONTYPEENUM
_BATCHINGESTREQUEST_RESPONSE.containing_type = _BATCHINGESTREQUEST
_BATCHINGESTREQUEST.fields_by_name['source_type'].enum_type = _SOURCETYPEENUM_SOURCETYPE
_BATCHINGESTREQUEST.fields_by_name['config'].message_type = modeldb_dot_versioning_dot_Dataset__pb2._DATASETBLOB
_BATCHINGESTREQUEST.fields_by_name['ingest_columns'].message_type = _BATCHINGESTREQUEST_INGESTCOLUMNSENTRY
_BATCHINGESTREQUEST.fields_by_name['encoding_type'].enum_type = _BATCHINGESTREQUEST_ENCODINGTYPEENUM_ENCODINGTYPE
_BATCHINGESTREQUEST.fields_by_name['compression_type'].enum_type = _BATCHINGESTREQUEST_COMPRESSIONTYPEENUM_COMPRESSIONTYPE
_BATCHINGESTREQUEST.oneofs_by_name['timestamp'].fields.append(
  _BATCHINGESTREQUEST.fields_by_name['column_name'])
_BATCHINGESTREQUEST.fields_by_name['column_name'].containing_oneof = _BATCHINGESTREQUEST.oneofs_by_name['timestamp']
_BATCHINGESTREQUEST.oneofs_by_name['timestamp'].fields.append(
  _BATCHINGESTREQUEST.fields_by_name['rfc3339_nano'])
_BATCHINGESTREQUEST.fields_by_name['rfc3339_nano'].containing_oneof = _BATCHINGESTREQUEST.oneofs_by_name['timestamp']
_BATCHINGESTREQUEST.oneofs_by_name['timestamp'].fields.append(
  _BATCHINGESTREQUEST.fields_by_name['unix_utc_nano'])
_BATCHINGESTREQUEST.fields_by_name['unix_utc_nano'].containing_oneof = _BATCHINGESTREQUEST.oneofs_by_name['timestamp']
_BATCHINGESTREQUEST.oneofs_by_name['dataset'].fields.append(
  _BATCHINGESTREQUEST.fields_by_name['dataset_version_id'])
_BATCHINGESTREQUEST.fields_by_name['dataset_version_id'].containing_oneof = _BATCHINGESTREQUEST.oneofs_by_name['dataset']
_BATCHINGESTREQUEST.oneofs_by_name['dataset'].fields.append(
  _BATCHINGESTREQUEST.fields_by_name['config'])
_BATCHINGESTREQUEST.fields_by_name['config'].containing_oneof = _BATCHINGESTREQUEST.oneofs_by_name['dataset']
DESCRIPTOR.message_types_by_name['SourceTypeEnum'] = _SOURCETYPEENUM
DESCRIPTOR.message_types_by_name['ColumnSpec'] = _COLUMNSPEC
DESCRIPTOR.message_types_by_name['IngestData'] = _INGESTDATA
DESCRIPTOR.message_types_by_name['IngestRequest'] = _INGESTREQUEST
DESCRIPTOR.message_types_by_name['BatchIngestRequest'] = _BATCHINGESTREQUEST
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

SourceTypeEnum = _reflection.GeneratedProtocolMessageType('SourceTypeEnum', (_message.Message,), {
  'DESCRIPTOR' : _SOURCETYPEENUM,
  '__module__' : 'monitoring.Ingest_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.monitoring.SourceTypeEnum)
  })
_sym_db.RegisterMessage(SourceTypeEnum)

ColumnSpec = _reflection.GeneratedProtocolMessageType('ColumnSpec', (_message.Message,), {

  'IOTypeEnum' : _reflection.GeneratedProtocolMessageType('IOTypeEnum', (_message.Message,), {
    'DESCRIPTOR' : _COLUMNSPEC_IOTYPEENUM,
    '__module__' : 'monitoring.Ingest_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.monitoring.ColumnSpec.IOTypeEnum)
    })
  ,
  'DESCRIPTOR' : _COLUMNSPEC,
  '__module__' : 'monitoring.Ingest_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.monitoring.ColumnSpec)
  })
_sym_db.RegisterMessage(ColumnSpec)
_sym_db.RegisterMessage(ColumnSpec.IOTypeEnum)

IngestData = _reflection.GeneratedProtocolMessageType('IngestData', (_message.Message,), {
  'DESCRIPTOR' : _INGESTDATA,
  '__module__' : 'monitoring.Ingest_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.monitoring.IngestData)
  })
_sym_db.RegisterMessage(IngestData)

IngestRequest = _reflection.GeneratedProtocolMessageType('IngestRequest', (_message.Message,), {

  'IngestColumnsEntry' : _reflection.GeneratedProtocolMessageType('IngestColumnsEntry', (_message.Message,), {
    'DESCRIPTOR' : _INGESTREQUEST_INGESTCOLUMNSENTRY,
    '__module__' : 'monitoring.Ingest_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.monitoring.IngestRequest.IngestColumnsEntry)
    })
  ,

  'Response' : _reflection.GeneratedProtocolMessageType('Response', (_message.Message,), {
    'DESCRIPTOR' : _INGESTREQUEST_RESPONSE,
    '__module__' : 'monitoring.Ingest_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.monitoring.IngestRequest.Response)
    })
  ,
  'DESCRIPTOR' : _INGESTREQUEST,
  '__module__' : 'monitoring.Ingest_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.monitoring.IngestRequest)
  })
_sym_db.RegisterMessage(IngestRequest)
_sym_db.RegisterMessage(IngestRequest.IngestColumnsEntry)
_sym_db.RegisterMessage(IngestRequest.Response)

BatchIngestRequest = _reflection.GeneratedProtocolMessageType('BatchIngestRequest', (_message.Message,), {

  'IngestColumnsEntry' : _reflection.GeneratedProtocolMessageType('IngestColumnsEntry', (_message.Message,), {
    'DESCRIPTOR' : _BATCHINGESTREQUEST_INGESTCOLUMNSENTRY,
    '__module__' : 'monitoring.Ingest_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.monitoring.BatchIngestRequest.IngestColumnsEntry)
    })
  ,

  'EncodingTypeEnum' : _reflection.GeneratedProtocolMessageType('EncodingTypeEnum', (_message.Message,), {
    'DESCRIPTOR' : _BATCHINGESTREQUEST_ENCODINGTYPEENUM,
    '__module__' : 'monitoring.Ingest_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.monitoring.BatchIngestRequest.EncodingTypeEnum)
    })
  ,

  'CompressionTypeEnum' : _reflection.GeneratedProtocolMessageType('CompressionTypeEnum', (_message.Message,), {
    'DESCRIPTOR' : _BATCHINGESTREQUEST_COMPRESSIONTYPEENUM,
    '__module__' : 'monitoring.Ingest_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.monitoring.BatchIngestRequest.CompressionTypeEnum)
    })
  ,

  'Response' : _reflection.GeneratedProtocolMessageType('Response', (_message.Message,), {
    'DESCRIPTOR' : _BATCHINGESTREQUEST_RESPONSE,
    '__module__' : 'monitoring.Ingest_pb2'
    # @@protoc_insertion_point(class_scope:ai.verta.monitoring.BatchIngestRequest.Response)
    })
  ,
  'DESCRIPTOR' : _BATCHINGESTREQUEST,
  '__module__' : 'monitoring.Ingest_pb2'
  # @@protoc_insertion_point(class_scope:ai.verta.monitoring.BatchIngestRequest)
  })
_sym_db.RegisterMessage(BatchIngestRequest)
_sym_db.RegisterMessage(BatchIngestRequest.IngestColumnsEntry)
_sym_db.RegisterMessage(BatchIngestRequest.EncodingTypeEnum)
_sym_db.RegisterMessage(BatchIngestRequest.CompressionTypeEnum)
_sym_db.RegisterMessage(BatchIngestRequest.Response)


DESCRIPTOR._options = None
_INGESTREQUEST_INGESTCOLUMNSENTRY._options = None
_BATCHINGESTREQUEST_INGESTCOLUMNSENTRY._options = None

_INGESTSERVICE = _descriptor.ServiceDescriptor(
  name='IngestService',
  full_name='ai.verta.monitoring.IngestService',
  file=DESCRIPTOR,
  index=0,
  serialized_options=None,
  serialized_start=1933,
  serialized_end=2234,
  methods=[
  _descriptor.MethodDescriptor(
    name='ingest',
    full_name='ai.verta.monitoring.IngestService.ingest',
    index=0,
    containing_service=None,
    input_type=_INGESTREQUEST,
    output_type=_INGESTREQUEST_RESPONSE,
    serialized_options=b'\202\323\344\223\002\036\"\031/api/v1/monitoring/ingest:\001*',
  ),
  _descriptor.MethodDescriptor(
    name='batchIngest',
    full_name='ai.verta.monitoring.IngestService.batchIngest',
    index=1,
    containing_service=None,
    input_type=_BATCHINGESTREQUEST,
    output_type=_BATCHINGESTREQUEST_RESPONSE,
    serialized_options=b'\202\323\344\223\002*\"%/api/v1/monitoring/ingest/batchIngest:\001*',
  ),
])
_sym_db.RegisterServiceDescriptor(_INGESTSERVICE)

DESCRIPTOR.services_by_name['IngestService'] = _INGESTSERVICE

# @@protoc_insertion_point(module_scope)
