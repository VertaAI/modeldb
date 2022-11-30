"""
Original collaboration/sharing tests, before the visibility overhaul.

"""
import pytest

from verta._internal_utils import _utils

pytestmark = pytest.mark.not_oss


class TestProject:
    def test_org_public_project(self, client, workspace, client_2, email_2):
        """
        User 2 tries to access a org-public project created by a user in the same workspace.
        """
        project_name = _utils.generate_default_name()
        project = client.create_project(
            project_name, workspace=workspace.name, public_within_org=True
        )

        workspace.add_member(email_2)

        assert client_2.get_project(id=project.id)
        assert client_2.get_project(name=project.name, workspace=workspace.name)

    def test_non_org_public_project_access_error(
        self, client, workspace, client_2, email_2
    ):
        """
        User 2 tries to access a non-org-public project created by a user in the same workspace.
        """
        project_name = _utils.generate_default_name()
        project = client.create_project(
            project_name, workspace=workspace.name, public_within_org=False
        )

        workspace.add_member(email_2)

        # Shouldn't be able to access:
        with pytest.raises(ValueError, match="not found"):
            client_2.get_project(id=project.id)

class TestDataset:
    def test_org_public_dataset(
        self, client, workspace, client_2, email_2, created_entities
    ):
        """
        User 2 tries to access a org-public dataset created by a user in the same workspace.
        """
        dataset_name = _utils.generate_default_name()
        dataset = client.create_dataset(
            dataset_name, workspace=workspace.name, public_within_org=True
        )
        created_entities.append(dataset)

        workspace.add_member(email_2)

        assert client_2.get_dataset(id=dataset.id)
        assert client_2.get_dataset(name=dataset.name, workspace=workspace.name)

    def test_non_org_public_dataset_access_error(
        self, client, workspace, client_2, email_2, created_entities
    ):
        """
        User 2 tries to access a non-org-public dataset created by a user in the same workspace.
        """
        dataset_name = _utils.generate_default_name()
        dataset = client.create_dataset(
            dataset_name, workspace=workspace.name, public_within_org=False
        )
        created_entities.append(dataset)

        workspace.add_member(email_2)

        # Shouldn't be able to access:
        with pytest.raises(ValueError, match="not found"):
            client_2.get_dataset(id=dataset.id)


class TestRegisteredModel:
    def test_org_public_registered_model(
        self, client, workspace, client_2, email_2, created_entities
    ):
        """
        User 2 tries to access a org-public registered_model created by a user in the same workspace.
        """
        registered_model_name = _utils.generate_default_name()
        registered_model = client.create_registered_model(
            registered_model_name, workspace=workspace.name, public_within_org=True
        )
        created_entities.append(registered_model)

        workspace.add_member(email_2)

        assert client_2.get_registered_model(id=registered_model.id)
        assert client_2.get_registered_model(
            name=registered_model.name, workspace=workspace.name
        )

    def test_non_org_public_registered_model_access_error(
        self, client, workspace, client_2, email_2, created_entities
    ):
        """
        User 2 tries to access a non-org-public registered_model created by a user in the same workspace.
        """
        registered_model_name = _utils.generate_default_name()
        registered_model = client.create_registered_model(
            registered_model_name, workspace=workspace.name, public_within_org=False
        )
        created_entities.append(registered_model)

        workspace.add_member(email_2)

        # Shouldn't be able to access:
        with pytest.raises(ValueError, match="not found"):
            client_2.get_registered_model(id=registered_model.id)


class TestEndpoint:
    def test_org_endpoint(
        self, client, workspace, client_2, email_2, created_entities
    ):
        """
        Non-owner access to org-public endpoint and private endpoint within an org.
        """
        workspace.add_member(email_2)
        path = _utils.generate_default_name()

        # ORG_SCOPED_PUBLIC
        public_path = "public-{}".format(path)
        endpoint = client.create_endpoint(
            public_path, workspace=workspace.name, public_within_org=True
        )
        created_entities.append(endpoint)
        client_2.get_endpoint(public_path, workspace=workspace.name)

        # PRIVATE
        private_path = "private-{}".format(path)
        endpoint = client.create_endpoint(
            private_path, workspace=workspace.name, public_within_org=False
        )
        created_entities.append(endpoint)
        with pytest.raises(ValueError, match="Endpoint not found"):
            client_2.get_endpoint(private_path, workspace=workspace.name)
