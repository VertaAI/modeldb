# -*- coding: utf-*-

from verta.environment import Python


def test_endpoint_get_current_build(model_version, endpoint, model_for_deployment):
    """Verify get_current_build() returns expected build."""
    model_version.log_model(model_for_deployment["model"], custom_modules=[])
    model_version.log_environment(Python([model_for_deployment["requirements"].read()]))

    build = endpoint._create_build(model_version)
    endpoint.update(build, wait=False)

    retrieved_build = endpoint.get_current_build()
    assert build.id == retrieved_build.id


def test_model_version_list_builds(model_version, endpoint, model_for_deployment):
    """Verify list_builds() returns expected builds."""
    model_version.log_model(model_for_deployment["model"], custom_modules=[])
    model_version.log_environment(Python([model_for_deployment["requirements"].read()]))

    builds = sorted(
        [endpoint._create_build(model_version) for _ in range(3)],
        key=lambda build: build.date_created,
        reverse=True,
    )

    retrieved_builds = model_version.list_builds()
    assert [b.id for b in builds] == [b.id for b in retrieved_builds]
