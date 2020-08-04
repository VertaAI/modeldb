import pytest

from verta.deployment.resources import CpuMilli, Memory


@pytest.mark.parametrize("data", [3, 64])
def test_cpu_milli(data):
    CpuMilli(data)


@pytest.mark.parametrize("data", [-12, 0])
def test_cpu_milli_negative(data):
    with pytest.raises(ValueError):
        CpuMilli(data)


@pytest.mark.parametrize("data", ["T", 0.5])
def test_cpu_milli_negative_type(data):
    with pytest.raises(TypeError):
        CpuMilli(data)


@pytest.mark.parametrize("data", ['128974848', '129e6', '129M', '123Mi'])
def test_memory(data):
    Memory(data)


@pytest.mark.parametrize("data", ['12M3M', 'e12M3M', 'G', '123e6.3Gi', '123.3', '-5'])
def test_memory_negative(data):
    with pytest.raises(ValueError):
        Memory(data)

@pytest.mark.parametrize("data", [12.2, 4])
def test_memory_negative(data):
    with pytest.raises(TypeError):
        Memory(data)
