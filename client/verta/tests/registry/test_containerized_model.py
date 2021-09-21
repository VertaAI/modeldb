# -*- coding: utf-8 -*-

from verta.registry import _constants


class TestContainerizedModels:
    def test_log(self, registered_model, docker_image):
        model_ver = registered_model.create_containerized_model(docker_image)

        assert model_ver.get_docker() == docker_image

        attrs = model_ver.get_attributes()
        assert (
            attrs[_constants.MODEL_LANGUAGE_ATTR_KEY]
            == _constants.ModelLanguage.UNKNOWN
        )
        assert (
            attrs[_constants.MODEL_TYPE_ATTR_KEY]
            == _constants.ModelType.USER_CONTAINERIZED_MODEL
        )
