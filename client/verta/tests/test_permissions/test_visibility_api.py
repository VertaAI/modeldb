"""
Basic tests to make sure the client passes `visibility` without errors.

"""
import pytest

import requests

from verta._protos.public.common import CommonService_pb2 as _CommonCommonService
from verta._protos.public.modeldb import DatasetService_pb2 as _DatasetService
from verta._protos.public.modeldb import ProjectService_pb2 as _ProjectService
from verta._protos.public.modeldb.versioning import VersioningService_pb2 as _VersioningService

from verta._internal_utils import _utils
from verta.visibility import (
    OrgCustom,
    Private,
)


pytestmark = pytest.mark.not_oss


def assert_visibility(entity, visibility, entity_name):
    if not entity._msg.HasField('custom_permission'):
        pytest.skip("backend does not support new visibility")

    assert entity._msg.custom_permission == visibility._custom_permission
    if entity_name == "registered_model":
        assert entity._msg.resource_visibility == visibility._visibility
    else:
        assert entity._msg.visibility == visibility._visibility


def assert_endpoint_visibility(endpoint, visibility):
    endpoint_json = endpoint._get_json_by_id(endpoint._conn, endpoint.workspace, endpoint.id)
    if 'custom_permission' not in endpoint_json['creator_request']:
        pytest.skip("backend does not support new visibility")

    assert endpoint_json['creator_request']['custom_permission']['collaborator_type'] == visibility._collaborator_type_str
    assert endpoint_json['creator_request']['resource_visibility'] == visibility._visibility_str


def assert_repository_visibility(repo, visibility):
    repo_msg = repo._get_proto_by_id(repo._conn, repo.id)

    if not repo_msg.HasField('custom_permission'):
        pytest.skip("backend does not support new visibility")

    assert repo_msg.custom_permission == visibility._custom_permission
    assert repo_msg.visibility == visibility._visibility


class TestCreate:
    @pytest.mark.parametrize(
        ("entity_name", "visibility"),
        [
            ("dataset", OrgCustom(write=True)),
            ("project", OrgCustom(write=True, deploy=True)),
            ("registered_model", OrgCustom(write=True, deploy=True)),
        ]
    )
    def test_mdb_entity(self, client, organization, entity_name, visibility):
        create_entity = getattr(client, "create_{}".format(entity_name))

        entity = create_entity(workspace=organization.name, visibility=visibility)
        try:
            assert_visibility(entity, visibility, entity_name)
        finally:
            entity.delete()
            client._ctx.proj = None  # otherwise client teardown tries to delete

    def test_endpoint(self, client, organization, created_entities):
        visibility = OrgCustom(write=True)

        endpoint = client.create_endpoint(
            path=_utils.generate_default_name(),
            workspace=organization.name, visibility=visibility,
        )
        created_entities.append(endpoint)
        assert_endpoint_visibility(endpoint, visibility)

    @pytest.mark.skip(reason="client.create_repository() does not yet exist")
    def test_repository(self, client, organization):
        raise NotImplementedError


