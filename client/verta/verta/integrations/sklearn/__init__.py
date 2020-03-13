# -*- coding: utf-8 -*-

"""
scikit-learn dynamic patch that automates logging to Verta during training.

This patch adds a ``run`` parameter to the ``fit()`` methods of most scikit-learn models, and logs
the model's hyperparameters.

See our `GitHub repository
<https://github.com/VertaAI/modeldb/blob/master/client/workflows/examples/sklearn-integration.ipynb>`__
for an example of this intergation in action.

.. versionadded:: 0.13.20

Examples
--------
.. code-block:: python

    import verta.integrations.sklearn
    run = client.set_experiment_run()
    model = sklearn.linear_model.LogisticRegression()
    model.fit(X, y, run=run)

"""

from ...external import six

from sklearn import (
    linear_model,
    tree,
    svm,
    ensemble,
    neural_network,
    multiclass,
    multioutput,
    isotonic,
    kernel_ridge,
)

from ...external import gorilla

from ..._internal_utils import _utils


classes = [
    linear_model.ARDRegression,
    linear_model.BayesianRidge,
    linear_model.ElasticNet, linear_model.ElasticNetCV,
    linear_model.HuberRegressor,
    linear_model.Lars, linear_model.LarsCV,
    linear_model.Lasso, linear_model.LassoCV,
    linear_model.LassoLars, linear_model.LassoLarsCV, linear_model.LassoLarsIC,
    linear_model.LinearRegression,
    linear_model.LogisticRegression, linear_model.LogisticRegressionCV,
    linear_model.MultiTaskLasso, linear_model.MultiTaskLassoCV,
    linear_model.MultiTaskElasticNet, linear_model.MultiTaskElasticNetCV,
    linear_model.OrthogonalMatchingPursuit, linear_model.OrthogonalMatchingPursuitCV,
    linear_model.PassiveAggressiveClassifier, linear_model.PassiveAggressiveRegressor,
    linear_model.Perceptron,
    linear_model.RANSACRegressor,
    linear_model.Ridge, linear_model.RidgeCV,
    linear_model.RidgeClassifier, linear_model.RidgeClassifierCV,
    linear_model.SGDClassifier, linear_model.SGDRegressor,
    linear_model.TheilSenRegressor,
    tree.DecisionTreeClassifier, tree.DecisionTreeRegressor,
    tree.ExtraTreeClassifier, tree.ExtraTreeRegressor,
    svm.LinearSVC, svm.LinearSVR,
    svm.NuSVC, svm.NuSVR,
    svm.OneClassSVM,
    svm.SVC, svm.SVR,
    ensemble.AdaBoostClassifier, ensemble.AdaBoostRegressor,
    ensemble.BaggingClassifier, ensemble.BaggingRegressor,
    ensemble.ExtraTreesClassifier, ensemble.ExtraTreesRegressor,
    ensemble.GradientBoostingClassifier, ensemble.GradientBoostingRegressor,
    ensemble.IsolationForest,
    ensemble.RandomForestClassifier, ensemble.RandomForestRegressor, ensemble.RandomTreesEmbedding,
    neural_network.BernoulliRBM,
    neural_network.MLPClassifier, neural_network.MLPRegressor,
    isotonic.IsotonicRegression,
    kernel_ridge.KernelRidge,
]


settings = gorilla.Settings(allow_hit=True)


def fit_and_log(self, cls, *args, **kwargs):
    run = kwargs.pop('run', None)
    if run is not None:
        params = self.get_params()

        # remove items that can't be logged as Verta hyperparameters
        cleaned_params = dict()
        for key, val in six.viewitems(params):
            try:
                _utils.python_to_val_proto(val)
            except TypeError:
                continue
            else:
                cleaned_params.update({key: val})

        try:
            run.log_hyperparameters(cleaned_params)
        except:
            pass  # don't halt execution

    original_fit = gorilla.get_original_attribute(cls, 'fit')
    return original_fit(self, *args, **kwargs)


def patch_fit(cls):
    @gorilla.patch(cls)
    def fit(self, *args, **kwargs):
        return fit_and_log(self, cls, *args, **kwargs)
    patch = gorilla.Patch(cls, 'fit', fit, settings=settings)
    gorilla.apply(patch)


for cls in classes:
    patch_fit(cls)
