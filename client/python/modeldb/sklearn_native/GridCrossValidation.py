import numpy as np
import time
import warnings
from sklearn.grid_search import GridSearchCV, ParameterGrid, _CVScoreTuple
from sklearn.pipeline import Pipeline
from sklearn import datasets, linear_model, cross_validation, grid_search
from sklearn.cross_validation import _safe_split, _score
from sklearn.cross_validation import check_cv
from sklearn.metrics.scorer import check_scoring
from sklearn.utils.validation import _num_samples, indexable
from sklearn.utils.multiclass import type_of_target
from sklearn.externals.joblib import Parallel, delayed
from sklearn.base import BaseEstimator, is_classifier, clone

# This overrides the fit method found in grid_search.py. _fit method has
# been modified and changes to the original code are described in comments
# below.


def fit(self, X, y=None):
    return _fit(self, X, y, ParameterGrid(self.param_grid))

# This overrides the _fit method typically found in grid_search.py.
# Changes are clearly marked in comments, but the main change is near the
# end of the function, creating a new field, grid_cv_event for storing
# attributes.


def _fit(self, X, y, parameter_iterable):
    """Actual fitting,  performing the search over parameters."""
    estimator = self.estimator
    foldsForEstimator = {}
    cv = self.cv

    self.scorer_ = check_scoring(self.estimator, scoring=self.scoring)

    n_samples = _num_samples(X)
    X, y = indexable(X, y)

    if y is not None:
        if len(y) != n_samples:
            raise ValueError('Target variable (y) has a different number '
                             'of samples (%i) than data (X: %i samples)'
                             % (len(y), n_samples))

    # Splits the data based on provided cross-validation splitting strategy.
    cv = check_cv(cv, X, y, classifier=is_classifier(estimator))
    if self.verbose > 0:
        if isinstance(parameter_iterable, Sized):
            n_candidates = len(parameter_iterable)
            print("Fitting {0} folds for each of {1} candidates, totalling \
                {2} fits".format(len(cv), n_candidates,
                                 n_candidates * len(cv)))

    base_estimator = clone(self.estimator)

    pre_dispatch = self.pre_dispatch

    # Change from original scikit code: adding a new argument,
    # foldsForEstimator, to the _fit_and_score function to track metadata
    # for each estimator, for each fold.
    # _fit_and_score fits the estimator and computes the score for a given
    # data-split, for given parameters.
    out = Parallel(
        n_jobs=self.n_jobs, verbose=self.verbose,
        pre_dispatch=pre_dispatch
    )(
        delayed(_fit_and_score)(clone(base_estimator), X, y, self.scorer_,
                                train, test, self.verbose, parameters,
                                self.fit_params, foldsForEstimator,
                                return_parameters=True,
                                error_score=self.error_score)
        for parameters in parameter_iterable
        for train, test in cv)

    # Out is a list of triplet: score, estimator, n_test_samples
    n_fits = len(out)
    n_folds = len(cv)

    # Computes the scores for each of the folds, for all the possible
    # parameters, and stores them in grid_scores.
    scores = list()
    grid_scores = list()
    for grid_start in range(0, n_fits, n_folds):
        n_test_samples = 0
        score = 0
        all_scores = []
        for this_score, this_n_test_samples, _, parameters in out[
                grid_start:grid_start + n_folds]:
            all_scores.append(this_score)
            if self.iid:
                this_score *= this_n_test_samples
                n_test_samples += this_n_test_samples
            score += this_score
        if self.iid:
            score /= float(n_test_samples)
        else:
            score /= float(n_folds)
        scores.append((score, parameters))
        # TODO: shall we also store the test_fold_sizes?
        grid_scores.append(_CVScoreTuple(
            parameters,
            score,
            np.array(all_scores)))
    # Store the computed scores
    self.grid_scores_ = grid_scores

    # Find the best parameters by comparing on the mean validation score:
    # note that `sorted` is deterministic in the way it breaks ties
    best = sorted(grid_scores, key=lambda x: x.mean_validation_score,
                  reverse=True)[0]
    self.best_params_ = best.parameters
    self.best_score_ = best.mean_validation_score

    if self.refit:
        # fit the best estimator using the entire dataset
        # clone first to work around broken estimators
        best_estimator = clone(base_estimator).set_params(
            **best.parameters)
        if y is not None:
            best_estimator.fit(X, y, **self.fit_params)
        else:
            best_estimator.fit(X, **self.fit_params)
        self.best_estimator_ = best_estimator
    else:
        # If refit is false, we cannot _best_estimator_ is unavailable, and
        # further predictions can't be made on instance
        raise Warning(
            "Note: Refit has been set to false, which makes it impossible to "
            "make predictions using this GridSearchCV instance after fitting. "
            "Change refit to true to enable this")

    # Change from original scikit code:
    # Populate new field with necessary attributes for storing
    # cross-validation event
    self.grid_cv_event = [X, foldsForEstimator, 0, type_of_target(
        y), self.best_estimator_, self.best_estimator_, n_folds]
    return self

# This overrides the behavior of _fit_and_score method in
# cross_validation.py. Note that a new argument, foldsForEstimator, has
# been added to the function.


def _fit_and_score(estimator, X, y, scorer, train, test, verbose,
                   parameters, fit_params, foldsForEstimator,
                   return_train_score=False, return_parameters=False,
                   error_score='raise'):
    """Fit estimator and compute scores for a given dataset split.
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
    foldsForEstimator : dict
        Stores the necessary metadata for each estimator, for each fold.
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

    scoring_time = time.time() - start_time

    # Change from original scikit: For each estimator, for each fold, we keep
    # track of the fitted estimator, the training/test set, and the score.
    if not estimator in foldsForEstimator:
        foldsForEstimator[estimator] = []
    foldsForEstimator[estimator].append([(estimator, test, train, test_score)])

    if verbose > 2:
        msg += ", score=%f" % test_score
    if verbose > 1:
        end_msg = "%s -%s" % (msg, logger.short_format_time(scoring_time))
        print("[CV] %s %s" % ((64 - len(end_msg)) * '.', end_msg))

    ret = [train_score] if return_train_score else []
    ret.extend([test_score, _num_samples(x_test), scoring_time])
    if return_parameters:
        ret.append(parameters)
    return ret
