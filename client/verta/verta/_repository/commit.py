# -*- coding: utf-8 -*-

from __future__ import print_function

import collections
from datetime import datetime
import heapq
import time

import requests

from .._protos.public.modeldb.versioning import VersioningService_pb2 as _VersioningService

from ..external import six

from .._internal_utils import _utils
from .. import code
from .. import configuration
from .. import dataset
from .. import environment
from . import blob as blob_module
from . import diff as diff_module


class Commit(object):
    """
    Commit within a ModelDB Repository.

    There should not be a need to instantiate this class directly; please use
    :meth:`Repository.get_commit() <verta._repository.Repository.get_commit>`.

    Attributes
    ----------
    id : str or None
        ID of the Commit, or ``None`` if the Commit has not yet been saved.

    """
    def __init__(self, conn, repo, commit_msg, branch_name=None):
        self._conn = conn
        self._commit_json = _utils.proto_to_json(commit_msg)  # dict representation of Commit protobuf

        self._repo = repo
        self._parent_ids = list(collections.OrderedDict.fromkeys(commit_msg.parent_shas or []))  # remove duplicates while maintaining order

        self.branch_name = branch_name  # TODO: find a way to clear if branch is moved

        self._blobs = dict()  # will be loaded when needed
        self._loaded_from_remote = False

    @property
    def id(self):
        return self._commit_json['commit_sha'] or None

    @property
    def parent(self):
        return self._repo.get_commit(id=self._parent_ids[0]) if self._parent_ids else None

    def _lazy_load_blobs(self):
        if self._loaded_from_remote:
            return

        # until Commits can be created from blob diffs, load in blobs
        if self.id is not None:
            self._update_blobs_from_commit(self.id)
        else:
            for parent_id in self._parent_ids:
                # parents will be read in first-to-last, possibly overwriting previous blobs
                self._update_blobs_from_commit(parent_id)

        self._loaded_from_remote = True

    def describe(self):
        self._lazy_load_blobs()

        contents = '\n'.join((
            "{} ({}.{})".format(path, blob.__class__.__module__.split('.')[1], blob.__class__.__name__)
            for path, blob
            in sorted(six.viewitems(self._blobs))
        ))
        if not contents:
            contents = "<no contents>"

        components = [self.__repr__(), 'Contents:', contents]
        return '\n'.join(components)

    def __repr__(self):
        branch_and_tag = ' '.join((
            "Branch: {}".format(self.branch_name) if self.branch_name is not None else '',
            # TODO: put tag here
        ))
        if self.id is None:
            header = "unsaved Commit"
            if branch_and_tag:
                header = header +  " (was {})".format(branch_and_tag)
        else:
            header = "Commit {}".format(self.id)
            if branch_and_tag:
                header = header +  " ({})".format(branch_and_tag)

        # TODO: add author
        # TODO: make data more similar to git
        date_created = int(self._commit_json['date_created'])  # protobuf uint64 is str, so cast to int
        date = 'Date: ' + datetime.fromtimestamp(date_created/1000.).strftime('%Y-%m-%d %H:%M:%S')
        message = '\n'.join('    ' + c for c in self._commit_json['message'].split('\n'))
        components = [header, date, '', message, '']
        return '\n'.join(components)

    @classmethod
    def _from_id(cls, conn, repo, id_, **kwargs):
        endpoint = "{}://{}/api/v1/modeldb/versioning/repositories/{}/commits/{}".format(
            conn.scheme,
            conn.socket,
            repo.id,
            id_,
        )
        response = _utils.make_request("GET", endpoint, conn)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response),
                                            _VersioningService.GetCommitRequest.Response)
        commit_msg = response_msg.commit
        return cls(conn, repo, commit_msg, **kwargs)

    @staticmethod
    def _raise_lookup_error(path):
        e = LookupError("Commit does not contain path \"{}\"".format(path))
        six.raise_from(e, None)

    # TODO: consolidate this with similar method in `_ModelDBEntity`
    def _get_url_for_artifact(self, blob_path, dataset_component_path, method, part_num=0):
        """
        Obtains a URL to use for accessing stored artifacts.

        Parameters
        ----------
        blob_path : str
            Path to blob within repo.
        dataset_component_path : str
            Filepath in dataset component blob.
        method : {'GET', 'PUT'}
            HTTP method to request for the generated URL.
        part_num : int, optional
            If using Multipart Upload, number of part to be uploaded.

        Returns
        -------
        response_msg : `_VersioningService.GetUrlForBlobVersioned.Response`
            Backend response.

        """
        if method.upper() not in ("GET", "PUT"):
            raise ValueError("`method` must be one of {'GET', 'PUT'}")

        Message = _VersioningService.GetUrlForBlobVersioned
        msg = Message(
            location=path_to_location(blob_path),
            path_dataset_component_blob_path=dataset_component_path,
            method=method,
            part_number=part_num,
        )
        data = _utils.proto_to_json(msg)
        endpoint = "{}://{}/api/v1/modeldb/versioning/repositories/{}/commits/{}/getUrlForBlobVersioned".format(
            self._conn.scheme,
            self._conn.socket,
            self._repo.id,
            self.id,
        )
        response = _utils.make_request("POST", endpoint, self._conn, json=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(response.json(), Message.Response)

        url = response_msg.url
        # accommodate port-forwarded NFS store
        if 'https://localhost' in url[:20]:
            url = 'http' + url[5:]
        if 'localhost%3a' in url[:20]:
            url = url.replace('localhost%3a', 'localhost:')
        if 'localhost%3A' in url[:20]:
            url = url.replace('localhost%3A', 'localhost:')
        response_msg.url = url

        return response_msg

    # TODO: consolidate this with similar method in `ExperimentRun`
    def _upload_artifact(self, blob_path, dataset_component_path, file_handle, part_size=64*(10**6)):
        """
        Uploads `file_handle` to ModelDB artifact store.

        Parameters
        ----------
        blob_path : str
            Path to blob within repo.
        dataset_component_path : str
            Filepath in dataset component blob.
        file_handle : file-like
            Artifact to be uploaded.
        part_size : int, default 64 MB
            If using multipart upload, number of bytes to upload per part.

        """
        file_handle.seek(0)

        # check if multipart upload ok
        url_for_artifact = self._get_url_for_artifact(blob_path, dataset_component_path, "PUT", part_num=1)

        print("uploading {} to ModelDB".format(dataset_component_path))
        if url_for_artifact.multipart_upload_ok:
            # TODO: parallelize this
            file_parts = iter(lambda: file_handle.read(part_size), b'')
            for part_num, file_part in enumerate(file_parts, start=1):
                print("uploading part {}".format(part_num), end='\r')

                # get presigned URL
                url = self._get_url_for_artifact(blob_path, dataset_component_path, "PUT", part_num=part_num).url

                # wrap file part into bytestream to avoid OverflowError
                #     Passing a bytestring >2 GB (num bytes > max val of int32) directly to
                #     ``requests`` will overwhelm CPython's SSL lib when it tries to sign the
                #     payload. But passing a buffered bytestream instead of the raw bytestring
                #     indicates to ``requests`` that it should perform a streaming upload via
                #     HTTP/1.1 chunked transfer encoding and avoid this issue.
                #     https://github.com/psf/requests/issues/2717
                part_stream = six.BytesIO(file_part)

                # upload part
                response = _utils.make_request("PUT", url, self._conn, data=part_stream)
                _utils.raise_for_http_error(response)

                # commit part
                url = "{}://{}/api/v1/modeldb/versioning/commitVersionedBlobArtifactPart".format(
                    self._conn.scheme,
                    self._conn.socket,
                )
                msg = _VersioningService.CommitVersionedBlobArtifactPart(
                    commit_sha=self.id,
                    location=path_to_location(blob_path),
                    path_dataset_component_blob_path=dataset_component_path,
                )
                msg.repository_id.repo_id = self._repo.id
                msg.artifact_part.part_number = part_num
                msg.artifact_part.etag = response.headers['ETag']
                data = _utils.proto_to_json(msg)
                response = _utils.make_request("POST", url, self._conn, json=data)
                _utils.raise_for_http_error(response)
            print()

            # complete upload
            url = "{}://{}/api/v1/modeldb/versioning/commitMultipartVersionedBlobArtifact".format(
                self._conn.scheme,
                self._conn.socket,
            )
            msg = _VersioningService.CommitMultipartVersionedBlobArtifact(
                commit_sha=self.id,
                location=path_to_location(blob_path),
                path_dataset_component_blob_path=dataset_component_path,
            )
            msg.repository_id.repo_id = self._repo.id
            data = _utils.proto_to_json(msg)
            response = _utils.make_request("POST", url, self._conn, json=data)
            _utils.raise_for_http_error(response)
        else:
            # upload full artifact
            if url_for_artifact.fields:
                # if fields were returned by backend, make a POST request and supply them as form fields
                response = _utils.make_request(
                    "POST", url_for_artifact.url, self._conn,
                    # requests uses the `files` parameter for sending multipart/form-data POSTs.
                    #     https://stackoverflow.com/a/12385661/8651995
                    # the file contents must be the final form field
                    #     https://docs.aws.amazon.com/AmazonS3/latest/dev/HTTPPOSTForms.html#HTTPPOSTFormFields
                    files=list(url_for_artifact.fields.items()) + [('file', file_handle)],
                )
            else:
                response = _utils.make_request("PUT", url_for_artifact.url, self._conn, data=file_handle)
            _utils.raise_for_http_error(response)

        print("upload complete")

    def _update_blobs_from_commit(self, id_):
        """Fetches commit `id_`'s blobs and stores them as objects in `self._blobs`."""
        endpoint = "{}://{}/api/v1/modeldb/versioning/repositories/{}/commits/{}/blobs".format(
            self._conn.scheme,
            self._conn.socket,
            self._repo.id,
            id_,
        )
        response = _utils.make_request("GET", endpoint, self._conn)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response),
                                            _VersioningService.ListCommitBlobsRequest.Response)
        self._blobs.update({
            '/'.join(blob_msg.location): blob_msg_to_object(blob_msg.blob)
            for blob_msg
            in response_msg.blobs
        })

    def _become_child(self):
        """
        This method is for when `self` had been saved and is then modified, meaning that
        this commit object has become a child of the commit that had been saved.

        """
        self._lazy_load_blobs()
        self._parent_ids = [self.id]
        self._commit_json['commit_sha'] = ""

    def _become_saved_child(self, child_id):
        """
        This method is for when a child commit is created in the back end from `self`, and `self`
        and its branch need to be updated to become that newly-created commit.

        """
        if self.branch_name is not None:
            # update branch to child commit
            set_branch(self._conn, self._repo.id, child_id, self.branch_name)
            new_commit = self._repo.get_commit(branch=self.branch_name)
        else:
            new_commit = self._repo.get_commit(id=child_id)

        self.__dict__ = new_commit.__dict__

    def _to_create_msg(self, commit_message):
        self._lazy_load_blobs()

        msg = _VersioningService.CreateCommitRequest()
        msg.repository_id.repo_id = self._repo.id  # pylint: disable=no-member
        msg.commit.parent_shas.extend(self._parent_ids)  # pylint: disable=no-member
        msg.commit.message = commit_message

        for path, blob in six.viewitems(self._blobs):
            blob_msg = _VersioningService.BlobExpanded()
            blob_msg.location.extend(path_to_location(path))  # pylint: disable=no-member
            blob_msg.blob.CopyFrom(blob._as_proto())
            msg.blobs.append(blob_msg)  # pylint: disable=no-member

        return msg

    def walk(self):
        """
        Generates folder names and blob names in this commit by walking through its folder tree.

        Similar to the Python standard library's ``os.walk()``, the yielded `folder_names` can be
        modified in-place to remove subfolders from upcoming iterations or alter the order in which
        they are to be visited.

        Note that, also similar to ``os.walk()``, `folder_names` and `blob_names` are simply the
        *names* of those entities, and *not* their full paths.

        Yields
        ------
        folder_path : str
            Path to current folder.
        folder_names : list of str
            Names of subfolders in `folder_path`.
        blob_names : list of str
            Names of blobs in `folder_path`.

        """
        if self.id is None:
            raise RuntimeError("Commit must be saved before it can be walked")

        endpoint = "{}://{}/api/v1/modeldb/versioning/repositories/{}/commits/{}/path".format(
            self._conn.scheme,
            self._conn.socket,
            self._repo.id,
            self.id,
        )

        locations = [()]
        while locations:
            location = locations.pop()

            msg = _VersioningService.GetCommitComponentRequest()
            msg.location.extend(location)  # pylint: disable=no-member
            data = _utils.proto_to_json(msg)
            response = _utils.make_request("GET", endpoint, self._conn, params=data)
            _utils.raise_for_http_error(response)

            response_msg = _utils.json_to_proto(_utils.body_to_json(response), msg.Response)
            folder_msg = response_msg.folder

            folder_path = '/'.join(location)
            folder_names = list(sorted(element.element_name for element in folder_msg.sub_folders))
            blob_names = list(sorted(element.element_name for element in folder_msg.blobs))
            yield (folder_path, folder_names, blob_names)

            locations.extend(
                location + (folder_name,)
                for folder_name
                in reversed(folder_names)  # maintains order, because locations are popped from end
            )

    def update(self, path, blob):
        """
        Adds `blob` to this Commit at `path`.

        If `path` is already in this Commit, it will be updated to the new `blob`.

        Parameters
        ----------
        path : str
            Location to add `blob` to.
        blob : :ref:`Blob <blobs>`
            ModelDB versioning blob.

        """
        if not isinstance(blob, blob_module.Blob):
            raise TypeError("unsupported type {}".format(type(blob)))

        self._lazy_load_blobs()

        if self.id is not None:
            self._become_child()

        self._blobs[path] = blob

    def get(self, path):
        """
        Retrieves the blob at `path` from this Commit.

        Parameters
        ----------
        path : str
            Location of a blob.

        Returns
        -------
        blob : :ref:`Blob <blobs>`
            ModelDB versioning blob.

        Raises
        ------
        LookupError
            If `path` is not in this Commit.

        """
        self._lazy_load_blobs()

        try:
            blob = self._blobs[path]
        except KeyError:
            self._raise_lookup_error(path)

        if isinstance(blob, dataset._Dataset):
            # for _Dataset.download()
            blob._set_commit_and_blob_path(self, path)

        return blob

    def remove(self, path):
        """
        Deletes the blob at `path` from this Commit.

        Parameters
        ----------
        path : str
            Location of a blob.

        Raises
        ------
        LookupError
            If `path` is not in this Commit.

        """
        self._lazy_load_blobs()

        if self.id is not None:
            self._become_child()

        try:
            del self._blobs[path]
        except KeyError:
            self._raise_lookup_error(path)

    def save(self, message):
        """
        Saves this commit to ModelDB.

        .. note::

            If this commit contains new S3 datasets to be versioned by ModelDB, a very large
            temporary download may occur before uploading them to ModelDB.

        Parameters
        ----------
        message : str
            Description of this Commit.

        """
        # prepare ModelDB-versioned blobs, and track for upload after commit save
        mdb_versioned_blobs = dict()
        for blob_path, blob in self._blobs.items():
            if isinstance(blob, dataset._Dataset) and blob._mdb_versioned:
                blob._prepare_components_to_upload()
                mdb_versioned_blobs[blob_path] = blob

        msg = self._to_create_msg(commit_message=message)
        self._save(msg)

        # upload ModelDB-versioned blobs
        for blob_path, blob in mdb_versioned_blobs.items():
            for component in blob._components_map.values():
                if component._internal_versioned_path:
                    with open(component._local_path, 'rb') as f:
                        self._upload_artifact(blob_path, component.path, f)

            blob._clean_up_uploaded_components()

    def _save(self, proto_message):
        data = _utils.proto_to_json(proto_message)
        endpoint = "{}://{}/api/v1/modeldb/versioning/repositories/{}/commits".format(
            self._conn.scheme,
            self._conn.socket,
            self._repo.id,
        )
        response = _utils.make_request("POST", endpoint, self._conn, json=data)
        _utils.raise_for_http_error(response)
        response_msg = _utils.json_to_proto(_utils.body_to_json(response), proto_message.Response)

        self._become_saved_child(response_msg.commit.commit_sha)

    # TODO: Add ways to retrieve and delete tag
    def tag(self, tag):
        """
        Assigns a tag to this Commit.

        Parameters
        ----------
        tag : str
            Tag.

        Raises
        ------
        RuntimeError
            If this Commit has not yet been saved.

        """
        if self.id is None:
            raise RuntimeError("Commit must be saved before it can be tagged")

        data = self.id
        endpoint = "{}://{}/api/v1/modeldb/versioning/repositories/{}/tags/{}".format(
            self._conn.scheme,
            self._conn.socket,
            self._repo.id,
            tag,
        )
        response = _utils.make_request("PUT", endpoint, self._conn, json=data)
        _utils.raise_for_http_error(response)

    def log(self):
        """
        Yields ancestors, starting from this Commit until the root of the Repository.

        Analogous to ``git log``.

        Yields
        ------
        commit : :class:`Commit`
            Ancestor commit.

        """
        if self.id is None:  # unsaved commit
            # use parent
            commit_id = self._parent_ids[0]
        else:
            commit_id = self.id

        endpoint = "{}://{}/api/v1/modeldb/versioning/repositories/{}/commits/{}/log".format(
            self._conn.scheme,
            self._conn.socket,
            self._repo.id,
            commit_id,
        )
        response = _utils.make_request("GET", endpoint, self._conn)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response),
                                            _VersioningService.ListCommitsLogRequest.Response)
        commits = response_msg.commits

        for c in commits:
            yield Commit(self._conn, self._repo, c, self.branch_name if c.commit_sha == commit_id else None)

    def new_branch(self, branch):
        """
        Creates a branch at this Commit and returns the checked-out branch.

        If `branch` already exists, it will be moved to this Commit.

        Parameters
        ----------
        branch : str
            Branch name.

        Returns
        -------
        commit : :class:`Commit`
            This Commit as the head of `branch`.

        Raises
        ------
        RuntimeError
            If this Commit has not yet been saved.

        Examples
        --------
        .. code-block:: python

            master = repo.get_commit(branch="master")
            dev = master.new_branch("development")

        """
        if self.id is None:
            raise RuntimeError("Commit must be saved before it can be attached to a branch")

        set_branch(self._conn, self._repo.id, self.id, branch)

        return self._repo.get_commit(branch=branch)

    def diff_from(self, reference=None):
        """
        Returns the diff from `reference` to `self`.

        Parameters
        ----------
        reference : :class:`Commit`, optional
            Commit to be compared to.

        Returns
        -------
        :class:`~verta._repository.diff.Diff`
            Commit diff.

        Raises
        ------
        RuntimeError
            If this Commit or `reference` has not yet been saved, or if they do not belong to the
            same Repository.

        """
        if self.id is None:
            raise RuntimeError("Commit must be saved before a diff can be calculated")

        if reference is None:
            reference_id = self._parent_ids[0]
        elif not isinstance(reference, Commit) or reference.id is None:
            raise TypeError("`reference` must be a saved Commit")
        elif self._repo.id != reference._repo.id:
            raise ValueError("Commit and `reference` must belong to the same Repository")
        else:
            reference_id = reference.id

        endpoint = "{}://{}/api/v1/modeldb/versioning/repositories/{}/diff?commit_a={}&commit_b={}".format(
            self._conn.scheme,
            self._conn.socket,
            self._repo.id,
            reference_id,
            self.id,
        )
        response = _utils.make_request("GET", endpoint, self._conn)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response),
                                            _VersioningService.ComputeRepositoryDiffRequest.Response)
        return diff_module.Diff(response_msg.diffs)

    def apply_diff(self, diff, message, other_parents=[]):
        """
        Applies a diff to this Commit.

        This method creates a new Commit in ModelDB, and assigns a new ID to this object.

        Parameters
        ----------
        diff : :class:`~verta._repository.diff.Diff`
            Commit diff.
        message : str
            Description of the diff.

        Raises
        ------
        RuntimeError
            If this Commit has not yet been saved.

        """
        if self.id is None:
            raise RuntimeError("Commit must be saved before a diff can be applied")

        msg = _VersioningService.CreateCommitRequest()
        msg.repository_id.repo_id = self._repo.id
        msg.commit.parent_shas.append(self.id)
        msg.commit.parent_shas.extend(other_parents)
        msg.commit.message = message
        msg.commit_base = self.id
        msg.diffs.extend(diff._diffs)

        self._save(msg)

    def get_revert_diff(self):
        return self.parent.diff_from(self)

    def revert(self, other=None, message=None):
        """
        Reverts `other`.

        This method creates a new Commit in ModelDB, and assigns a new ID to this object.

        Parameters
        ----------
        other : :class:`Commit`, optional
            Commit to be reverted. If not provided, this Commit will be reverted.
        message : str, optional
            Description of the revert. If not provided, a default message will be used.

        Raises
        ------
        RuntimeError
            If this Commit or `other` has not yet been saved, or if they do not belong to the
            same Repository.

        """
        if self.id is None:
            raise RuntimeError("Commit must be saved before a revert can be performed")

        if other is None:
            other = self
        elif not isinstance(other, Commit) or other.id is None:
            raise TypeError("`other` must be a saved Commit")
        elif self._repo.id != other._repo.id:
            raise ValueError("Commit and `other` must belong to the same Repository")

        msg = _VersioningService.RevertRepositoryCommitsRequest()
        msg.base_commit_sha = self.id
        msg.commit_to_revert_sha = other.id
        if message is not None:
            msg.content.message = message

        data = _utils.proto_to_json(msg)
        endpoint = "{}://{}/api/v1/modeldb/versioning/repositories/{}/commits/{}/revert".format(
            self._conn.scheme,
            self._conn.socket,
            self._repo.id,
            msg.commit_to_revert_sha,
        )
        response = _utils.make_request("POST", endpoint, self._conn, json=data)
        _utils.raise_for_http_error(response)
        response_msg = _utils.json_to_proto(_utils.body_to_json(response), msg.Response)

        self._become_saved_child(response_msg.commit.commit_sha)

    def _to_heap_element(self):
        date_created = int(self._commit_json['date_created'])  # protobuf uint64 is str, so cast to int
        # Most recent has higher priority
        return (-date_created, self.id, self)

    def get_common_parent(self, other):
        if self.id is None:
            raise RuntimeError("Commit must be saved before a common parent can be calculated")

        if not isinstance(other, Commit) or other.id is None:
            raise TypeError("`other` must be a saved Commit")
        elif self._repo.id != other._repo.id:
            raise ValueError("Commit and `other` must belong to the same Repository")

        # Keep a set of all parents we see for each side. This doesn't have to be *all* but facilitates implementation
        left_ids = set([self.id])
        right_ids = set([other.id])

        # Keep a heap of all candidate commits to be the common parent, ordered by the date so that we fetch the most recent first
        heap = []
        heapq.heappush(heap, self._to_heap_element())
        heapq.heappush(heap, other._to_heap_element())

        while heap:
            # Get the most recent commit
            _, _, commit = heapq.heappop(heap)

            # If it's in the list for both sides, then it's a parent of both and return
            if commit.id in left_ids and commit.id in right_ids:
                return commit

            # Update the heap with all the current parents
            parent_ids = commit._parent_ids
            for parent_id in parent_ids:
                parent_commit = self._repo.get_commit(id=parent_id)
                heap_element = parent_commit._to_heap_element()
                try:
                    heapq.heappush(heap, heap_element)
                except TypeError:  # already in heap, because comparison between Commits failed
                    pass

            # Update the parent sets based on which side the commit came from
            # We know the commit came from the left if its ID is in the left set. If it was on the right too, then it would be the parent and we would have returned early
            if commit.id in left_ids:
                left_ids.update(parent_ids)
            if commit.id in right_ids:
                right_ids.update(parent_ids)

        # Should never happen, since we have the initial commit
        return None

    def merge(self, other, message=None):
        """
        Merges a branch headed by `other` into this Commit.

        This method creates a new Commit in ModelDB, and assigns a new ID to this object.

        Parameters
        ----------
        other : :class:`Commit`
            Commit to be merged.
        message : str, optional
            Description of the merge. If not provided, a default message will be used.

        Raises
        ------
        RuntimeError
            If this Commit or `other` has not yet been saved, or if they do not belong to the
            same Repository.

        """
        if self.id is None:
            raise RuntimeError("Commit must be saved before a merge can be performed")

        if not isinstance(other, Commit) or other.id is None:
            raise TypeError("`other` must be a saved Commit")
        elif self._repo.id != other._repo.id:
            raise ValueError("Commit and `other` must belong to the same Repository")

        msg = _VersioningService.MergeRepositoryCommitsRequest()
        if self.branch_name is not None:
            msg.branch_b = self.branch_name
        else:
            msg.commit_sha_b = self.id
        if other.branch_name is not None:
            msg.branch_a = other.branch_name
        else:
            msg.commit_sha_a = other.id
        if message is not None:
            msg.content.message = message

        data = _utils.proto_to_json(msg)
        endpoint = "{}://{}/api/v1/modeldb/versioning/repositories/{}/merge".format(
            self._conn.scheme,
            self._conn.socket,
            self._repo.id,
        )
        response = _utils.make_request("POST", endpoint, self._conn, json=data)
        _utils.raise_for_http_error(response)
        response_msg = _utils.json_to_proto(_utils.body_to_json(response), msg.Response)

        # raise for conflict
        if response_msg.conflicts:
            raise RuntimeError('\n    '.join([
                "merge conflict",
                "resolution is not currently supported through the Client",
                "please create a new Commit with the updated blobs",
                "see https://docs.verta.ai/en/master/examples/tutorials/merge.html for instructions",
            ]))

        self._become_saved_child(response_msg.commit.commit_sha)


