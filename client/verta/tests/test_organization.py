import pytest
import json

import requests

from verta import Client
from verta._internal_utils import _utils
from verta._protos.public.uac import Organization_pb2 as _Organization
from verta._protos.public.common import CommonService_pb2 as _CommonCommonService
from verta.tracking._organization import Organization, CollaboratorType

pytestmark = pytest.mark.not_oss


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

    def test_create_same_name_diff_workspace(self, client, organization, created_entities):
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
        created_entities.append(project)
        exp = client.create_experiment(exp_name)
        run = client.create_experiment_run(run_name)
        repository = client.get_or_create_repository(name=repository_name)
        created_entities.append(repository)

        dataset = client.create_dataset(dataset_name)
        created_entities.append(dataset)
        model = client.create_registered_model(name=model_name)
        version = model.create_version(name=version_name)
        created_entities.append(model)

        endpoint = client.create_endpoint(path=endpoint_path)
        created_entities.append(endpoint)

        # create entities with same name, but different workspace:
        new_model = client.create_registered_model(name=model_name, workspace=organization.name)
        created_entities.append(new_model)
        new_version = new_model.create_version(name=version_name)
        # new_endpoint = client.create_endpoint(path=endpoint_path, workspace=organization.name)  TODO: uncomment after VR-6053
        # created_entities.append(new_endpoint)  TODO: uncomment after VR-6053
        # TODO: remove followinng three lines after VR-6053; until then, endpoints with same name diff workspace is a 409
        with pytest.raises(requests.HTTPError) as excinfo:
            client.create_endpoint(path=endpoint_path, workspace=organization.name)
        assert excinfo.value.response.status_code == 409

        new_project = client.create_project(project_name, workspace=organization.name)
        created_entities.append(new_project)
        new_exp = client.create_experiment(exp_name)
        new_run = client.create_experiment_run(run_name)
        new_repository = client.get_or_create_repository(name=repository_name, workspace=organization.name)
        created_entities.append(new_repository)

        new_dataset = client.create_dataset(dataset_name, workspace=organization.name)
        created_entities.append(new_dataset)


        assert model.id != new_model.id
        assert version.id != new_version.id
        # assert endpoint.id != new_endpoint.id  TODO: uncommment after VR-6053
        assert project.id != new_project.id
        assert exp.id != new_exp.id
        assert run.id != new_run.id
        assert dataset.id != new_dataset.id
        assert repository.id != new_repository.id
