import json

# from dotenv import load_dotenv

# load_dotenv()


class WrapperConfig:
    def __init__(self, model_filename, model_type, deserialization):
        self.MODEL_FILENAME = model_filename
        self.MODEL_TYPE = model_type
        self.DESERIALIZATION = deserialization