def blob_msg_to_object(blob_msg):
    # TODO: make this more concise
    content_type = blob_msg.WhichOneof('content')
    content_subtype = None
    blob_cls = None
    if content_type == 'code':
        content_subtype = blob_msg.code.WhichOneof('content')
        if content_subtype == 'git':
            blob_cls = code.Git
        elif content_subtype == 'notebook':
            blob_cls = code.Notebook
    elif content_type == 'config':
        blob_cls = configuration.Hyperparameters
    elif content_type == 'dataset':
        content_subtype = blob_msg.dataset.WhichOneof('content')
        if content_subtype == 's3':
            blob_cls = dataset.S3
        elif content_subtype == 'path':
            blob_cls = dataset.Path
    elif content_type == 'environment':
        content_subtype = blob_msg.environment.WhichOneof('content')
        if content_subtype == 'python':
            blob_cls = environment.Python
        elif content_subtype == 'docker':
            raise NotImplementedError

    if blob_cls is None:
        if content_subtype is None:
            raise NotImplementedError("found unexpected content type {};"
                                      " please notify the Verta development team".format(content_type))
        else:
            raise NotImplementedError("found unexpected {} type {};"
                                      " please notify the Verta development team".format(content_type, content_subtype))

    return blob_cls._from_proto(blob_msg)


def path_to_location(path):
    """Messages take a `repeated string` of path components."""
    if path.startswith('/'):
        # `path` is already meant to be relative to repo root
        path = path[1:]

    return path.split('/')


def location_to_path(location):
    return '/'.join(location)


def set_branch(conn, repo_id, commit_id, branch):
    """Sets `branch` to Commit `commit_id`."""
    data = commit_id
    endpoint = "{}://{}/api/v1/modeldb/versioning/repositories/{}/branches/{}".format(
        conn.scheme,
        conn.socket,
        repo_id,
        branch,
    )
    response = _utils.make_request("PUT", endpoint, conn, json=data)
    _utils.raise_for_http_error(response)
