import json

import pytest

from verta._internal_utils import (
    _config_utils,
    _utils,
)


pytestmark = pytest.mark.not_oss


class TestAttr:
    """`workspace` attribute of entity objects."""

    @pytest.mark.parametrize(
        "entity_name",
        ["dataset", "project", "registered_model"],
    )
    def test_top_level_entities(
        self, client, workspace, created_entities, entity_name
    ):
        set_entity = getattr(client, "set_{}".format(entity_name))
        try:
            # default workspace
            entity = set_entity(_utils.generate_default_name())
            created_entities.append(entity)
            assert entity.workspace == client._conn.get_default_workspace()

            # organization workspace
            entity = set_entity(
                _utils.generate_default_name(), workspace=workspace.name
            )
            created_entities.append(entity)
            assert entity.workspace == workspace.name
        finally:
            client._ctx.proj = None  # otherwise client teardown tries to delete

    def test_expt_run(self, client, created_entities, workspace):
        try:
            # default workspace
            created_entities.append(client.create_project())
            assert (
                client.create_experiment_run().workspace
                == client._conn.get_default_workspace()
            )

            # organization workspace
            created_entities.append(client.create_project(workspace=workspace.name))
            assert client.create_experiment_run().workspace == workspace.name
        finally:
            client._ctx.proj = None  # otherwise client teardown tries to delete

    def test_model_ver(self, client, workspace, created_entities):
        # default workspace
        reg_model = client.create_registered_model()
        created_entities.append(reg_model)
        assert (
            reg_model.create_version().workspace == client._conn.get_default_workspace()
        )

        # organization workspace
        reg_model = client.create_registered_model(workspace=workspace.name)
        created_entities.append(reg_model)
        assert reg_model.create_version().workspace == workspace.name

