"""
Overrides cross_val_score to store Fit and Metric Events for each fold.
"""
import numpy as np
import time
import warnings
import sys
from sklearn.grid_search import ParameterGrid, _CVScoreTuple
from sklearn.pipeline import Pipeline
from sklearn import datasets, linear_model, cross_validation
from sklearn.cross_validation import _safe_split, _score
from sklearn.cross_validation import check_cv
from sklearn.metrics.scorer import check_scoring
from sklearn.utils.validation import _num_samples, indexable
from sklearn.utils.multiclass import type_of_target
from sklearn.externals.joblib import Parallel, delayed
from sklearn.base import BaseEstimator, is_classifier, clone
from ..events import FitEvent
from ..events import MetricEvent

# Python 2 adn 3 deal differently with relative circular imports
if sys.version_info <= (3, 0):
    import ModelDbSyncer
else:
    from . import ModelDbSyncer


def cross_val_score_fn(estimator, X, y=None, scoring=None, cv=None, n_jobs=1,
                       verbose=0, fit_params=None, pre_dispatch='2*n_jobs'):
    """
    Evaluate a score by cross-validation.
    This overrides the cross_val_score method typically found in
    cross_validation.py. Changes are clearly marked in comments, but
    the main change is augmenting the function to store Fit and Metric Events
    for each fold.
    """
    X, y = indexable(X, y)

    cv = check_cv(cv, X, y, classifier=is_classifier(estimator))

    scorer = check_scoring(estimator, scoring=scoring)

    # Default scoring scheme is 'accuracy' unless provided by user.
    if scoring is None:
        scoring = 'accuracy'
    # We clone the estimator to make sure that all the folds are
    # independent, and that it is pickle-able.
    parallel = Parallel(n_jobs=n_jobs, verbose=verbose,
                        pre_dispatch=pre_dispatch)

    # Change from original scikit code: adding a new argument, scoring, to the
    # _fit_and_score function to track scoring function and create
    # MetricEvents.
    scores = parallel(delayed(_fit_and_score)(clone(estimator), X, y, scorer,
                                              train, test, verbose, None,
                                              fit_params, scoring)
                      for train, test in cv)
    return np.array(scores)[:, 0]


def _fit_and_score(estimator, X, y, scorer, train, test, verbose,
                   parameters, fit_params, scoring, return_train_score=False,
                   return_parameters=False, error_score='raise'):
    """
    Fit estimator and compute scores for a given dataset split.
    This overrides the behavior of _fit_and_score method in
    cross_validation.py.
    Note that a new argument, scoring, has been added to the function.

    Parameters
    ----------
    estimator : estimator object implementing 'fit'
        The object to use to fit the data.
    X : array-like of shape at least 2D
        The data to fit.
    y : array-like, optional, default: None
        The target variable to try to predict in the case of
        supervised learning.
    scorer : callable
        A scorer callable object / function with signature
        ``scorer(estimator, X, y)``.
    train : array-like, shape (n_train_samples,)
        Indices of training samples.
    test : array-like, shape (n_test_samples,)
        Indices of test samples.
    verbose : integer
        The verbosity level.
    error_score : 'raise' (default) or numeric
        Value to assign to the score if an error occurs in estimator fitting.
        If set to 'raise', the error is raised. If a numeric value is given,
        FitFailedWarning is raised. This parameter does not affect the refit
        step, which will always raise the error.
    parameters : dict or None
        Parameters to be set on the estimator.
    fit_params : dict or None
        Parameters that will be passed to ``estimator.fit``.
    scoring: string
        The name of the scoring function used in cross_val_score. Default is
        accuracy.
    return_train_score : boolean, optional, default: False
        Compute and return score on training set.
    return_parameters : boolean, optional, default: False
        Return parameters that has been used for the estimator.
    Returns
    -------
    train_score : float, optional
        Score on training set, returned only if `return_train_score` is `True`.
    test_score : float
        Score on test set.
    n_test_samples : int
        Number of test samples.
    scoring_time : float
        Time spent for fitting and scoring in seconds.
    parameters : dict or None, optional
        The parameters that have been evaluated.
    """
    if verbose > 1:
        if parameters is None:
            msg = "no parameters to be set"
        else:
            msg = '%s' % (', '.join('%s=%s' % (k, v)
                                    for k, v in parameters.items()))
        print("[CV] %s %s" % (msg, (64 - len(msg)) * '.'))

    # Adjust length of sample weights
    fit_params = fit_params if fit_params is not None else {}
    fit_params = dict([(k, _index_param_value(X, v, train))
                       for k, v in fit_params.items()])

    if parameters is not None:
        estimator.set_params(**parameters)

    start_time = time.time()

    x_train, y_train = _safe_split(estimator, X, y, train)
    x_test, y_test = _safe_split(estimator, X, y, test, train)

    try:
        if y_train is None:
            b = estimator.fit(x_train, **fit_params)
        else:
            b = estimator.fit(x_train, y_train, **fit_params)
    except Exception as e:
        if error_score == 'raise':
            raise
        elif isinstance(error_score, numbers.Number):
            test_score = error_score
            if return_train_score:
                train_score = error_score
            warnings.warn("Classifier fit failed. The score on this train-test"
                          " partition for these parameters will be set to %f. "
                          "Details: \n%r" % (error_score, e), FitFailedWarning)
        else:
            raise ValueError("error_score must be the string 'raise' or a"
                             " numeric value. (Hint: if using 'raise', please"
                             " make sure that it has been spelled correctly.)"
                             )

    else:
        test_score = _score(estimator, x_test, y_test, scorer)
        if return_train_score:
            train_score = _score(estimator, x_train, y_train, scorer)

    # Addition to original scikit code:
    # Create FitEvents for each estimator fit.
    fit_event = FitEvent(b, estimator, x_train)
    ModelDbSyncer.Syncer.instance.add_to_buffer(fit_event)

    scoring_time = time.time() - start_time
    if verbose > 2:
        msg += ", score=%f" % test_score
    if verbose > 1:
        end_msg = "%s -%s" % (msg, logger.short_format_time(scoring_time))
        print("[CV] %s %s" % ((64 - len(end_msg)) * '.', end_msg))

    ret = [train_score] if return_train_score else []
    ret.extend([test_score, _num_samples(x_test), scoring_time])

    # Addition to original scikit code:
    # Create MetricEvents for each estimator.
    metric_event = MetricEvent(x_test, estimator, "", "", scoring, test_score)
    ModelDbSyncer.Syncer.instance.add_to_buffer(metric_event)

    if return_parameters:
        ret.append(parameters)
    return ret
