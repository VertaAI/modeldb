import pytest
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
    @pytest.mark.skip("delete not implemented yet")
    def test_create(self, client):
        name = _utils.generate_default_name()
        assert client._create_organization(name)
