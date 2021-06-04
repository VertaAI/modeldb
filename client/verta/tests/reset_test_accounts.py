# -*- coding: utf-8 -*-

import itertools
import multiprocessing

import requests
from verta import Client

import constants  # pylint: disable=relative-import


def get_clients():
    clients = [
        Client(
            constants.HOST,
            email=constants.EMAIL,
            dev_key=constants.DEV_KEY,
        )
    ]

    if constants.EMAIL_2 and constants.DEV_KEY_2:
        clients.append(
            Client(
                constants.HOST,
                email=constants.EMAIL_2,
                dev_key=constants.DEV_KEY_2,
            )
        )

    if constants.EMAIL_3 and constants.DEV_KEY_3:
        clients.append(
            Client(
                constants.HOST,
                email=constants.EMAIL_3,
                dev_key=constants.DEV_KEY_3,
            )
        )

    return clients


def delete_build(args):
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
        msg = "{} failed to delete {} in {} ({})".format(
            client._conn.email,
            build_id,
            workspace,
            response.status_code,
        )
    else:
        msg = "{} deleted {} in {}".format(
            client._conn.email,
            build_id,
            workspace,
        )

    print(msg)


def delete_builds(clients):
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
            if not response.ok:
                print(
                    "{} failed to list builds in {} ({})".format(
                        client._conn.email,
                        workspace,
                        response.status_code,
                    )
                )
                continue

            build_ids = [build["id"] for build in response.json().get("builds", [])]
            p = multiprocessing.Pool(12)
            p.map(delete_build, zip(itertools.repeat(client), itertools.repeat(workspace), build_ids))
            p.close()


def delete_endpoints(clients):
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
                    msg = "{} failed to delete {} in {} ({})".format(
                        client._conn.email,
                        path,
                        workspace,
                        e.response.status_code,
                    )
                else:
                    msg = "{} deleted {} in {}".format(
                        client._conn.email,
                        path,
                        workspace,
                    )

                print(msg)


def delete_orgs(clients):
    for client in clients:
        for org_name in client._conn._get_visible_orgs():
            if "do-not-delete" in org_name:
                continue

            try:
                client._get_organization(org_name).delete()
            except requests.HTTPError as e:
                msg = "{} failed to delete {} ({})".format(
                    client._conn.email,
                    org_name,
                    e.response.status_code,
                )
            else:
                msg = "{} deleted {}".format(
                    client._conn.email,
                    org_name,
                )

            print(msg)


def main():
    clients = get_clients()

    delete_builds(clients)
    delete_endpoints(clients)

    delete_orgs(clients)


if __name__ == "__main__":
    main()
