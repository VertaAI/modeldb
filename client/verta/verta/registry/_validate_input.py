import functools
import json
import os

import jsonschema

# for use like: `if getattr(model.predict, _DECORATED_FLAG, False)`
_DECORATED_FLAG = "_verta_validate_input"
_MODEL_SCHEMA_PATH = os.environ.get("VERTA_MODEL_SCHEMA_PATH", "/app/model_schema.json")


def validate_input(f):
    """Decorator to validate prediction input against previously provided schema."""

    @functools.wraps(f)
    def wrapper(self, *args, **kwargs):
        # accepts dict input
        # TODO: validate length of args and type
        prediction_input = args[0]
        try:
            with open(_MODEL_SCHEMA_PATH, "r") as file:
                # Load the JSON data into a variable
                schema = json.load(file)
                input_schema = schema["input"]
                jsonschema.validate(instance=prediction_input, schema=input_schema)

            output = f(self, *args, **kwargs)
            return output
        except FileNotFoundError:
            print("No schema found for model. Did you remember to `set_schema`?")

    setattr(wrapper, _DECORATED_FLAG, True)
    return wrapper
