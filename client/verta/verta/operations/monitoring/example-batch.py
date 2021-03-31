import pandas as pd

continuous_columns = ["age", "capital-gain", "capital-loss", "hours-per-week", ">50k"]
binary_columns = [
    "workclass_local-gov",
    "workclass_private",
    "workclass_self-emp-inc",
    "workclass_self-emp-not-inc",
    "workclass_state-gov",
    "workclass_without-pay",
    "education_11th",
    "education_12th",
    "education_1st-4th",
    "education_5th-6th",
    "education_7th-8th",
    "education_9th",
    "education_assoc-acdm",
    "education_assoc-voc",
    "education_bachelors",
    "education_doctorate",
    "education_hs-grad",
    "education_masters",
    "education_preschool",
    "education_prof-school",
    "education_some-college",
    "relationship_not-in-family",
    "relationship_other-relative",
    "relationship_own-child",
    "relationship_unmarried",
    "relationship_wife",
    "occupation_armed-forces",
    "occupation_craft-repair",
    "occupation_exec-managerial",
    "occupation_farming-fishing",
    "occupation_handlers-cleaners",
    "occupation_machine-op-inspct",
    "occupation_other-service",
    "occupation_priv-house-serv",
    "occupation_prof-specialty",
    "occupation_protective-serv",
    "occupation_sales",
    "occupation_tech-support",
    "occupation_transport-moving",
]

from profilers import (
    MissingValuesProfiler,
    BinaryHistogramProfiler,
    ContinuousHistogramProfiler,
)

continuous_profilers = [MissingValuesProfiler, ContinuousHistogramProfiler]
binary_profilers = [MissingValuesProfiler, BinaryHistogramProfiler]

from client import Client

client = Client("https://dev.verta.ai")

entity = client.get_or_create_monitored_entity("example-batch-profiling-demo")
print(entity)


def profile_data(name, df):
    summaries = {}

    for continuous_column in continuous_columns:
        for continuous_profiler in continuous_profilers:
            summaries.update(
                continuous_profiler(columns=[continuous_column]).profile(df)
            )

    for binary_column in binary_columns:
        for binary_profiler in binary_profilers:
            summaries.update(binary_profiler(columns=[binary_column]).profile(df))

    data_source = entity.get_or_create_data_source(name)

    for k, v in summaries.items():
        summary = client.get_or_create_summary(
            name=k, content=v, data_source=data_source, monitored_entity=entity
        )


profile_data("training", pd.read_csv("../data/census-train.csv"))
profile_data("test", pd.read_csv("../data/census-test.csv"))
