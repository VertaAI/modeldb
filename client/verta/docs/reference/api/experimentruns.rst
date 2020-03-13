ExperimentRuns
==============


.. autoclass:: verta.client.ExperimentRuns
    :members:

    .. describe:: len(runs)

        Returns the number of :class:`ExperimentRun`\ s in this collection.

    .. describe:: runs[i]

        Returns the :class:`ExperimentRun` in this collection at index `i`.

    .. commented out until union is supported again
        .. describe:: runs + other

            Returns a new :class:`ExperimentRuns` that is the concatenation of this collection and `other`.
