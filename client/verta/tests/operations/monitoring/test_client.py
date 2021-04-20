# -*- coding: utf-8 -*-

from verta._internal_utils._utils import generate_default_name

class TestClient(object):

    def test_get_or_create_monitored_entity(self, client):
        monitored_entity = client.operations.get_or_create_monitored_entity()

        user_defined_name = generate_default_name()
        another_entity = client.operations.get_or_create_monitored_entity(name=user_defined_name)

        name = monitored_entity.name
        retrieved_entity = client.operations.get_or_create_monitored_entity(name=name)
        assert monitored_entity.id == retrieved_entity.id
