"""
End-to-end tests for org permissions access and actions.

"""
import pytest

import requests

from verta._internal_utils import _utils
from verta.visibility import (
    OrgCustom,
    Private,
)
from verta.dataset import Path
from verta.environment import Python


pytestmark = pytest.mark.not_oss

pytest.skip("visibility deprecated; tests disabled for permissions v2", allow_module_level=True)

class TestAccess:
    @pytest.mark.parametrize(
        "entity_name",
        ["dataset", "endpoint", "project", "registered_model"],  # , "repository"],
    )
    def test_private(
        self, client, client_2, organization, created_entities, entity_name
    ):
        """Org member cannot get."""
        organization.add_member(client_2._conn.email)
        client.set_workspace(organization.name)
        client_2.set_workspace(organization.name)
        name = _utils.generate_default_name()
        visibility = Private()

        entity = getattr(client, "create_{}".format(entity_name))(
            name, visibility=visibility
        )
        created_entities.append(entity)

        with pytest.raises(Exception, match="not found|Denied"):
            getattr(client_2, "get_{}".format(entity_name))(name)

    @pytest.mark.parametrize(
        "entity_name",
        ["dataset", "endpoint", "project", "registered_model"],  # , "repository"],
    )
    def test_read(self, client, client_2, organization, created_entities, entity_name):
        """Org member can get, but not delete."""
        organization.add_member(client_2._conn.email)
        client.set_workspace(organization.name)
        client_2.set_workspace(organization.name)
        name = _utils.generate_default_name()
        visibility = OrgCustom(write=False)

        entity = getattr(client, "create_{}".format(entity_name))(
            name, visibility=visibility
        )
        created_entities.append(entity)

        retrieved_entity = getattr(client_2, "get_{}".format(entity_name))(name)
        assert retrieved_entity.id == entity.id

        with pytest.raises(requests.HTTPError, match="^403"):
            retrieved_entity.delete()

    def test_read_registry(self, client, client_2, organization, created_entities):
        """Registry entities erroneously masked 403s in _update()."""
        organization.add_member(client_2._conn.email)
        client.set_workspace(organization.name)
        client_2.set_workspace(organization.name)
        visibility = OrgCustom(write=False)

        reg_model = client.create_registered_model(visibility=visibility)
        retrieved_reg_model = client_2.get_registered_model(reg_model.name)
        with pytest.raises(requests.HTTPError, match="^403"):
            retrieved_reg_model.add_label("foo")

        model_ver = reg_model.create_version()
        retrieved_model_ver = retrieved_reg_model.get_version(model_ver.name)
        with pytest.raises(requests.HTTPError, match="^403"):
            retrieved_model_ver.add_label("foo")

    @pytest.mark.parametrize(
        "entity_name",
        ["dataset", "endpoint", "project", "registered_model"],  # , "repository"],
    )
    def test_read_write(
        self, client, client_2, organization, created_entities, entity_name
    ):
        """Org member can get, and delete."""
        organization.add_member(client_2._conn.email)
        client.set_workspace(organization.name)
        client_2.set_workspace(organization.name)
        name = _utils.generate_default_name()
        visibility = OrgCustom(write=True)

        entity = getattr(client, "create_{}".format(entity_name))(
            name, visibility=visibility
        )

        try:
            retrieved_entity = getattr(client_2, "get_{}".format(entity_name))(name)
            retrieved_entity.delete()
        except:
            created_entities.append(entity)


