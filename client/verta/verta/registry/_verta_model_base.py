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
        :meth:`RegisteredModel.create_standard_model()
        <verta.registry.entities.RegisteredModel.create_standard_model>`.

    Examples
    --------
    .. code-block:: python

        import pickle
        import numpy as np
        from verta.registry import VertaModelBase, verify_io
        from verta.environment import Python

        class Model(VertaModelBase):
            def __init__(self, artifacts):
                with open(artifacts["np_matrix"], "rb") as f:
                    self._transform = pickle.load(f)

            @verify_io
            def predict(self, input):
                input = np.array(input)

                return np.matmul(input, self._transform)

        model_ver = reg_model.create_standard_model(
            Model,
            environment=Python(["numpy"]),
            artifacts={"np_matrix": arr},
        )

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

            :meth:`predict` must be written to both recieve [1]_ and return
            [2]_ JSON-serializable objects (i.e. mostly basic Python types).

            For example to work with NumPy arrays, the `input` argument should
            be a Python :class:`list` that would then be passed to
            ``np.array()`` inside this function. The result must then be cast
            back to a :class:`list` before ``return``\ ing.

        Parameters
        ----------
        input : any JSON-compatible Python type
            Model input.

        Returns
        -------
        any JSON-compatible Python type
            Model output.

        References
        ----------
        .. [1] https://docs.python.org/3/library/json.html#json-to-py-table
        .. [2] https://docs.python.org/3/library/json.html#py-to-json-table

        """
        raise NotImplementedError

    def model_test(self):
        """Test a model's behavior for correctness.

        :meth:`test` does nothing by default. Implement this method—with any
        assertions you wish—to validate your model.

        This method is called

        - in :func:`verta.registry.test_model_build`
        - when a model build is completed in the Verta platform
        - when an endpoint is initializing in the Verta platform

        .. note::

            If using model data logging (e.g. :func:`verta.runtime.log`), any
            calls here to the model's :meth:`predict` method must be wrapped
            in a :class:`verta.runtime.context`. This incidentally also
            enables assertions for expected logs.

        Returns
        -------
        None
            Returned values will be unused and discarded.

        Raises
        ------
        Any
            Raised exceptions will be propagated.

        """
        pass
