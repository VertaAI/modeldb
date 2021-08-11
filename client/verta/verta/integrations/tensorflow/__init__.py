# -*- coding: utf-8 -*-
"""TensorFlow and TensorBoard integrations for automatic experiment run logging."""

from ...external import six

import numbers
import os

import tensorflow as tf  # pylint: disable=import-error
from tensorflow.core.framework.summary_pb2 import Summary  # pylint: disable=import-error, no-name-in-module
from tensorflow.core.util.event_pb2 import Event  # pylint: disable=import-error, no-name-in-module
from tensorflow.compat.v1 import summary  # pylint: disable=import-error
try:
    from tensorflow.estimator import SessionRunArgs
except ImportError:  # tensorflow<2.0
    from tensorflow.train import SessionRunArgs
try:
    from tensorflow.estimator import SessionRunHook
except ImportError:  # tensorflow<2.0
    from tensorflow.train import SessionRunHook

from ..._internal_utils import _utils


def _parse_summary_proto_str(proto_str):
    """
    Converts the serialized protobuf `SessionRunValues.results['summary']` into a `Message` object.

    """
    summary_msg = Summary()
    summary_msg.ParseFromString(proto_str)
    return summary_msg


class VertaHook(SessionRunHook):
    """
    TensorFlow Estimator hook that automates logging to Verta during model training.

    This hook logs loss during training.

    This hook has been verified to work with the TensorFlow 1.X API.

    .. versionadded:: 0.13.20

    Parameters
    ----------
    run : :class:`~verta.tracking.entities.ExperimentRun`
        Experiment Run tracking this model.
    every_n_steps : int, default 1000
        How often to log summary metrics.

    Examples
    --------
    .. code-block:: python

        from verta.integrations.tensorflow import VertaHook
        run = client.set_experiment_run()
        estimator.train(
            input_fn=train_input_fn,
            hooks=[VertaHook(run)],
        )

    """
    def __init__(self, run, every_n_steps=1000):
        self._summary = None
        self._every_n_steps = every_n_steps
        self._step = 0

        self.run = run

    def begin(self):
        if self._summary is None:
            self._summary = summary.merge_all()

    def before_run(self, run_context):
        self._step += 1
        return SessionRunArgs({"summary": self._summary})

    def after_run(self, run_context, run_values):
        if self._step % self._every_n_steps != 0:
            return

        summary_msg = _parse_summary_proto_str(run_values.results['summary'])

        for value in summary_msg.value:  # pylint: disable=no-member
            # TODO: support other value types
            if value.WhichOneof("value") == "simple_value":
                try:
                    self.run.log_observation(value.tag, value.simple_value)
                except:
                    pass  # don't halt execution


def _parse_event_proto_str(proto_str):
    """
    Converts the serialized Event protobufs from `tf.data.TFRecordDataset` into a `Message` object.

    """
    event_msg = Event()
    event_msg.ParseFromString(proto_str)
    return event_msg


def _get_events_filepaths(log_dir):
    return [
        os.path.join(log_dir, filename)
        for filename in os.listdir(log_dir)
        if os.path.isfile(os.path.join(log_dir, filename))
        and filename.startswith("events.out.tfevents.")
    ]


def _collect_observations(events_filepath, prefix=None):
    """
    Collects scalars from a TensorBoard-compatible Events file as their values and timestamps.

    """
    if prefix is None:
        prefix = ''
    elif isinstance(prefix, six.string_types):
        prefix = prefix + '_'
    else:
        raise TypeError("`prefix` must be str")

    tf_major_version = int(tf.__version__.split('.')[0])  # pylint: disable=no-member
    if tf_major_version == 1 or not tf.executing_eagerly():
        events = tf.compat.v1.train.summary_iterator(events_filepath)
    else:
        events = (
            _parse_event_proto_str(tensor.numpy())
            for tensor in tf.data.TFRecordDataset(events_filepath)
        )

    observations = []
    for event in events:
        timestamp = _utils.ensure_timestamp(event.wall_time)
        if event.WhichOneof('what') == "summary":
            for value in event.summary.value:
                # TODO: support other value types
                if value.WhichOneof('value') == "simple_value":
                    observations.append({
                        'timestamp': timestamp,
                        'attribute': {
                            'key': prefix + value.tag,
                            'value': value.simple_value,
                        },
                    })
                elif (value.WhichOneof('value') == "tensor"
                        and tf.executing_eagerly()):
                    np_value = tf.make_ndarray(value.tensor)
                    if (np_value.size == 1  # scalar tensors can be logged
                            and isinstance(np_value.item(), numbers.Real)):
                        observations.append({
                            'timestamp': timestamp,
                            'attribute': {
                                'key': prefix + value.tag,
                                'value': np_value.item(),
                            },
                        })

    return observations


def log_tensorboard_events(run, log_dir):
    """
    Function that collects and logs TensorBoard-compatible events to an Experiment Run.

    This integration logs scalars that have been written as TensorFlow summaries.

    This integration has been verified to work with TensorFlow >=1.14 and 2.X.

    See our `GitHub repository
    <https://github.com/VertaAI/modeldb/blob/master/client/workflows/examples/tensorboard-integration.ipynb>`__
    for an example of this intergation in action.

    Parameters
    ----------
    run : :class:`~verta.tracking.entities.ExperimentRun`
        Experiment Run.
    log_dir : str
        Directory containing TensorBoard-compatible event files.

    Examples
    --------
    .. code-block:: python

        from verta.integrations.tensorflow import log_tensorboard_log_dir
        run = client.set_experiment_run()
        # log summary event files to `log_dir` during model operations
        # see https://www.tensorflow.org/tensorboard/get_started
        log_tensorboard_events(run, log_dir)

    """
    observations = []
    for events_filepath in _get_events_filepaths(log_dir):
        event_observations = _collect_observations(events_filepath)
        observations.extend(event_observations)
    for subdir in ('train', 'validation'):  # TensorFlow 2.X puts events in subdirs
        if subdir in os.listdir(log_dir) and os.path.isdir(os.path.join(log_dir, subdir)):
            for events_filepath in _get_events_filepaths(os.path.join(log_dir, subdir)):
                event_observations = _collect_observations(events_filepath, prefix=subdir)
                observations.extend(event_observations)

    # TODO: implement `run.log_observations()` instead of this
    _utils.make_request(
        "POST",
        "{}://{}/api/v1/modeldb/experiment-run/logObservations".format(run._conn.scheme, run._conn.socket),
        run._conn,
        json={
            'id': run.id,
            'observations': observations,
        },
    )
    run._clear_cache()
