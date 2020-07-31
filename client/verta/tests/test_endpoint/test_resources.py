import pytest

from verta.deployment.resources import CpuMilli, Memory


def test_cpu_milli():
    CpuMilli(3)
    with pytest.raises(ValueError):
        CpuMilli(0)
    with pytest.raises(ValueError):
        CpuMilli(-12)
    with pytest.raises(ValueError):
        CpuMilli("T")


def test_memory():
    for data in ['128974848', '129e6', '129M', '123Mi']:
        Memory(data)

def test_memory_negative():
    for data in ['12M3M', 'e12M3M', 'G', '123e6.3Gi', '123.3']:
        with pytest.raises(ValueError):
            Memory(data)
