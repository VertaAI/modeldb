import pytest

from verta._internal_utils import _utils


pytestmark = pytest.mark.not_oss


class TestAttr:
    """`project.workspace`, etc."""
    @pytest.mark.parametrize(
        "entity_name",
        ["dataset", "project", "repository", "registered_model"],
    )
    def test_top_level_entities(self, client, organization, created_entities, entity_name):
        set_entity = getattr(client, "set_{}".format(entity_name))
        try:
            # default workspace
            entity = set_entity(_utils.generate_default_name())
            created_entities.append(entity)
            assert entity.workspace == client._conn.get_default_workspace()

            # organization workspace
            entity = set_entity(_utils.generate_default_name(), workspace=organization.name)
            created_entities.append(entity)
            assert entity.workspace == organization.name
        finally:
            client._ctx.proj = None  # otherwise client teardown tries to delete

    def test_expt_run(self, client, created_entities, organization):
        try:
            # default workspace
            created_entities.append(client.create_project())
            assert client.create_experiment_run().workspace == client._conn.get_default_workspace()

            # organization workspace
            created_entities.append(client.create_project(workspace=organization.name))
            assert client.create_experiment_run().workspace == organization.name
        finally:
            client._ctx.proj = None  # otherwise client teardown tries to delete

    def test_model_ver(self, client, organization, created_entities):
        # default workspace
        reg_model = client.create_registered_model()
        created_entities.append(reg_model)
        assert reg_model.create_version().workspace == client._conn.get_default_workspace()

        # organization workspace
        reg_model = client.create_registered_model(workspace=organization.name)
        created_entities.append(reg_model)
        assert reg_model.create_version().workspace == organization.name
