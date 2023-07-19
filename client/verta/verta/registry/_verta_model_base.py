# -*- coding: utf-8 -*-

import abc

from verta._vendored import six


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

            :meth:`predict` must be written to both receive [#]_ and return
            [#]_ JSON-serializable objects (i.e. mostly basic Python types).

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
        .. [#] https://docs.python.org/3/library/json.html#json-to-py-table
        .. [#] https://docs.python.org/3/library/json.html#py-to-json-table

        """
        raise NotImplementedError

    def batch_predict(self, df):
        """Produce an output from `df`.

        This method is called when batch predictions are made against a Verta endpoint.

        See `our product documentation on batch prediction
        <https://docs.verta.ai/verta/deployment/guides/batch-predictions>`__
        for more information about how this method is used.

        .. note::

            :meth:`batch_predict` must be written to both receive and return
            ``pandas.DataFrame``\ s [#]_.

            At this time, your subclass must still implement :meth:`predict`, regardless
            of whether it contains anything meaningful.

        Parameters
        ----------
        df : pandas.DataFrame

        Returns
        -------
        pandas.DataFrame

        References
        ----------
        .. [#] https://pandas.pydata.org/docs/reference/frame.html

        """
        raise NotImplementedError

    def model_test(self):
        """Test a model's behavior for correctness.

        Implement this method on your model—with any desired calls and
        assertions—to validate behavior and state.

        See `our product documentation on model verification
        <https://docs.verta.ai/verta/registry/guides/model-verification-and-testing#vertamodelbase.model_test>`__
        for more information about how this method is used.

        .. note::

            If using model data logging (e.g. :func:`verta.runtime.log`), any
            calls here to the model's :meth:`predict` method must be wrapped
            in a :class:`verta.runtime.context`. This also allows testing of
            expected logs.

        Returns
        -------
        None
            Returned values will be unused and discarded.

        Raises
        ------
        Any
            Raised exceptions will be propagated.

        Examples
        --------
        .. code-block:: python

            class MyModel(VertaModelBase):
                def __init__(self, artifacts):
                    with open(artifacts["sklearn_logreg"], "rb") as f:
                        self.logreg = pickle.load(f)

                @verify_io
                def predict(self, input):
                    verta.runtime.log("num_rows", len(input))
                    return self.logreg.predict(input).tolist()

                def example(self):
                    return [
                        [71.67822567370767, 0.0, 0.0, 99.0, 0.0, 0.0, 0.0, 1.0, 0.0],
                        [6.901547652701675, 0.0, 1887.0, 50.0, 0.0, 0.0, 0.0, 1.0, 0.0],
                        [72.84132724180968, 0.0, 0.0, 40.0, 0.0, 0.0, 0.0, 0.0, 1.0],
                    ]

                def model_test(self):
                    # call predict(), capturing model data logs
                    input = self.example()
                    with verta.runtime.context() as ctx:
                        output = self.predict(input)
                    logs = ctx.logs()

                    # check predict() output
                    expected_output = [0, 1, 0]
                    if output != expected_output:
                        raise ValueError(f"expected output {expected_output}, got {output}")

                    # check model data logs
                    expected_logs = {"num_rows": len(input)}
                    if logs != expected_logs:
                        raise ValueError(f"expected logs {expected_logs}, got {logs}")

        """
        pass
