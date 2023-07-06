import functools
import json
import os
import warnings

import jsonschema

# for use like: `if getattr(model.predict, _DECORATED_FLAG, False)`
_VALIDATE_DECORATED_FLAG = "_verta_validate_schema"
_MODEL_SCHEMA_PATH_ENV_VAR = "VERTA_MODEL_SCHEMA_PATH"


def validate_schema(f):
    """Decorator to validate prediction input and output against previously provided schema.

    Validation is done with the ``jsonschema`` library [#]_. If no schema has been provided, an exception is raised.

    Note that an input schema is required but output is not. If the output schema was not set, then the output will
    not be validated.

    Examples
    --------
    .. code-block:: python
        from pydantic import BaseModel
        from verta.registry import validate_schema, VertaModelBase
        from verta import Client
        from verta.environment import Python

        class Input(BaseModel):
            a: int
            b: int

        class Output(BaseModel):
            c: int
            d: int

        class MyModel(VertaModelBase):
            def __init__(self, artifacts=None):
                pass

            @validate_schema
            def predict(self, input):
                return {'c': 3, 'd': 4}

        def main():
            client = Client()

            # Register
            model_ver = client.get_or_create_registered_model("My Model").create_standard_model(
                MyModel,
                environment=Python([]),
            )
            model_ver.set_schema(input=Input.schema(), output=Output.schema())

            # Deploy
            endpoint = client.get_or_create_endpoint("my-model")
            endpoint.update(model_ver, wait=True)
            deployed_model = endpoint.get_deployed_model()

            # succeeds; input matches previously provided schema
            input = Input(a=1, b=2)
            output = deployed_model.predict(input.dict())

            # fails; a list will be given to the deployed model as-is
            deployed_model.predict({"something": "random"})

    References
    ----------
    .. [#] https://python-jsonschema.readthedocs.io/en/stable/
    """

    @functools.wraps(f)
    def wrapper(self, *args, **kwargs):
        # accepts dict input
        # TODO: validate length of args and type
        # Fetch schema
        prediction_input = args[0]
        model_schema_path = os.environ.get(
            _MODEL_SCHEMA_PATH_ENV_VAR, "/app/model_schema.json"
        )
        try:
            with open(model_schema_path, "r") as file:
                # Load the JSON data into a variable
                schema = json.load(file)
        except FileNotFoundError as e:
            raise FileNotFoundError(
                "no schema found for model. Did you remember to `set_schema`?"
            ) from e
        input_schema = schema["input"]
        output_schema = None
        if "output" in schema:
            output_schema = schema["output"]

        # Validate input
        if not isinstance(prediction_input, dict):
            raise TypeError("input must be a dict. Did you remember to call `.dict()`?")
        try:
            jsonschema.validate(instance=prediction_input, schema=input_schema)
        except jsonschema.exceptions.ValidationError as e:
            raise jsonschema.exceptions.ValidationError(
                "input failed schema validation"
            ) from e

        # Run function
        output = f(self, *args, **kwargs)
        if output_schema is None:
            return output

        # Validate output
        if not isinstance(output, dict):
            raise TypeError(
                "output must be a dict. Did you remember to call `.dict()` in your model?"
            )
        try:
            jsonschema.validate(instance=output, schema=output_schema)
        except jsonschema.exceptions.ValidationError as e:
            warnings.warn("output failed schema validation: " + str(e))

        return output

    setattr(wrapper, _VALIDATE_DECORATED_FLAG, True)
    return wrapper
