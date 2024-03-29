# -*- coding: utf-8 -*-

from verta._protos.public.registry import ModelMetadata_pb2
from verta.tracking.entities._deployable_entity import _RESERVED_ATTR_PREFIX


MODEL_LANGUAGE_ATTR_KEY = f"{_RESERVED_ATTR_PREFIX}model_language"
MODEL_TYPE_ATTR_KEY = f"{_RESERVED_ATTR_PREFIX}model_type"


class ModelLanguage(object):
    UNKNOWN = ModelMetadata_pb2.ModelLanguageEnum.ModelLanguage.Name(
        ModelMetadata_pb2.ModelLanguageEnum.Unknown,
    )
    PYTHON = ModelMetadata_pb2.ModelLanguageEnum.ModelLanguage.Name(
        ModelMetadata_pb2.ModelLanguageEnum.Python,
    )


class ModelType(object):
    CUSTOM = ModelMetadata_pb2.ModelTypeEnum.ModelType.Name(
        ModelMetadata_pb2.ModelTypeEnum.Custom,
    )
    STANDARD_VERTA_MODEL = ModelMetadata_pb2.ModelTypeEnum.ModelType.Name(
        ModelMetadata_pb2.ModelTypeEnum.StandardVertaModel,
    )
    USER_CONTAINERIZED_MODEL = ModelMetadata_pb2.ModelTypeEnum.ModelType.Name(
        ModelMetadata_pb2.ModelTypeEnum.UserContainerizedModel,
    )
