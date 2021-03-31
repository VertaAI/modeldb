from client import Client
from alerter import Alerter
from verta.environment import Python

client = Client("https://cj.dev.verta.ai")
model = client.get_or_create_registered_model("profilers")

from profilers import (
    MissingValuesProfiler,
    BinaryHistogramProfiler,
    ContinuousHistogramProfiler,
)


continuous_columns = ["age", "capital-gain", "capital-loss", "hours-per-week", ">50k"]
binary_columns = [
    "education_11th",
    "education_12th",
    "education_1st-4th",
    "education_5th-6th",
    "education_7th-8th",
    "education_9th",
]


def create_profiler(registered_model, profiler, name, type):
    version = registered_model.get_or_create_version(name=name)
    version.add_attribute(key="type", value=type, overwrite=True)
    version.log_model(profiler, overwrite=True)
    version.log_environment(Python(requirements=[]), overwrite=True)
    profiler = client.profilers.create(name=name, reference="{}".format(version.id))
    print(
        "Profiler {} has id {} and references model version with ID {}".format(
            name, profiler.id, version.id
        )
    )


def create_continuous_histogram_profiler(registered_model, column):
    name = "{} continuous histogram".format(column)
    profiler = ContinuousHistogramProfiler(columns=[column])
    create_profiler(registered_model, profiler, name, "profiler")


def create_binary_histogram_profiler(registered_model, column):
    name = "{} binary histogram".format(column)
    profiler = BinaryHistogramProfiler(columns=[column])
    create_profiler(registered_model, profiler, name, "profiler")


def create_missing_values_profiler(registered_model, column):
    name = "{} missing values".format(column)
    profiler = MissingValuesProfiler(columns=[column])
    create_profiler(registered_model, profiler, name, "profiler")

#
# for continuous_column in continuous_columns:
#     create_missing_values_profiler(model, continuous_column)
#     create_continuous_histogram_profiler(model, continuous_column)
#
#
# for binary_column in binary_columns:
#     create_missing_values_profiler(model, binary_column)
#     create_binary_histogram_profiler(model, binary_column)


monitored_entity = client.get_or_create_monitored_entity(name="alert-test")
reference_data_source = client.get_or_create_data_source("data_source", monitored_entity_id=monitored_entity.id)
profiler_ref = create_profiler(model, Alerter(columns=['age']), name='age', type='alert evaluator')
alert_definition = \
    client.alert_definitions.create("age missing values", monitored_entity.id, reference_data_source.id, profiler_ref.id)
print("created test alert definition: {}".format(alert_definition))