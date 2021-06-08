# -*- coding: utf-8 -*-

import abc

from verta.external import six


@six.add_metaclass(abc.ABCMeta)
class VertaModelBase(object):
    """Abstract base class for Verta Standard Models.

    .. note::

        ``__init__()`` and :meth:`predict` **must** be implemented by
        subclasses. All other methods are optional.

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

            This method should expect to recieve and return values that are
            JSON-compatible (most Python built-in types) to ensure
            interoperability with Verta deployment.

        Parameters
        ----------
        input : any JSON-compatible type
            Model input.

        Returns
        -------
        any JSON-compatible type
            Model output.

        """
        raise NotImplementedError

    def describe(self):
        """Return a rich description of the model's behavior for use by the model sandbox.

        Returns
        -------
        dict of str to str
            A mapping where values are textual descriptions, and keys are of
            the following values:

            - method
            - args
            - returns
            - description
            - input_description
            - output_description

        """
        raise NotImplementedError

    def example(self):
        """Return example input data for :meth:`predict` for use by the model sandbox.

        Returns
        -------
        any JSON-compatible type
            Example model input

        Examples
        --------
        .. code-block:: python

            output = model.predict(
                input=model.example(),
            )

        """
        raise NotImplementedError
