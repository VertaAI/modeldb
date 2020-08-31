# -*- coding: utf-8 -*-

from ...external import six

import xgboost as xgb

from ..._internal_utils import _utils


def verta_callback(run):
    """
    XGBoost callback that automates logging to Verta during booster training.

    This callback logs ``eval_metric``\ s passed into ``xgb.train()``.

    See our `GitHub repository
    <https://github.com/VertaAI/modeldb/blob/master/client/workflows/examples/xgboost-integration.ipynb>`__
    for an example of this intergation in action.

    .. versionadded:: 0.13.20

    Parameters
    ----------
    run : :class:`~verta._tracking.experimentrun.ExperimentRun`
        Experiment Run tracking this model.

    Examples
    --------
    .. code-block:: python

        from verta.integrations.xgboost import verta_callback
        run = client.set_experiment_run()
        run.log_hyperparameters(params)
        bst = xgb.train(
            params, dtrain,
            evals=[(dtrain, "train")],
            callbacks=[verta_callback(run)],
        )

    """
    def callback(env):
        for metric, val in env.evaluation_result_list:
            try:
                run.log_observation(metric, val)
            except:
                pass  # don't halt execution
        # TODO: support `xgb.cv()`, which gives `(metric, val, std_dev)` across folds
    return callback
