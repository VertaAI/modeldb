"""
Basic tests to make sure the client passes `visibility` without errors.

"""


import pytest

from verta._protos.public.common import CommonService_pb2 as _CommonCommonService
from verta._protos.public.modeldb import DatasetService_pb2 as _DatasetService
from verta._protos.public.modeldb import ProjectService_pb2 as _ProjectService
from verta._protos.public.modeldb.versioning import VersioningService_pb2 as _VersioningService

from verta._internal_utils import _utils
from verta.visibility import (
    OrgCustom,
    Private,
)


def assert_visibility(entity, visibility, entity_name):
    assert entity._msg.custom_permission == visibility._custom_permission
    if entity_name == "registered_model":
        assert entity._msg.resource_visibility == visibility._visibility
    else:
        assert entity._msg.visibility == visibility._visibility


class TestCreate:
    @pytest.mark.parametrize(
        ("entity_name", "visibility"),
        [
            ("dataset", OrgCustom(write=True)),
            # ("endpoint", OrgCustom(write=True)),  # not implemented yet
            ("project", OrgCustom(write=True, deploy=True)),
            ("registered_model", OrgCustom(write=True, deploy=True)),
            # ("repository", OrgCustom(write=True)),  # no create_repository()
        ]
    )
    def test_create(self, client, organization, entity_name, visibility):
        create_entity = getattr(client, "create_{}".format(entity_name))

        entity = create_entity(
            name=_utils.generate_default_name(),
            workspace=organization.name, visibility=visibility,
        )
        try:
            assert_visibility(entity, visibility, entity_name)
        finally:
            entity.delete()
            client._ctx.proj = None  # otherwise client teardown tries to delete


class TestSet:
    @pytest.mark.parametrize(
        ("entity_name", "visibility"),
        [
            ("dataset", OrgCustom(write=True)),
            # ("endpoint", OrgCustom(write=True)),  # not implemented yet
            ("project", OrgCustom(write=True, deploy=True)),
            ("registered_model", OrgCustom(write=True, deploy=True)),
            ("repository", OrgCustom(write=True)),
        ]
    )
    def test_set(self, client, organization, entity_name, visibility):
        set_entity = getattr(client, "set_{}".format(entity_name))

        entity = set_entity(
            name=_utils.generate_default_name(),
            workspace=organization.name, visibility=visibility,
        )
        try:
            assert_visibility(entity, visibility, entity_name)

            # second set ignores visibility
            with pytest.warns(UserWarning, match="cannot set"):
                entity = set_entity(entity.name, workspace=organization.name, visibility=Private())
            assert_visibility(entity, visibility, entity_name)
        finally:
            entity.delete()
            client._ctx.proj = None  # otherwise client teardown tries to delete

class TestPublicWithinOrg:
    """
    `visibility` gets translated to an equivalent `public_within_org` value for
    compatibility with older backends.

    """
    def test_dataset(self, client, organization):
        visibility = OrgCustom(write=True)
        entity = client.set_dataset(workspace=organization.name, visibility=visibility)
        try:
            if visibility._to_public_within_org():
                assert entity._msg.dataset_visibility == _DatasetService.DatasetVisibilityEnum.ORG_SCOPED_PUBLIC
            else:
                assert entity._msg.dataset_visibility == _DatasetService.DatasetVisibilityEnum.PRIVATE
        finally:
            entity.delete()

    @pytest.mark.skip(reason="not implemented yet")
    def test_endpoint(self, client, organization):
        raise NotImplementedError

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

    def test_registered_model(self, client, organization):
        visibility = OrgCustom(write=True)
        entity = client.set_registered_model(workspace=organization.name, visibility=visibility)
        try:
            if visibility._to_public_within_org():
                assert entity._msg.visibility == _CommonCommonService.VisibilityEnum.ORG_SCOPED_PUBLIC
            else:
                assert entity._msg.visibility == _CommonCommonService.VisibilityEnum.PRIVATE
        finally:
            entity.delete()

    def test_repository(self, client, organization):
        visibility = OrgCustom(write=True)
        entity = client.set_repository(
            name=_utils.generate_default_name(),
            workspace=organization.name, visibility=visibility,
        )
        try:
            if visibility._to_public_within_org():
                assert entity._msg.repository_visibility == _VersioningService.RepositoryVisibilityEnum.ORG_SCOPED_PUBLIC
            else:
                assert entity._msg.repository_visibility == _VersioningService.RepositoryVisibilityEnum.PRIVATE
        finally:
            entity.delete()
