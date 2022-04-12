# -*- coding: utf-8 -*-

import hypothesis
import hypothesis.strategies as st
import pytest

from verta.endpoint import KafkaSettings
from verta.environment import Python
from verta._internal_utils import _utils


class TestKafkaSettings:

    @hypothesis.given(
        input_topic=st.text(min_size=1),
        output_topic=st.text(min_size=1),
        error_topic=st.text(min_size=1),
        cluster_config_id=st.text(min_size=1),
    )
    def test_kafka_settings(self, input_topic, output_topic, error_topic, cluster_config_id):
        hypothesis.assume(input_topic != output_topic and input_topic != error_topic)

        kafka_settings = KafkaSettings(input_topic, output_topic, error_topic, cluster_config_id)
        assert kafka_settings.cluster_config_id == cluster_config_id
        assert kafka_settings.input_topic == input_topic
        assert kafka_settings.output_topic == output_topic
        assert kafka_settings.error_topic == error_topic

        assert kafka_settings == KafkaSettings._from_dict({
            "cluster_config_id": cluster_config_id,
            "input_topic": input_topic,
            "output_topic": output_topic,
            "error_topic": error_topic,
        })

        assert kafka_settings._as_dict() == {
            "disabled": False,
            "cluster_config_id": cluster_config_id,
            "input_topic": input_topic,
            "output_topic": output_topic,
            "error_topic": error_topic,
        }

    @hypothesis.given(
        input_topic=st.text(min_size=1),
        other_topic=st.text(min_size=1),
        cluster_config_id=st.text(min_size=1),
    )
    def test_different_topics(self, cluster_config_id, input_topic, other_topic):
        hypothesis.assume(input_topic != other_topic)

        with pytest.raises(ValueError, match="input_topic must not be equal to either the output or error topics"):
            KafkaSettings(cluster_config_id, input_topic, input_topic, other_topic)

        with pytest.raises(ValueError, match="input_topic must not be equal to either the output or error topics"):
            KafkaSettings(cluster_config_id, input_topic, other_topic, input_topic)

    @hypothesis.given(
        input_topic=st.text(),
        output_topic=st.text(),
        error_topic=st.text(),
        cluster_config_id=st.text(),
    )
    def test_nonempty_topics(self, input_topic, output_topic, error_topic, cluster_config_id):
        hypothesis.assume(input_topic != output_topic and input_topic != error_topic)
        hypothesis.assume(any(len(string_value) == 0 for string_value in (input_topic, output_topic, error_topic, cluster_config_id)))

        with pytest.raises(ValueError, match="must be a non-empty string"):
            KafkaSettings(input_topic, output_topic, error_topic, cluster_config_id)


@pytest.mark.deployment
class TestConfigureEndpoint:
    def test_configure_endpoint(self, client, model_version, strs):
        LogisticRegression = pytest.importorskip("sklearn.linear_model").LogisticRegression
        strs = iter(strs)

        model_version.log_model(
            LogisticRegression, custom_modules=[],
        )
        model_version.log_environment(Python(["scikit-learn"]))

        # create
        kafka_settings = KafkaSettings(next(strs), next(strs), next(strs), next(strs))
        endpoint = client.create_endpoint(_utils.generate_default_name(), kafka_settings=kafka_settings)
        assert endpoint.kafka_settings == kafka_settings

        # update
        kafka_settings = KafkaSettings(next(strs), next(strs), next(strs), next(strs))
        endpoint.update(model_version, kafka_settings=kafka_settings)
        assert endpoint.kafka_settings == kafka_settings

        # clear
        endpoint.update(model_version, kafka_settings=False)
        assert endpoint.kafka_settings is None
