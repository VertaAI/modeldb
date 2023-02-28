# -*- coding: utf-8 -*-

import calendar
import collections as collecs
import datetime as dt
import json
from typing import List, Type, Union

import click
import cloudpickle as cp
import pytest
import requests
import urllib3
import yaml
from google.protobuf import message as msg

from verta import runtime
from verta.registry import DockerImage, verify_io, VertaModelBase


@pytest.fixture(scope="session")
def docker_image():
    return DockerImage(
        port=5000,
        request_path="/predict_json",
        health_path="/health",
        repository="012345678901.dkr.ecr.apne2-az1.amazonaws.com/models/example",
        tag="example",
        env_vars={"CUDA_VISIBLE_DEVICES": "0,1"},
    )


@pytest.fixture(scope="session")
def dependency_testing_model() -> Type[VertaModelBase]:
    """Returns a model class that imports and calls external dependencies."""
    boto3 = pytest.importorskip("boto3")
    numpy = pytest.importorskip("numpy")
    spacy = pytest.importorskip("spacy")
    pd =pytest.importorskip("pandas")
    sklearn = pytest.importorskip("sklearn")
    torch = pytest.importorskip("torch")
    PIL = pytest.importorskip("PIL")

    class DependencyTestingModel(VertaModelBase):
        """
        Model class that imports and calls external dependencies in a variety of ways
        for the purpose of testing our model environment validation logic.
        """
        def __init__(self, artifacts):
            pass

        @staticmethod
        def expected_function_names() -> List[str]:
            return [
                '__init__',
                'expected_function_names',
                'expected_modules',
                'make_boto_session',
                'make_dataframe',
                'make_spacy_error',
                'make_timeout',
                'model_test',
                'nested_multiple_returns_hint',
                'nested_type_hint',
                'predict',
                'unwrapped_predict',
            ]

        @staticmethod
        def expected_modules() -> List[str]:
            return [
                'google.protobuf.message',
                'collections',
                'requests.exceptions',
                'urllib3',
                'PIL',
                'pandas.core.frame',
                'urllib3.util.retry',
                'sklearn.base',
                'click',
                'json.encoder',
                'builtins',
                'datetime',
                'requests',
                'torch',
                'yaml',
                'verta.runtime',
                'calendar',
                'numpy',
            ]

        # Note that wrapping in verify_io may have an impact on how modules are
        # extracted, thus we explicitly test the same scenarios with and without.
        @verify_io
        def predict(
                self,
                w: calendar.Calendar,            # standard library in function arg
                x: dt.datetime,                  # standard library in function arg via alias
                y: numpy.ndarray,                # 3rd-party module in function arg
                z: msg.Message,                  # 3rd-party module in function arg via alias
        ) -> pd.DataFrame:                       # 3rd-party module in return type hint
            hour = x.hour                        # standard library usage in function body
            runtime.log('error', 'Error')        # 3rd-party module in function body (VERTA)
            yaml_con = yaml.constructor          # 3rd party module in function body
            spacy_error: spacy.Errors = self.make_spacy_error()
                                                 # 3rd-party module in function body as type hint
            z = self.make_dataframe(y)           # 3rd party module called indirectly
            return z


        def unwrapped_predict(
                self,
                a: json.JSONEncoder,             # standard library in function arg
                b: collecs.OrderedDict,          # standard library in function arg via alias
                c: sklearn.base.BaseEstimator,   # 3rd-party module in function arg
                d: cp.CellType,                  # 3rd-party module in function arg via alias
        ) -> requests.Timeout:                   # 3rd-party module in return type hint
            _json = a.encode({'x':'y'})          # standard library usage in function body
            with runtime.context():              # 3rd-party module in function body (VERTA)
                runtime.log('error', 'Error')    # 3rd-party module in function body (VERTA)
            click_exc = click.ClickException     # 3rd party module in function body
            boto_sesh: boto3.Session = self.make_boto_session()
                                                 # 3rd-party module in function body as type hint
            z = self.make_timeout()              # 3rd party module called indirectly
            return z

        def make_dataframe(self, input):         # No modules in function signature
            return pd.DataFrame(input)           # 3rd party module in function body

        def make_timeout(self):                  # No modules in function signature
            return requests.Timeout()            # 3rd party module in function body

        def make_spacy_error(self):              # No modules in function signature
            return spacy.Errors                  # 3rd party module in function body

        @staticmethod
        def make_boto_session():                 # No modules in function signature
            return boto3.DEFAULT_SESSION         # 3rd-party module in function body

        # 3rd-party modules nested inside type constructs should still be extracted
        def nested_multiple_returns_hint(self) -> Union[urllib3.Retry, PIL.UnidentifiedImageError]:
            return urllib3.Retry or PIL.UnidentifiedImageError

        # 3rd-party modules nested inside type constructs should still be extracted
        def nested_type_hint(self) -> Type[torch.NoneType]:
            return torch.NoneType

    return DependencyTestingModel