class TestSet:
    @pytest.mark.parametrize(
        ("entity_name", "visibility"),
        [
            ("dataset", OrgCustom(write=True)),
            ("project", OrgCustom(write=True, deploy=True)),
            ("registered_model", OrgCustom(write=True, deploy=True)),
        ]
    )
    def test_mdb_entity(self, client, organization, entity_name, visibility):
        set_entity = getattr(client, "set_{}".format(entity_name))

        entity = set_entity(workspace=organization.name, visibility=visibility)
        try:
            assert_visibility(entity, visibility, entity_name)

            # second set ignores visibility
            with pytest.warns(UserWarning, match="cannot set"):
                entity = set_entity(entity.name, workspace=organization.name, visibility=Private())
            assert_visibility(entity, visibility, entity_name)
        finally:
            entity.delete()
            client._ctx.proj = None  # otherwise client teardown tries to delete

    def test_endpoint(self, client, organization, created_entities):
        visibility = OrgCustom(write=True)

        endpoint = client.set_endpoint(
            path=_utils.generate_default_name(),
            workspace=organization.name, visibility=visibility,
        )
        created_entities.append(endpoint)

        assert_endpoint_visibility(endpoint, visibility)

        # second set ignores visibility
        with pytest.warns(UserWarning, match="cannot set"):
            endpoint = client.set_endpoint(path=endpoint.path, workspace=organization.name, visibility=Private())
        assert_endpoint_visibility(endpoint, visibility)

    def test_repository(self, client, organization, created_entities):
        visibility = OrgCustom(write=True)

        repo = client.set_repository(
            name=_utils.generate_default_name(),
            workspace=organization.name, visibility=visibility,
        )
        created_entities.append(repo)

        assert_repository_visibility(repo, visibility)

        # second set ignores visibility
        repo = client.set_repository(name=repo.name, workspace=organization.name, visibility=Private())
        assert_repository_visibility(repo, visibility)


class TestPublicWithinOrg:
    """
    `visibility` gets translated to an equivalent `public_within_org` value for
    compatibility with older backends.

    """
    def test_dataset(self, client, organization, created_entities):
        visibility = OrgCustom(write=True)
        dataset = client.set_dataset(workspace=organization.name, visibility=visibility)
        created_entities.append(dataset)

        if visibility._to_public_within_org():
            assert dataset._msg.dataset_visibility == _DatasetService.DatasetVisibilityEnum.ORG_SCOPED_PUBLIC
        else:
            assert dataset._msg.dataset_visibility == _DatasetService.DatasetVisibilityEnum.PRIVATE

    def test_endpoint(self, client, organization, created_entities):
        visibility = OrgCustom(write=True)
        endpoint = client.set_endpoint(
            path=_utils.generate_default_name(),
            workspace=organization.name, visibility=visibility,
        )
        created_entities.append(endpoint)

        endpoint_json = endpoint._get_json_by_id(endpoint._conn, endpoint.workspace, endpoint.id)
        if visibility._to_public_within_org():
            assert endpoint_json['creator_request']['visibility'] == "ORG_SCOPED_PUBLIC"
        else:
            assert endpoint_json['creator_request']['visibility'] == "PRIVATE"

    def test_project(self, client, organization):
        visibility = OrgCustom(write=True)
        entity = client.set_project(workspace=organization.name, visibility=visibility)
        try:
            if visibility._to_public_within_org():
                assert entity._msg.project_visibility == _ProjectService.ORG_SCOPED_PUBLIC
            else:
                assert entity._msg.project_visibility == _ProjectService.PRIVATE
        finally:
            entity.delete()
            client._ctx.proj = None  # otherwise client teardown tries to delete

    def test_registered_model(self, client, organization, created_entities):
        visibility = OrgCustom(write=True)
        entity = client.set_registered_model(workspace=organization.name, visibility=visibility)
        created_entities.append(entity)

        if visibility._to_public_within_org():
            assert entity._msg.visibility == _CommonCommonService.VisibilityEnum.ORG_SCOPED_PUBLIC
        else:
            assert entity._msg.visibility == _CommonCommonService.VisibilityEnum.PRIVATE

    def test_repository(self, client, organization, created_entities):
        visibility = OrgCustom(write=True)
        repo = client.set_repository(
            name=_utils.generate_default_name(),
            workspace=organization.name, visibility=visibility,
        )
        created_entities.append(repo)

        retrieved_visibility = repo._get_proto_by_id(repo._conn, repo.id).repository_visibility
        if visibility._to_public_within_org():
            assert retrieved_visibility == _VersioningService.RepositoryVisibilityEnum.ORG_SCOPED_PUBLIC
        else:
            assert retrieved_visibility == _VersioningService.RepositoryVisibilityEnum.PRIVATE
