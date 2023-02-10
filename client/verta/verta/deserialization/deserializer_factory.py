from verta.deserialization.cloudpickle_deserializer import CloudpickleDeserializer
from verta.deserialization.joblib_deserialzer import JoblibDeserializer
from verta.deserialization.pickle_deserializer import PickleDeserializer
from verta.deserialization.keras_deserializer import KerasDeserializer

def make_deserializer(deserializer_type):
    deserializers = (
        JoblibDeserializer,
        PickleDeserializer,
        CloudpickleDeserializer,
        KerasDeserializer,
    )

    deserializer_classes = {deserializer.deserializer_type(): deserializer for deserializer in deserializers}

    return deserializer_classes[deserializer_type]()
