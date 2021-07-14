# -*- coding: utf-8 -*-


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

        self._input_topic = input_topic
        self._output_topic = output_topic
        self._error_topic = error_topic

    @property
    def input_topic(self):
        return self._input_topic

    @property
    def output_topic(self):
        return self._output_topic

    @property
    def error_topic(self):
        return self._error_topic
