{{ objname | escape | underline }}

.. currentmodule:: {{ module }}

.. autoclass:: {{ objname }}
    :members:
    :inherited-members:
    {% if 'ExperimentRun' != objname -%}
    :exclude-members: log_code, get_code
    {%- endif %}
