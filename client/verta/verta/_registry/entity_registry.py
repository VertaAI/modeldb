# -*- coding: utf-8 -*-

from verta._internal_utils import _utils
from verta._tracking.entity import _ModelDBEntity


class _ModelDBRegistryEntity(_ModelDBEntity):
    def _get_workspace_name_by_id(self, workspace_id):
        # getting workspace
        response = _utils.make_request(
            "GET",
            "{}://{}/api/v1/uac-proxy/workspace/getWorkspaceById".format(self._conn.scheme, self._conn.socket),
            self._conn, params={'id': workspace_id},
        )
        _utils.raise_for_http_error(response)
        response_body = _utils.body_to_json(response)
        if 'user_id' in response_body:
            user_id = response_body['user_id']
            # try getting user
            response = _utils.make_request(
                "GET",
                "{}://{}/api/v1/uac-proxy/uac/getUser".format(self._conn.scheme, self._conn.socket),
                self._conn, params={'user_id': user_id},
            )
            _utils.raise_for_http_error(response)

            # workspace is user
            return _utils.body_to_json(response)['verta_info']['username']
        else:
            org_id = response_body['org_id']
            # try getting organization
            response = _utils.make_request(
                "GET",
                "{}://{}/api/v1/uac-proxy/organization/getOrganizationById".format(self._conn.scheme,
                                                                                   self._conn.socket),
                self._conn, params={'org_id': org_id},
            )
            # workspace is organization
            return _utils.body_to_json(response)['organization']['name']
