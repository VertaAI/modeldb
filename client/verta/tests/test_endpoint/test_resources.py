import pytest

from verta.endpoint.resources import Resources


@pytest.mark.parametrize("data", [3, 64, 0.25])
def test_cpu_milli(data):
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
def test_memory(data):
    Resources(memory=data)


@pytest.mark.parametrize("data", ['12M3M', 'e12M3M', 'G', '123e6.3Gi', '123.3', '-5'])
def test_memory_negative(data):
    with pytest.raises(ValueError):
        Resources(memory=data)

@pytest.mark.parametrize("data", [12.2, 4])
def test_memory_negative_type(data):
    with pytest.raises(TypeError):
        Resources(memory=data)
