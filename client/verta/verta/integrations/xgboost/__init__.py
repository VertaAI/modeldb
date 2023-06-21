# -*- coding: utf-8 -*-
"""XGBoost callback for automatic experiment run logging."""

from ..._vendored import six

import xgboost as xgb  # pylint: disable=import-error

from ..._internal_utils import _utils


class VertaCallback(xgb.callback.TrainingCallback):
    """
    XGBoost callback that automates logging to Verta during booster training.

    This callback logs ``eval_metric``\ s passed into ``xgb.train()``.

    See our `GitHub repository
    <https://github.com/VertaAI/examples/blob/main/experiment-management/xgboost/xgboost-integration.ipynb>`__
    for an example of this intergation in action.

    .. versionadded:: 0.13.20

    Parameters
    ----------
    run : :class:`~verta.tracking.entities.ExperimentRun`
        Experiment Run tracking this model.

    Examples
    --------
    .. code-block:: python

        from verta.integrations.xgboost import VertaCallback
        run = client.set_experiment_run()
        run.log_hyperparameters(params)
        bst = xgb.train(
            params, dtrain,
            evals=[(dtrain, "train")],
            callbacks=[VertaCallback(run)],
        )

    """

    def __init__(self, run):
        self.run = run

    def after_iteration(self, model, epoch, evals_log):
        for data, metric in evals_log.items():
            for metric_name, log in metric.items():
                try:
                    self.run.log_observation(
                        f"{data}-{metric_name}", log[-1]
                    )  # don't halt execution
                except:
                    pass
        # TODO: support `xgb.cv()`, which gives `(metric, val, std_dev)` across folds
