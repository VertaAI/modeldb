import pytest
import json

import requests

from verta import Client
from verta._internal_utils import _utils
from verta._protos.public.uac import Organization_pb2 as _Organization
from verta._protos.public.common import CommonService_pb2 as _CommonCommonService
from verta._tracking.organization import Organization, CollaboratorType


def test_create_msg():
    assert Organization._create_msg("name", "desc", None, None) == \
           _Organization.Organization(name="name", description="desc",
                                      global_can_deploy=_CommonCommonService.TernaryEnum.Ternary.FALSE)
    assert Organization._create_msg("name", "desc", CollaboratorType(
        default_repo_collaborator_type="READ_WRITE",
        default_endpoint_collaborator_type="READ_ONLY"), True) == \
           _Organization.Organization(name="name", description="desc",
                                      global_can_deploy=_CommonCommonService.TernaryEnum.Ternary.TRUE,
                                      default_repo_collaborator_type=
                                      _CommonCommonService.CollaboratorTypeEnum.CollaboratorType.READ_WRITE)
    with pytest.raises(ValueError):
        assert Organization._create_msg("name", "desc", CollaboratorType(
            default_repo_collaborator_type="READ_WRITET",
            default_endpoint_collaborator_type="READ_ONLY"), True)


class TestOrganization:
    def test_create(self, client):
        name = _utils.generate_default_name()
        org = client._create_organization(name)
        assert org
        assert org.id == client._get_organization(name).id

        org.delete()

    def test_create_same_name_diff_workspace(self, client, organization, created_endpoints, created_registered_models, created_datasets):
        # creating some entities:
        project_name = _utils.generate_default_name()
        exp_name = _utils.generate_default_name()
        run_name = _utils.generate_default_name()
        dataset_name = _utils.generate_default_name()
        repository_name = _utils.generate_default_name()
        model_name = _utils.generate_default_name()
        version_name = _utils.generate_default_name()
        endpoint_path = _utils.generate_default_name()

        project = client.create_project(project_name)
        exp = client.create_experiment(exp_name)
        run = client.create_experiment_run(run_name)
        repository = client.get_or_create_repository(name=repository_name)

        dataset = client._create_dataset2(dataset_name)
        created_datasets.append(dataset)
        model = client.create_registered_model(name=model_name)
        version = model.create_version(name=version_name)
        created_registered_models.append(model)

        endpoint = client.create_endpoint(path=endpoint_path)
        created_endpoints.append(endpoint)

        # create entities with same name, but different workspace:
        new_model = client.create_registered_model(name=model_name, workspace=organization.name)
        new_version = new_model.create_version(name=version_name)
        # new_endpoint = client.create_endpoint(path=endpoint_path, workspace=organization.name)  TODO: uncomment after VR-6053
        # TODO: remove followinng three lines after VR-6053; until then, endpoints with same name diff workspace is a 409
        with pytest.raises(requests.HTTPError) as excinfo:
            client.create_endpoint(path=endpoint_path, workspace=organization.name)
        assert excinfo.value.response.status_code == 409

        new_project = client.create_project(project_name, workspace=organization.name)
        new_exp = client.create_experiment(exp_name)
        new_run = client.create_experiment_run(run_name)
        new_repository = client.get_or_create_repository(name=repository_name, workspace=organization.name)

        new_dataset = client._create_dataset2(dataset_name, workspace=organization.name)
        created_datasets.append(new_dataset)

        # created_endpoints.append(new_endpoint)  TODO: uncomment after VR-6053
        created_registered_models.append(new_model)

        assert model.id != new_model.id
        assert version.id != new_version.id
        # assert endpoint.id != new_endpoint.id  TODO: uncommment after VR-6053
        assert project.id != new_project.id
        assert exp.id != new_exp.id
        assert run.id != new_run.id
        assert dataset.id != new_dataset.id
        assert repository.id != new_repository.id

        project.delete()  # have to delete manually because creating dataset makes project untracked by client context.
        new_project.delete()
        repository.delete()
