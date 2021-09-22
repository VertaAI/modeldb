# -*- coding: utf-8 -*-


class TestContainerizedModels:
    def test_log(self, registered_model, docker_image):
        model_ver = registered_model.create_containerized_model(docker_image)

        assert model_ver.get_docker() == docker_image
