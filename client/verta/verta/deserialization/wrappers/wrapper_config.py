import json

# from dotenv import load_dotenv

# load_dotenv()


class WrapperConfig:
    def __init__(self, config_file=None):
        if config_file:
            with open(config_file) as f:
                config_file_dict = json.load(f)
        else:
            config_file_dict = {}

        self.MODEL_FILENAME = config_file_dict.get('MODEL_FILENAME', '/app/model.pkl')
        self.MODEL_TYPE = config_file_dict.get('MODEL_TYPE', 'sklearn')
        self.DESERIALIZATION = config_file_dict.get('DESERIALIZATION', 'cloudpickle')