{{ objname | escape | underline }}

.. currentmodule:: {{ module }}

.. autoclass:: {{ objname }}
    :members:
    :inherited-members:
    {% if objname.endswith("Enum") -%}
    :undoc-members:
    :member-order: bysource
    {% endif -%}
    {% if 'ExperimentRun' != objname -%}
    :exclude-members: log_code, get_code
    {% endif -%}
