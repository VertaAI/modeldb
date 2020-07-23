# -*- coding: utf-8 -*-

import abc

from ..external import six


@six.add_metaclass(abc.ABCMeta)
class _UpdateStrategy(object):
    _STRATEGY = ""

    @abc.abstractmethod
    def _as_build_update_req_body(self, build_id):
        """
        Returns
        -------
        dict
            JSON to be passed as the body for an Endpoint update request.

        """
        pass

class DirectUpdateStrategy(_UpdateStrategy):
    _STRATEGY = "rollout"

    def _as_build_update_req_body(self, build_id):
        return {
            'build_id': build_id,
            'strategy': self._STRATEGY,
        }

class CanaryUpdateStrategy(_UpdateStrategy):
    _STRATEGY = "canary"

    def __init__(self, interval, step):
        """
        Parameters
        ----------
        interval : int
            Rollout interval, in seconds.
        step : float in (0, 1]
            Ratio of deployment to roll out per `interval`.

        """
        interval_err_msg = "`interval` must be int greater than 0"
        if not isinstance(interval, int):
            raise TypeError(interval_err_msg)
        if not interval > 0:
            raise ValueError(interval_err_msg)

        step_err_msg = "`step` must be float in (0, 1]"
        if not isinstance(step, float):
            raise TypeError(step_err_msg)
        if not 0 < step <= 1:
            raise ValueError(step_err_msg)

        self._progress_interval_seconds = interval
        self._progress_step = step
        self._rules = []

    def _as_build_update_req_body(self, build_id):
        raise NotImplementedError
        return {
            'build_id': build_id,
            'strategy': self._STRATEGY,
            'canary_strategy': {
                'progress_interval_seconds': self._progress_interval_seconds,
                'progress_step': self._progress_step,
                'rules': [
                    # TODO
                ],
            },
        }

    def add_rule(self, rule):
        raise NotImplementedError
