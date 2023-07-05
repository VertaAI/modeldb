import functools
import json
import os
import warnings

import jsonschema

# for use like: `if getattr(model.predict, _DECORATED_FLAG, False)`
_VALIDATE_DECORATED_FLAG = "_verta_validate_schema"
_MODEL_SCHEMA_PATH_ENV_VAR = "VERTA_MODEL_SCHEMA_PATH"


def validate_schema(f):
    """Decorator to validate prediction input against previously provided schema."""

    @functools.wraps(f)
    def wrapper(self, *args, **kwargs):
        # accepts dict input
        # TODO: validate length of args and type
        # Fetch schema
        prediction_input = args[0]
        model_schema_path = os.environ.get(_MODEL_SCHEMA_PATH_ENV_VAR, "/app/model_schema.json")
        try:
            with open(model_schema_path, "r") as file:
                # Load the JSON data into a variable
                schema = json.load(file)
        except FileNotFoundError as e:
            raise FileNotFoundError(
                "no schema found for model. Did you remember to `set_schema`?"
            ) from e
        input_schema = schema["input"]
        output_schema = schema["output"]

        # Validate input
        try:
            jsonschema.validate(instance=prediction_input, schema=input_schema)
        except jsonschema.exceptions.ValidationError as e:
            raise jsonschema.exceptions.ValidationError(
                "input failed schema validation"
            ) from e

        # Run function and validate output
        output = f(self, *args, **kwargs)
        try:
            jsonschema.validate(instance=output, schema=output_schema)
        except jsonschema.exceptions.ValidationError as e:
            warnings.warn("output failed schema validation: " + str(e))

        return output

    setattr(wrapper, _VALIDATE_DECORATED_FLAG, True)
    return wrapper
