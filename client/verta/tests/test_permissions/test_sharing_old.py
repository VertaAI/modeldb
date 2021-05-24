"""
Original collaboration/sharing tests, before the visibility overhaul.

"""
import pytest

from verta._internal_utils import _utils

pytestmark = pytest.mark.not_oss


class TestProject:
    def test_share_project_personal_workspace(self, client, client_2, email_2):
        """
        User 1 share a project in personal workspace to user 2.
        """
        project_name = _utils.generate_default_name()
        project = client.create_project(project_name)
        project._add_collaborator(email=email_2)

        assert client_2.get_project(id=project.id)
        assert client_2.get_project(name=project.name)

    def test_org_public_project(self, client, organization, client_2, email_2):
        """
        User 2 tries to access a org-public project created by a user in the same organization.
        """
        project_name = _utils.generate_default_name()
        project = client.create_project(project_name, workspace=organization.name, public_within_org=True)

        organization.add_member(email_2)

        assert client_2.get_project(id=project.id)
        assert client_2.get_project(name=project.name, workspace=organization.name)

    def test_non_org_public_project_access_error(self, client, organization, client_2, email_2):
        """
        User 2 tries to access a non-org-public project created by a user in the same organization.
        """
        project_name = _utils.generate_default_name()
        project = client.create_project(project_name, workspace=organization.name, public_within_org=False)

        organization.add_member(email_2)

        # Shouldn't be able to access:
        with pytest.raises(ValueError, match="not found"):
            client_2.get_project(id=project.id)

    def test_share_org_project(self, client, organization, client_2, email_2):
        """
        User 2 tries to access a non-org-public project created by another user, but has been shared to user 2.
        """
        project_name = _utils.generate_default_name()
        project = client.create_project(project_name, workspace=organization.name, public_within_org=False)

        organization.add_member(email_2)
        project._add_collaborator(email=email_2)

        assert client_2.get_project(id=project.id)
        assert client_2.get_project(name=project.name, workspace=organization.name)


class TestDataset:
    def test_org_public_dataset(self, client, organization, client_2, email_2, created_entities):
        """
        User 2 tries to access a org-public dataset created by a user in the same organization.
        """
        dataset_name = _utils.generate_default_name()
        dataset = client.create_dataset(dataset_name, workspace=organization.name, public_within_org=True)
        created_entities.append(dataset)

        organization.add_member(email_2)

        assert client_2.get_dataset(id=dataset.id)
        assert client_2.get_dataset(name=dataset.name, workspace=organization.name)

    def test_non_org_public_dataset_access_error(self, client, organization, client_2, email_2, created_entities):
        """
        User 2 tries to access a non-org-public dataset created by a user in the same organization.
        """
        dataset_name = _utils.generate_default_name()
        dataset = client.create_dataset(dataset_name, workspace=organization.name, public_within_org=False)
        created_entities.append(dataset)

        organization.add_member(email_2)

        # Shouldn't be able to access:
        with pytest.raises(ValueError, match="not found"):
            client_2.get_dataset(id=dataset.id)


class TestRegisteredModel:
    def test_org_public_registered_model(self, client, organization, client_2, email_2, created_entities):
        """
        User 2 tries to access a org-public registered_model created by a user in the same organization.
        """
        registered_model_name = _utils.generate_default_name()
        registered_model = client.create_registered_model(registered_model_name, workspace=organization.name, public_within_org=True)
        created_entities.append(registered_model)

        organization.add_member(email_2)

        assert client_2.get_registered_model(id=registered_model.id)
        assert client_2.get_registered_model(name=registered_model.name, workspace=organization.name)

    def test_non_org_public_registered_model_access_error(self, client, organization, client_2, email_2, created_entities):
        """
        User 2 tries to access a non-org-public registered_model created by a user in the same organization.
        """
        registered_model_name = _utils.generate_default_name()
        registered_model = client.create_registered_model(registered_model_name, workspace=organization.name, public_within_org=False)
        created_entities.append(registered_model)

        organization.add_member(email_2)

        # Shouldn't be able to access:
        with pytest.raises(ValueError, match="not found"):
            client_2.get_registered_model(id=registered_model.id)


class TestRepository:
    def test_org_public_repository(self, client, organization, client_2, email_2, created_entities):
        """
        User 2 tries to access a org-public repository created by a user in the same organization.
        """
        repository_name = _utils.generate_default_name()
        repository = client.set_repository(repository_name, workspace=organization.name, public_within_org=True)
        created_entities.append(repository)

        organization.add_member(email_2)

        assert client_2.get_or_create_repository(id=repository.id)
        assert client_2.get_or_create_repository(name=repository.name, workspace=organization.name).id == repository.id

    def test_non_org_public_repository_access_error(self, client, organization, client_2, email_2, created_entities):
        """
        User 2 tries to access a non-org-public repository created by a user in the same organization.
        """
        repository_name = _utils.generate_default_name()
        repository = client.set_repository(repository_name, workspace=organization.name, public_within_org=False)
        created_entities.append(repository)

        organization.add_member(email_2)

        # Shouldn't be able to access:
        with pytest.raises(ValueError, match="no Repository found"):
            client_2.get_or_create_repository(id=repository.id)


class TestEndpoint:
    def test_org_endpoint(self, client, organization, client_2, email_2, created_entities):
        """
        Non-owner access to org-public endpoint and private endpoint within an org.
        """
        organization.add_member(email_2)
        path = _utils.generate_default_name()

        # ORG_SCOPED_PUBLIC
        public_path = "public-{}".format(path)
        endpoint = client.create_endpoint(public_path, workspace=organization.name, public_within_org=True)
        created_entities.append(endpoint)
        client_2.get_endpoint(public_path, workspace=organization.name)

        # PRIVATE
        private_path = "private-{}".format(path)
        endpoint = client.create_endpoint(private_path, workspace=organization.name, public_within_org=False)
        created_entities.append(endpoint)
        with pytest.raises(ValueError, match="Endpoint not found"):
            client_2.get_endpoint(private_path, workspace=organization.name)
