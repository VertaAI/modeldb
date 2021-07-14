# -*- coding: utf-8 -*-

from verta.external import six


class KafkaSettings(object):
    """
    A set of topics to be used with deployed endpoints.

    Attributes
    ----------
    input_topic : str
        The input topic for an endpoint to subscribe to.
    output_topic : str
        The output topic for an endpoint to write predictions to.
    error_topic : str
        The error topic for an endpoint to write errors to.

    Examples
    --------
    .. code-block:: python
        from verta.deployment import KafkaSettings

        kafka_settings = KafkaSettings("my_input_data", "my_predictions", "my_endpoint_errors")
    """

    def __init__(self, input_topic, output_topic, error_topic):
        if input_topic == output_topic or input_topic == error_topic:
            raise ValueError(
                "input_topic must not be equal to either the output or error topics"
            )

        self._input_topic = self._check_non_empty_str("input_topic", input_topic)
        self._output_topic = self._check_non_empty_str("output_topic", output_topic)
        self._error_topic = self._check_non_empty_str("error_topic", error_topic)

    def __repr__(self):
        return "KafkaSettings({}, {}, {})".format(
            repr(self.input_topic), repr(self.output_topic), repr(self.error_topic)
        )

    @property
    def input_topic(self):
        return self._input_topic

    @property
    def output_topic(self):
        return self._output_topic

    @property
    def error_topic(self):
        return self._error_topic

    @staticmethod
    def _check_non_empty_str(name, value):
        if not isinstance(value, six.string_types):
            raise TypeError("`value` must be a string, not {}".format(type(value)))
        if not value:
            raise ValueError("`name` must be a non-empty string".format(name))
        return value
