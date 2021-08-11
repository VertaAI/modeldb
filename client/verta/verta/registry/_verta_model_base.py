# -*- coding: utf-8 -*-

import abc

from verta.external import six


@six.add_metaclass(abc.ABCMeta)
class VertaModelBase(object):
    """Abstract base class for Verta Standard Models.

    .. note::

        ``__init__()`` and :meth:`predict` **must** be implemented by
        subclasses.

    Parameters
    ----------
    artifacts : dict of str to str
        A mapping of artifact keys to filepaths. This will be provided to the
        deployed model based on artifact keys specified through
        :meth:`RegisteredModelVersion.log_model()
        <verta.registry.entities.RegisteredModelVersion.log_model>`.

    Examples
    --------
    .. code-block:: python

        import pickle
        import numpy as np
        from verta.registry import VertaModelBase

        class Model(VertaModelBase):
            def __init__(self, artifacts):
                with open(artifacts["np_matrix"], "rb") as f:
                    self._transform = pickle.load(f)

            def predict(self, input):
                input = np.array(input)

                return np.matmul(input, self._transform)

        # iterate locally
        model = Model(
            artifacts=model_ver.fetch_artifacts(["np_matrix"]),
        )

        # persist to model version
        model_ver.log_model(Model, artifacts=["np_matrix"])

    """

    @abc.abstractmethod
    def __init__(self, artifacts):
        raise NotImplementedError

    @abc.abstractmethod
    def predict(self, input):
        """Produce an output from `input`.

        This method is called when requests are made against a Verta endpoint.

        .. note::

            **It is recommended** to use the :func:`~verta.registry.verify_io`
            decorator to help ensure that your model's input and output types
            will be fully compatible with the Verta platform as you iterate
            locally.

            Specifically, this method should be written in a way that expects
            the parameter `input` to be a type returned from ``json.loads()``,
            i.e. a basic Python type. `input` would then need to be manually
            cast to a NumPy array, pandas DataFrame, etc. in order to be used
            as such.

            Similarly, the return value should also be a type that can be
            passed to ``json.dumps()``

        Parameters
        ----------
        input : any JSON-compatible Python type
            Model input.

        Returns
        -------
        any JSON-compatible Python type
            Model output.

        """
        raise NotImplementedError
