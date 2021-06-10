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

            To ensure interoperability with Verta deployment, this method
            should expect the parameter `input` to be one of the following types:

            - ``int``
            - ``float``
            - ``bool``
            - ``str``
            - ``NoneType``
            - ``list``
            - ``dict``
            - nested ``dict``/``list`` of the above

            `input` should be manually cast to a NumPy array, pandas DataFrame,
            etc. in order to be used as such.

            The value returned by this method should also be converted to one
            of the above types.

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
