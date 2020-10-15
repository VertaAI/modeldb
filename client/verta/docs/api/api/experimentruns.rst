ExperimentRuns
==============


.. autoclass:: verta._tracking.ExperimentRuns
    :members:
    :inherited-members:

    .. describe:: len(runs)

        Returns the number of :class:`~verta._tracking.experimentrun.ExperimentRun`\ s in this collection.

    .. describe:: runs[i]

        Returns the :class:`~verta._tracking.experimentrun.ExperimentRun` in this collection at index `i`.

    .. commented out until union is supported again
        .. describe:: runs + other

            Returns a new :class:`~verta._tracking.experimentrun.ExperimentRuns` that is the concatenation of this collection and `other`.
