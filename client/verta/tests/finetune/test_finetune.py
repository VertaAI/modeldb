# -*- coding: utf-8 -*-

"""Test RegisteredModelVersion.finetune() and its downstream effects."""

import itertools

import pytest

from verta.dataset import Path
from verta import finetune


class TestHappyPaths:
    def test_entity_names(self, client, make_registered_model, dataset):
        """Verify created entities' names are as expected."""
        base_model_ver = make_registered_model().create_version()  # mocked base LLM RMV
        name = "v1"
        reg_model = make_registered_model()
        train_dataset = dataset.create_version(
            Path(__file__, enable_mdb_versioning=True),
        )

        model_ver = base_model_ver.finetune(
            destination_registered_model=reg_model,
            train_dataset=train_dataset,
            name=name,
        )
        run = client.get_experiment_run(id=model_ver.experiment_run_id)

        # check entity names
        assert client.proj.name == reg_model.name + finetune._PROJECT_NAME_SUFFIX
        assert client.expt.name == finetune._EXPERIMENT_NAME_PREFIX + dataset.name
        # TODO: wait for fine-tuning to launch, then check ER name
        assert model_ver.name == name

    def test_datasets(self, client, make_registered_model, dataset):
        """Verify created entities' logged dataset versions are as expected."""
        base_model_ver = make_registered_model().create_version()  # mocked base LLM RMV
        destination_registered_model = make_registered_model()
        train_dataset = dataset.create_version(
            Path(__file__, enable_mdb_versioning=True),
        )
        eval_dataset = dataset.create_version(
            Path(__file__, enable_mdb_versioning=True),
        )
        test_dataset = dataset.create_version(
            Path(__file__, enable_mdb_versioning=True),
        )

        # get every relevant arrangement of dataset arguments
        possible_datasets = itertools.product(
            [train_dataset],
            [eval_dataset, None],
            [test_dataset, None],
        )

        for train_dataset, eval_dataset, test_dataset in possible_datasets:
            model_ver = base_model_ver.finetune(
                destination_registered_model=destination_registered_model,
                train_dataset=train_dataset,
                eval_dataset=eval_dataset,
                test_dataset=test_dataset,
            )
            run = client.get_experiment_run(id=model_ver.experiment_run_id)

            # check dataset association
            for entity in [model_ver, run]:
                for dataset_name, dataset_version in [
                    (finetune._TRAIN_DATASET_NAME, train_dataset),
                    (finetune._EVAL_DATASET_NAME, eval_dataset),
                    (finetune._TEST_DATASET_NAME, test_dataset),
                ]:
                    if dataset_version is not None:
                        assert (
                            entity.get_dataset_version(dataset_name).id
                            == dataset_version.id
                        )

    def test_attributes(self, client, make_registered_model, dataset):
        """Verify created entities' attributes are as expected."""
        base_model_ver = make_registered_model().create_version()  # mocked base LLM RMV
        reg_model = make_registered_model()
        train_dataset = dataset.create_version(
            Path(__file__, enable_mdb_versioning=True),
        )

        model_ver = base_model_ver.finetune(
            destination_registered_model=reg_model,
            train_dataset=train_dataset,
        )
        run = client.get_experiment_run(id=model_ver.experiment_run_id)

        # check attributes
        for entity in [model_ver, run]:
            assert (
                entity.get_attributes().items()
                >= {
                    finetune._FINETUNE_ATTR_KEY: True,
                }.items()
            )


class TestErrors:
    def test_dataset_not_mdb_versioned_error(self, make_registered_model, dataset):
        """Verify that using non-ModelDB-versioned datasets raises an exception."""
        base_model_ver = make_registered_model().create_version()  # mocked base LLM RMV
        destination_registered_model = make_registered_model()
        unversioned_dataset = dataset.create_version(
            Path(__file__),
        )
        versioned_dataset = dataset.create_version(
            Path(__file__, enable_mdb_versioning=True),
        )

        # get every relevant arrangement of dataset arguments
        possible_datasets = itertools.product(
            [unversioned_dataset, versioned_dataset, None],
            repeat=3,
        )
        possible_datasets = filter(  # `train_dataset` cannot be None
            lambda datasets: datasets[0] is not None,
            possible_datasets,
        )
        possible_datasets = filter(  # this test needs at least one unversioned
            lambda datasets: any(
                dataset is unversioned_dataset for dataset in datasets
            ),
            possible_datasets,
        )

        for train_dataset, eval_dataset, test_dataset in possible_datasets:
            with pytest.raises(
                ValueError,
                match=" must have ``enable_mdb_versioning=True`` on creation",
            ):
                base_model_ver.finetune(
                    destination_registered_model=destination_registered_model,
                    train_dataset=train_dataset,
                    eval_dataset=eval_dataset,
                    test_dataset=test_dataset,
                )
