from ...external.six import PY2, PY3

if PY3:
    from ._time_utils_py3 import *
if PY2:
    from ._time_utils_py2 import *
