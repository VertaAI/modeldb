{{ name | escape | underline }}

.. automodule:: {{ fullname }}

    {% block functions %}
    {% if functions %}
    .. rubric:: {{ _('Functions') }}

    .. autosummary::
        :toctree:
        :template: function.rst
        :nosignatures:
    {% for item in functions %}
        {{ item }}
    {%- endfor %}
    {% endif %}
    {% endblock %}

    {% block classes %}
    {% if classes %}
    .. rubric:: {{ _('Classes') }}

    .. autosummary::
        :toctree:
        :template: class.rst
        :nosignatures:
    {% for item in classes %}
        {{ item }}
    {%- endfor %}
    {% endif %}
    {% endblock %}

{% block modules %}
{% if modules %}
.. rubric:: {{ _('Submodules') }}

.. autosummary::
    :toctree:
    :template: module.rst
    :recursive:
{% for item in modules %}
    {% set item_path = item.split('.') %}
    {{ item_path[-1] }}
{%- endfor %}
{% endif %}
{% endblock %}
