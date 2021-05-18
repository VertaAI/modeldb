# -*- coding: utf-8 -*-

def reassign_module(members, module_name):
    """Reassign the ``__module__`` attribute of `members`.

    Our client follows a development pattern of placing classes in their own
    individual files, then surfacing them to a more friendly import path for
    users. To have Sphinx's autosummary extension pick up these classes at
    their intended paths, their ``__module__`` attribute needs to be
    reassigned to their user-facing import location.

    Parameters
    ----------
    members : list of type
        Classes (functions, objects, as desired).
    module_name : str
        Module name to assign. Typically the global ``__name__`` of the
        caller's module.

    Examples
    --------
    .. code-block:: python

        from ._submodule import Class

        documentation.reassign_module(
            members=[Class],
            module_name=__name__,
        )

    """
    for member in members:
        member.__module__ = module_name
