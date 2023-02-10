import abc

from verta.deserialization.six import _six


@_six.add_metaclass(abc.ABCMeta)
class ABCModelWrapper:
    @abc.abstractmethod
    def predict(self, data):
        pass

    def process_input(self, inp):
        return inp

    def process_input_recursive(self, inputs):
        if isinstance(inputs, dict):
            return {k: self.process_input_recursive(v) for k,v in inputs.items()}

        if isinstance(inputs, str):
            return inputs

        try:
            iter(inputs)
        except TypeError:
            pass
        else:
            try:
                # Try to process from the wrapper (to convert to a tensor) and, if it fails, process as a list
                return self.process_input(inputs)
            except:
                return [self.process_input_recursive(v) for v in inputs]

        return self.process_input(inputs)

    @staticmethod
    @abc.abstractmethod
    def model_type():
        # type: (...) -> str
        pass

    def run(self, data):
        return self.clean_prediction(self.predict(self.process_input_recursive(data)))

    def clean_prediction(self, predicted):
        return self.to_builtin(predicted)

    def to_builtin(self, obj):
        """
        Tries to coerce `obj` into a built-in type, for JSON serialization.
        Parameters
        ----------
        obj
        Returns
        -------
        object
            A built-in equivalent of `obj`, or `obj` unchanged if it could not be handled by this function.
        """
        # jump through ludicrous hoops to avoid having hard dependencies in the Client
        cls_ = obj.__class__
        obj_class = getattr(cls_, '__name__', None)
        obj_module = getattr(cls_, '__module__', None)

        if hasattr(obj, "to_json"):
            return obj.to_json()

        # NumPy scalars
        if obj_module == "numpy" and obj_class.startswith(('int', 'uint', 'float', 'str')):
            return obj.item()

        # scientific library collections
        if obj_class == "ndarray":
            return obj.tolist()
        if obj_class == "Series":
            return obj.values.tolist()
        if obj_class == "DataFrame":
            return obj.values.tolist()
        if obj_class == "Tensor" and obj_module == "torch":
            return obj.numpy().tolist()

        # strings
        if isinstance(obj, _six.string_types):  # prevent infinite loop with iter
            return obj
        if isinstance(obj, _six.binary_type):
            return _six.ensure_str(obj)

        # dicts and lists
        if isinstance(obj, dict):
            return {self.to_builtin(key): self.to_builtin(val) for key, val in obj.items()}
        try:
            iter(obj)
        except TypeError:
            pass
        else:
            return [self.to_builtin(val) for val in obj]

        return obj
