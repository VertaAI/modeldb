# -*- coding: utf-8 -*-

import itertools
import logging
import multiprocessing
import sys

import requests
from verta import Client

# TODO: structure files better so we don't have to do this
if __name__ == "__main__":
    import constants  # pylint: disable=relative-import
else:
    from . import constants


logger = logging.getLogger(__name__)


def configure_logger():
    """Configure logger formatting and verbosity."""
    formatter = logging.Formatter("%(levelname)s - %(message)s")

    handler = logging.StreamHandler(sys.stdout)
    handler.setLevel(logging.DEBUG)
    handler.setFormatter(formatter)

    logger.setLevel(logging.DEBUG)
    logger.addHandler(handler)


def get_clients():
    """Return test account clients using available env var credentials.

    Returns
    -------
    list of :class:`~verta.Client`

    """
    credentials = [
        (constants.EMAIL, constants.DEV_KEY),
        (constants.EMAIL_2, constants.DEV_KEY_2),
        (constants.EMAIL_3, constants.DEV_KEY_3),
    ]

    clients = []
    for email, dev_key in credentials:
        if email and dev_key:
            clients.append(
                Client(constants.HOST, email=email, dev_key=dev_key)
            )

    return clients


def delete_build(args):
    """Helper function to delete a single build.

    This is meant to be called from :func:`delete_builds`.

    Parameters
    ----------
    args : 3-tuple of :class:`~verta.Client`, workspace str, and build ID int

    Notes
    -----
    This would take more helpful parameters than just `args` if not for
    ``starmap()`` being unavailable in Python 2's ``multiprocessing``.

    """
    client, workspace, build_id = args

    response = requests.delete(
        "{}://{}/api/v1/deployment/workspace/{}/builds/{}".format(
            client._conn.scheme,
            client._conn.socket,
            workspace,
            build_id,
        ),
        headers=client._conn.auth,
    )
    if not response.ok:
        logger.warning(
            "%s failed to delete %s in %s (%s)",
            client._conn.email,
            build_id,
            workspace,
            response.status_code,
        )
    else:
        logger.info(
            "%s deleted %s in %s",
            client._conn.email,
            build_id,
            workspace,
        )


def delete_builds(clients):
    """Delete all builds in all workspaces of `clients`.

    Parameters
    ----------
    clients : list of :class:`~verta.Client`

    """
    logger.info("deleting builds")
    for client in clients:
        workspaces = (
            client._conn._get_visible_orgs()
            + [client._conn.get_personal_workspace()]
        )
        for workspace in workspaces:
            # get builds
            response = requests.get(
                "{}://{}/api/v1/deployment/workspace/{}/builds".format(
                    client._conn.scheme,
                    client._conn.socket,
                    workspace,
                ),
                headers=client._conn.auth,
            )
            try:
                client._conn.must_response(response)
            except requests.HTTPError:
                logger.exception(
                    "%s failed to list builds in %s",
                    client._conn.email,
                    workspace,
                )
                continue

            build_ids = [build["id"] for build in response.json().get("builds", [])]
            p = multiprocessing.Pool(12)
            p.map(delete_build, zip(itertools.repeat(client), itertools.repeat(workspace), build_ids))
            p.close()


def delete_endpoints(clients):
    """Delete all endpoints in all workspaces of `clients`.

    Parameters
    ----------
    clients : list of :class:`~verta.Client`

    """
    logger.info("deleting endpoints")
    for client in clients:
        workspaces = (
            client._conn._get_visible_orgs()
            + [client._conn.get_personal_workspace()]
        )
        for workspace in workspaces:
            for endpoint in client.endpoints.with_workspace(workspace):
                path = endpoint.path  # need to get from obj before deletion
                try:
                    endpoint.delete()
                except requests.HTTPError as e:
                    logger.warning(
                        "%s failed to delete %s in %s (%s)",
                        client._conn.email,
                        path,
                        workspace,
                        e.response.status_code,
                    )
                else:
                    logger.info(
                        "%s deleted %s in %s",
                        client._conn.email,
                        path,
                        workspace,
                    )


def delete_orgs(clients):
    """Delete all organizations of `clients`.

    Parameters
    ----------
    clients : list of :class:`~verta.Client`

    """
    logger.info("deleting orgs")
    for client in clients:
        for org_name in client._conn._get_visible_orgs():
            try:
                client._get_organization(org_name).delete()
            except requests.HTTPError as e:
                logger.warning(
                    "%s failed to delete %s (%s)",
                    client._conn.email,
                    org_name,
                    e.response.status_code,
                )
            else:
                logger.info(
                    "%s deleted %s",
                    client._conn.email,
                    org_name,
                )


def main():
    clients = get_clients()

    delete_builds(clients)
    delete_endpoints(clients)

    delete_orgs(clients)


configure_logger()
if __name__ == "__main__":
    main()