class TestLink:
    def test_run_log_dataset_version(
        self, client_2, client_3, organization, created_entities
    ):
        """Log someone else's dataset version to my run."""
        organization.add_member(client_2._conn.email)
        organization.add_member(client_3._conn.email)
        client_2.set_workspace(organization.name)
        client_3.set_workspace(organization.name)

        created_entities.append(client_2.create_project())
        run = client_2.create_experiment_run()

        # private dataset version
        dataset = client_3.create_dataset(visibility=Private())
        created_entities.append(dataset)
        dataver = dataset.create_version(Path(__file__))
        with pytest.raises(requests.HTTPError, match="^403"):
            run.log_dataset_version("train", dataver)

        # org dataset version
        dataset = client_3.create_dataset()
        created_entities.append(dataset)
        dataver = dataset.create_version(Path(__file__))
        run.log_dataset_version("train", dataver)
        assert run.get_dataset_version("train").id == dataver.id

    def test_model_version_from_run(
        self, client_2, client_3, organization, created_entities
    ):
        """Create model version from someone else's run."""
        organization.add_member(client_2._conn.email)
        organization.add_member(client_3._conn.email)
        client_2.set_workspace(organization.name)
        client_3.set_workspace(organization.name)

        reg_model = client_2.create_registered_model()
        created_entities.append(reg_model)

        # private run
        created_entities.append(client_3.create_project(visibility=Private()))
        run = client_3.create_experiment_run()
        with pytest.raises(requests.HTTPError, match="^404.*not found"):
            reg_model.create_version_from_run(run.id)

        # org run
        created_entities.append(client_3.create_project())
        run = client_3.create_experiment_run()
        model_ver = reg_model.create_version_from_run(run.id)
        assert model_ver._msg.experiment_run_id == run.id

    @pytest.mark.deployment
    def test_endpoint_update_run(
        self, client_2, client_3, organization, created_entities
    ):
        """Update endpoint from someone else's run."""
        LogisticRegression = pytest.importorskip(
            "sklearn.linear_model"
        ).LogisticRegression

        organization.add_member(client_2._conn.email)
        organization.add_member(client_3._conn.email)
        client_2.set_workspace(organization.name)
        client_3.set_workspace(organization.name)

        endpoint = client_2.create_endpoint(_utils.generate_default_name())
        created_entities.append(endpoint)

        # private run
        created_entities.append(client_3.create_project(visibility=Private()))
        run = client_3.create_experiment_run()
        run.log_model(LogisticRegression(), custom_modules=[])
        run.log_environment(Python(["scikit-learn"]))
        with pytest.raises(requests.HTTPError, match="^404.*not found"):
            endpoint.update(run)

        # org run, deploy=False
        created_entities.append(
            client_3.create_project(visibility=OrgCustom(deploy=False))
        )
        run = client_3.create_experiment_run()
        run.log_model(LogisticRegression(), custom_modules=[])
        run.log_environment(Python(["scikit-learn"]))
        with pytest.raises(requests.HTTPError, match="^403"):
            endpoint.update(run)

        # org run, deploy=True
        created_entities.append(
            client_3.create_project(visibility=OrgCustom(deploy=True))
        )
        run = client_3.create_experiment_run()
        run.log_model(LogisticRegression(), custom_modules=[])
        run.log_environment(Python(["scikit-learn"]))
        assert endpoint.update(run)

    @pytest.mark.deployment
    def test_endpoint_update_model_version(
        self, client_2, client_3, organization, created_entities
    ):
        """Update endpoint from someone else's model version."""
        LogisticRegression = pytest.importorskip(
            "sklearn.linear_model"
        ).LogisticRegression

        organization.add_member(client_2._conn.email)
        organization.add_member(client_3._conn.email)
        client_2.set_workspace(organization.name)
        client_3.set_workspace(organization.name)

        endpoint = client_2.create_endpoint(_utils.generate_default_name())
        created_entities.append(endpoint)

        # private model version
        reg_model = client_3.create_registered_model(visibility=Private())
        created_entities.append(reg_model)
        model_ver = reg_model.create_version()
        model_ver.log_model(LogisticRegression(), custom_modules=[])
        model_ver.log_environment(Python(["scikit-learn"]))
        with pytest.raises(
            requests.HTTPError, match="^404 Client Error: Couldn't find modelVersion"
        ):
            endpoint.update(model_ver)

        # org model version, deploy=False
        reg_model = client_3.create_registered_model(visibility=OrgCustom(deploy=False))
        created_entities.append(reg_model)
        model_ver = reg_model.create_version()
        model_ver.log_model(LogisticRegression(), custom_modules=[])
        model_ver.log_environment(Python(["scikit-learn"]))
        with pytest.raises(requests.HTTPError, match="^403 Client Error: Forbidden"):
            endpoint.update(model_ver)

        # org model version, deploy=True
        reg_model = client_3.create_registered_model(visibility=OrgCustom(deploy=True))
        created_entities.append(reg_model)
        model_ver = reg_model.create_version()
        model_ver.log_model(LogisticRegression(), custom_modules=[])
        model_ver.log_environment(Python(["scikit-learn"]))
        assert endpoint.update(model_ver)
