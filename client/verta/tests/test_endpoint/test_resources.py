import pytest

from verta.endpoint.resources import Resources
from verta.endpoint.update import DirectUpdateStrategy

pytestmark = pytest.mark.not_oss  # skip if run in oss setup. Applied to entire module


@pytest.mark.parametrize("data, strategy", [(3, DirectUpdateStrategy()), (64, None)])
def test_download_endpoint_manifest(client, data, strategy, in_tempdir):
    resources = Resources(cpu=data)

    # test that `resources` can actually be used
    client.download_endpoint_manifest(
        "deployment.yaml", "/production-prediction", "production-prediction",
        strategy=strategy,
        resources=resources,
    )


@pytest.mark.parametrize("data", [3, 64, 0.25])
def test_cpu_milli(client, data, in_tempdir):
    Resources(cpu=data)


@pytest.mark.parametrize("data", [-12, 0])
def test_cpu_milli_negative(data):
    with pytest.raises(ValueError):
        Resources(cpu=data)


@pytest.mark.parametrize("data", ["T", "0.5"])
def test_cpu_milli_negative_type(data):
    with pytest.raises(TypeError):
        Resources(cpu=data)


@pytest.mark.parametrize("data", ['128974848', '129e6', '129M', '123Mi'])
def test_memory(client, data, in_tempdir):
    resources = Resources(memory=data)

    # test that `resources` can acutally be used
    client.download_endpoint_manifest(
        "deployment.yaml", "/production-prediction", "production-prediction",
        strategy=DirectUpdateStrategy(),
        resources=resources,
    )


@pytest.mark.parametrize("data", ['12M3M', 'e12M3M', 'G', '123e6.3Gi', '123.3', '-5'])
def test_memory_negative(data):
    with pytest.raises(ValueError):
        Resources(memory=data)

@pytest.mark.parametrize("data", [12.2, 4])
def test_memory_negative_type(data):
    with pytest.raises(TypeError):
        Resources(memory=data)
