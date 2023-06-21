# -*- coding: utf-8 -*-

from verta._vendored import six


class KafkaSettings(object):
    """
    A set of Kafka topics to be used with deployed endpoints.

    For use during :class:`~verta.endpoint.Endpoint` creation or with
    :meth:`Endpoint.update() <verta.endpoint.Endpoint.update>`.

    Search for available Kafka topics with :meth:`Client.get_kafka_topics() <verta.Client.get_kafka_topics>`.

    .. versionchanged:: 0.23.0
        If ``cluster_config_id`` is not provided, it is fetched from
        the active Kafka configuration, assuming only one config exists.
    .. versionadded:: 0.19.0

    Attributes
    ----------
    input_topic : str
        The input topic for an endpoint to subscribe to.
    output_topic : str
        The output topic for an endpoint to write predictions to.
    error_topic : str
        The error topic for an endpoint to write errors to.
    cluster_config_id: str, optional
        The ID of the current Kafka configuration.  Fetched from the
        current config by default.

    Examples
    --------
    .. code-block:: python

        from verta.endpoint import KafkaSettings

        kafka_settings = KafkaSettings(
            input_topic="my_input_data",
            output_topic="my_predictions",
            error_topic="my_endpoint_errors",
        )

        endpoint = client.create_endpoint(path="/my-endpoint", kafka_settings=kafka_settings)
        print(endpoint.kafka_settings)

    """

    def __init__(self, input_topic, output_topic, error_topic, cluster_config_id=None):
        if input_topic == output_topic or input_topic == error_topic:
            raise ValueError(
                "input_topic must not be equal to either the output or error topics"
            )

        self._input_topic = self._check_non_empty_str("input_topic", input_topic)
        self._output_topic = self._check_non_empty_str("output_topic", output_topic)
        self._error_topic = self._check_non_empty_str("error_topic", error_topic)
        self._cluster_config_id = (
            self._check_non_empty_str("cluster_config_id", cluster_config_id)
            if cluster_config_id
            else None
        )

    def __eq__(self, other):
        if not isinstance(other, type(self)):
            return NotImplemented

        return (
            self._input_topic == other._input_topic
            and self._output_topic == other._output_topic
            and self._error_topic == other._error_topic
            and self._cluster_config_id == other._cluster_config_id
        )

    def __repr__(self):
        return "KafkaSettings({}, {}, {}, {})".format(
            repr(self.input_topic),
            repr(self.output_topic),
            repr(self.error_topic),
            repr(self.cluster_config_id),
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

    @property
    def cluster_config_id(self):
        return self._cluster_config_id

    @staticmethod
    def _check_non_empty_str(name, value):
        if not isinstance(value, six.string_types):
            raise TypeError("`value` must be a string, not {}".format(type(value)))
        if not value:
            raise ValueError("`value` must be a non-empty string")
        return value

    def _as_dict(self):
        return {
            "disabled": False,
            "input_topic": self.input_topic,
            "output_topic": self.output_topic,
            "error_topic": self.error_topic,
            "cluster_config_id": self.cluster_config_id,
        }

    @classmethod
    def _from_dict(cls, d):
        # NOTE: ignores extraneous keys in `d`
        try:
            input_topic = d["input_topic"]
            output_topic = d["output_topic"]
            error_topic = d["error_topic"]
            cluster_config_id = d["cluster_config_id"]
        except KeyError as e:
            msg = 'expected but did not find key "{}"'.format(e.args[0])
            six.raise_from(RuntimeError(msg), None)

        return cls(input_topic, output_topic, error_topic, cluster_config_id)
