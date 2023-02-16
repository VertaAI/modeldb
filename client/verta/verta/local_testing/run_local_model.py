import os
import sys

from verta.deserialization.wrappers.make_model_wrapper import make_model_wrapper
from verta.deserialization.wrappers.config.wrapper_config import WrapperConfig


def main(args):
    model_filename = args[0]
    model_type = args[1]
    deserialization = args[2]
    config = WrapperConfig(model_filename, model_type, deserialization)
    model_wrapper = make_model_wrapper(config)
    inputs = ["hi", "hello", "bye"]
    inputs = model_wrapper.process_input(inputs)
    predicted = model_wrapper.predict(inputs)
    print(predicted)


if __name__ == "__main__":
    args = sys.argv[1:]
    main(args)
