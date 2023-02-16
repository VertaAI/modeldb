import json

from verta.deserialization.wrappers.config.wrapper_config import WrapperConfig


# from dotenv import load_dotenv

# load_dotenv()


class EnvWrapperConfig(WrapperConfig):
    def __init__(self, config_file=None):
        if config_file:
            with open(config_file) as f:
                config_file_dict = json.load(f)
        else:
            config_file_dict = {}

        model_filename = config_file_dict.get('MODEL_FILENAME', '/app/model.pkl')
        model_type = config_file_dict.get('MODEL_TYPE', 'sklearn')
        deserialization = config_file_dict.get('DESERIALIZATION', 'cloudpickle')
        super().__init__(model_filename, model_type, deserialization)
