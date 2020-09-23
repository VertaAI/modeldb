# -*- coding: utf-8 -*-

from __future__ import print_function

from .._protos.public.modeldb.versioning import VersioningService_pb2 as _VersioningService

from .._internal_utils import _utils
from . import commit

import requests


class Repository(object):
    """
    ModelDB Repository.

    There should not be a need to instantiate this class directly; please use
    :meth:`Client.get_or_create_repository() <verta.client.Client.get_or_create_repository>`.

    Attributes
    ----------
    id : str
        ID of the Repository.
    name : str
        Name of the Repository.

    """
    def __init__(self, conn, id_):
        self._conn = conn

        self.id = id_

    def __repr__(self):
        return "<Repository \"{}\">".format(self.name)

    @property
    def _endpoint_prefix(self):
        return "{}://{}/api/v1/modeldb/versioning/repositories/{}".format(
            self._conn.scheme,
            self._conn.socket,
            self.id,
        )

    @property
    def name(self):
        response = _utils.make_request("GET", self._endpoint_prefix, self._conn)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response),
                                            _VersioningService.GetRepositoryRequest.Response)
        return response_msg.repository.name

    @property
    def workspace(self):
        raise NotImplementedError

    @classmethod
    def _create(cls, conn, name, workspace, public_within_org):
        msg = _VersioningService.Repository()
        msg.name = name
        if public_within_org:
            if workspace is None:
                raise ValueError("cannot set `public_within_org` for personal workspace")
            elif not _utils.is_org(workspace, conn):
                raise ValueError(
                    "cannot set `public_within_org`"
                    " because workspace \"{}\" is not an organization".format(workspace)
                )
            else:
                msg.repository_visibility = _VersioningService.RepositoryVisibilityEnum.ORG_SCOPED_PUBLIC

        data = _utils.proto_to_json(msg)
        endpoint = "{}://{}/api/v1/modeldb/versioning/workspaces/{}/repositories".format(
            conn.scheme,
            conn.socket,
            workspace,
        )
        response = _utils.make_request("POST", endpoint, conn, json=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response),
                                            _VersioningService.SetRepository.Response)
        return cls(conn, response_msg.repository.id)

    @classmethod
    def _get(cls, conn, name=None, workspace=None, id_=None):
        if name and workspace and not id_:
            endpoint = "{}://{}/api/v1/modeldb/versioning/workspaces/{}/repositories/{}".format(
                conn.scheme,
                conn.socket,
                workspace,
                name,
            )
        elif not name and not workspace and id_:
            endpoint = "{}://{}/api/v1/modeldb/versioning/repositories/{}".format(
                conn.scheme,
                conn.socket,
                id_,
            )
        else:
            raise RuntimeError("the Client has encountered an error;"
                               " please notify the Verta development team")
        response = _utils.make_request("GET", endpoint, conn)

        if not response.ok:
            if ((response.status_code == 403 and _utils.body_to_json(response)['code'] == 7)
                    or (response.status_code == 404 and _utils.body_to_json(response)['code'] == 5)):
                return None
            else:
                _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response),
                                            _VersioningService.GetRepositoryRequest.Response)
        return cls(conn, response_msg.repository.id)

    def get_commit(self, branch=None, tag=None, id=None):
        """
        Returns the Commit with the specified `branch`, `tag`, or `id`.

        If no arguments are passed, ``branch="master"`` is the default.

        Parameters
        ----------
        branch : str, optional
            Branch of the Commit.
        tag : str, optional
            Tag of the Commit.
        id : str, optional
            ID of the Commit.

        Returns
        -------
        :class:`Commit`
            Specified Commit.

        """
        num_args = sum(map(lambda x: x is not None, [tag, id, branch]))
        if num_args > 1:
            raise ValueError("cannot specify more than one of `branch`, `tag`, and `id`")
        if num_args == 0:
            branch = "master"

        if branch is not None:
            msg = _VersioningService.GetBranchRequest()
            endpoint = "{}://{}/api/v1/modeldb/versioning/repositories/{}/branches/{}".format(
                self._conn.scheme,
                self._conn.socket,
                self.id,
                branch,
            )
        elif tag is not None:
            msg = _VersioningService.GetTagRequest()
            endpoint = "{}://{}/api/v1/modeldb/versioning/repositories/{}/tags/{}".format(
                self._conn.scheme,
                self._conn.socket,
                self.id,
                tag,
            )
        elif id is not None:
            msg = _VersioningService.GetCommitRequest()
            endpoint = "{}://{}/api/v1/modeldb/versioning/repositories/{}/commits/{}".format(
                self._conn.scheme,
                self._conn.socket,
                self.id,
                id,
            )
        response = _utils.make_request("GET", endpoint, self._conn)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), msg.Response)
        return commit.Commit._from_id(self._conn, self, response_msg.commit.commit_sha, branch_name=branch)

    def delete(self):
        """
        Deletes this repository.

        """
        request_url = "{}://{}/api/v1/modeldb/versioning/repositories/{}".format(self._conn.scheme, self._conn.socket, self.id)
        response = requests.delete(request_url, headers=self._conn.auth)
        _utils.raise_for_http_error(response)

