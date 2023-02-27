"""
Tests attributes like `proj.experiments` and `dataset.versions` in non-default workspaces.

"""
import os

import pytest

from verta.dataset import Path
from verta._internal_utils import _utils


class TestClient:
    """Attributes on client."""

    def test_projects(self, client, workspace, created_entities):
        client.set_workspace(workspace.name)

        proj_ids = set()
        for _ in range(2):
            proj = client.create_project()
            created_entities.append(proj)
            proj_ids.add(proj.id)

        assert set(proj.id for proj in client.projects) == proj_ids

    def test_experiments(self, client, workspace, created_entities):
        client.set_workspace(workspace.name)

        expt_ids = set()
        for _ in range(2):
            proj = client.create_project()
            created_entities.append(proj)
            for _ in range(2):
                expt_ids.add(client.create_experiment().id)

        assert set(expt.id for expt in client.experiments) == expt_ids

    def test_expt_runs(self, client, workspace, created_entities):
        client.set_workspace(workspace.name)

        run_ids = set()
        for _ in range(2):
            proj = client.create_project()
            created_entities.append(proj)
            for _ in range(2):
                client.create_experiment()
                for _ in range(2):
                    run_ids.add(client.create_experiment_run().id)

        assert set(run.id for run in client.expt_runs) == run_ids

    def test_registered_models(self, client, workspace, created_entities):
        client.set_workspace(workspace.name)

        reg_model_ids = set()
        for _ in range(2):
            reg_model = client.create_registered_model()
            created_entities.append(reg_model)
            reg_model_ids.add(reg_model.id)

        assert (
            set(reg_model.id for reg_model in client.registered_models) == reg_model_ids
        )

    def test_registered_model_versions(self, client, workspace, created_entities):
        client.set_workspace(workspace.name)

        model_ver_ids = set()
        for _ in range(2):
            reg_model = client.create_registered_model()
            created_entities.append(reg_model)
            for _ in range(2):
                model_ver_ids.add(reg_model.create_version().id)

        assert (
            set(model_ver.id for model_ver in client.registered_model_versions)
            == model_ver_ids
        )

    @pytest.mark.deployment
    def test_endpoints(self, client, workspace, created_entities):
        client.set_workspace(workspace.name)

        endpoint_ids = set()
        for _ in range(2):
            endpoint = client.create_endpoint(_utils.generate_default_name())
            created_entities.append(endpoint)
            endpoint_ids.add(endpoint.id)

        assert set(endpoint.id for endpoint in client.endpoints) == endpoint_ids

    def test_datasets(self, client, workspace, created_entities):
        client.set_workspace(workspace.name)

        dataset_ids = set()
        for _ in range(2):
            dataset = client.create_dataset()
            created_entities.append(dataset)
            dataset_ids.add(dataset.id)

        assert set(dataset.id for dataset in client.datasets) == dataset_ids

    @pytest.mark.skip(reason="`client.dataset_versions` does not exist")
    def test_dataset_versions(self, client, workspace, created_entities):
        raise NotImplementedError

    @pytest.mark.skip(reason="`client.repositories` does not exist")
    def test_repositories(self, client, workspace, created_entities):
        raise NotImplementedError


class TestEntitites:
    """Attributes on entity objects."""

    def test_proj_expts(self, client, workspace, created_entities):
        proj = client.create_project(workspace=workspace.name)
        created_entities.append(proj)

        expt_ids = {client.create_experiment().id for _ in range(2)}

        assert set(expt.id for expt in proj.experiments) == expt_ids

    def test_proj_runs(self, client, workspace, created_entities):
        proj = client.create_project(workspace=workspace.name)
        created_entities.append(proj)

        run_ids = set()
        for _ in range(2):
            client.create_experiment()
            run_ids.update({client.create_experiment_run().id for _ in range(2)})

        assert set(run.id for run in proj.expt_runs) == run_ids

    def test_expt_runs(self, client, workspace, created_entities):
        proj = client.create_project(workspace=workspace.name)
        created_entities.append(proj)
        expt = client.create_experiment()

        run_ids = {client.create_experiment_run().id for _ in range(2)}

        assert set(run.id for run in expt.expt_runs) == run_ids

    def test_dataset_versions(self, client, workspace, created_entities):
        dataset = client.create_dataset(workspace=workspace.name)
        created_entities.append(dataset)

        version_ids = {
            dataset.create_version(Path(filename)).id
            for filename in os.listdir(".")[:2]
        }

        assert set(version.id for version in dataset.versions) == version_ids

    def test_model_versions(self, client, workspace, created_entities):
        model = client.create_registered_model(workspace=workspace.name)
        created_entities.append(model)

        version_ids = {model.create_version().id for filename in os.listdir(".")[:2]}

        assert set(version.id for version in model.versions) == version_ids
