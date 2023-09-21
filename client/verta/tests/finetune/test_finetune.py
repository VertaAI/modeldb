# -*- coding: utf-8 -*-

"""Test RegisteredModelVersion.finetune() and its downstream effects."""

from verta.dataset import Path
from verta import finetune


def test_finetune(client, registered_model, dataset):
    """Verify that happy-path ``finetune()`` works."""
    base_model_ver = registered_model.create_version()  # mocked base LLM RMV
    name = "v1"

    reg_model = client.create_registered_model()
    train_dataset_version = dataset.create_version(
        Path(__file__, enable_mdb_versioning=True),
    )
    model_ver = base_model_ver.finetune(
        destination_registered_model=reg_model,
        train_dataset=train_dataset_version,
        name=name,
    )
    run = client.get_experiment_run(id=model_ver.experiment_run_id)

    # check entity names
    assert client.proj.name == reg_model.name + finetune._PROJECT_NAME_SUFFIX
    assert client.expt.name == finetune._EXPERIMENT_NAME_PREFIX + dataset.name
    # TODO: wait for fine-tuning to launch, then check ER name
    assert model_ver.name == name

    # check dataset association
    for entity in [model_ver, run]:
        for key, value in [
            (finetune._TRAIN_DATASET_NAME, train_dataset_version),
            # TODO: eval and test, too
        ]:
            assert entity.get_dataset_version(key).id == value.id

    # check attributes
    for entity in [model_ver, run]:
        assert (
            entity.get_attributes().items()
            >= {
                finetune._FINETUNE_ATTR_KEY: True,
            }.items()
        )
