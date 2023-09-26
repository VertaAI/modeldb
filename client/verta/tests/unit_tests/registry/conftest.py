# -*- coding: utf-8 -*-

import calendar
import collections as collecs
import datetime as dt
import json
from typing import Type, Union

import click
import cloudpickle as cp
import jsonschema
import pytest
import pytimeparse
import requests
from requests import post
import urllib3
import yaml
from google.protobuf.message import Message

from verta import runtime
from verta.registry import verify_io, VertaModelBase


@pytest.fixture(scope="session")
def dependency_testing_model() -> Type[VertaModelBase]:
    """Returns a model class that imports and calls external dependencies."""

    class DependencyTestingModel(VertaModelBase):
        """
        Model class that imports and calls external dependencies in a variety of ways
        for the purpose of testing our model environment validation logic.
        """

        def __init__(self, artifacts):
            pass

        # Note that wrapping in verify_io may have an impact on how modules are
        # extracted, thus we explicitly test the same scenarios with and without.
        @verify_io
        def predict(
            self,
            w: calendar.Calendar,  # standard library in function arg
            x: dt.datetime,  # standard library in function arg via alias
            y: click.File,  # 3rd-party module in function arg
            z: Message,  # 3rd-party module in function arg via class
        ) -> cp.CloudPickler:  # 3rd-party module in return type hint
            hour = x.hour  # standard library usage in function body
            runtime.log("error", "Error")  # 3rd-party module in function body (VERTA)
            yaml_con = yaml.constructor  # 3rd party module in function body
            z = self.do_something(y)  # 3rd party module called indirectly
            return z

        def unwrapped_predict(
            self,
            a: json.JSONEncoder,  # standard library in function arg
            b: collecs.OrderedDict,  # standard library in function arg via alias
            c: yaml.SafeLoader,  # 3rd-party module in function arg
            d: cp.CloudPickler,  # 3rd-party module in function arg via alias
        ) -> requests.Timeout:  # 3rd-party module in return type hint
            _json = a.encode({"x": "y"})  # standard library usage in function body
            with runtime.context():  # 3rd-party module in function body (VERTA)
                runtime.log(
                    "error", "Error"
                )  # 3rd-party module in function body (VERTA)
            click_exc = click.ClickException  # 3rd party module in function body
            z = self.make_timeout()  # 3rd party module called indirectly
            return z

        def make_dataframe(self, input):  # No modules in function signature
            return pytimeparse.parse(input)  # 3rd party module in function body

        def make_message(self, input: str):
            msg = Message(input)
            return msg

        @staticmethod
        def make_timeout():  # No modules in function signature
            return requests.Timeout()  # 3rd party module in function body

        def post_request(self) -> None:
            post("https://www.verta.ai")

        # 3rd-party modules nested inside type constructs should still be extracted
        def nested_multiple_returns_hint(
            self,
        ) -> Union[urllib3.Retry, jsonschema.exceptions.ValidationError]:
            return urllib3.Retry or jsonschema.exceptions.ValidationError

        # 3rd-party modules nested inside type constructs should still be extracted
        def nested_type_hint(self) -> Type[jsonschema.TypeChecker]:
            return jsonschema.TypeChecker

    return DependencyTestingModel
