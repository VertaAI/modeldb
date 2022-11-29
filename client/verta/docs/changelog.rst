Changelog
=========


.. This comment block is a template for version release notes.
   v.. (--)
   --------------------

   Backwards Incompatibilities
   ^^^^^^^^^^^^^^^^^^^^^^^^^^^
   -
     (`# <>`__)

   Deprecations
   ^^^^^^^^^^^^
   -
     (`# <>`__)

   New Features
   ^^^^^^^^^^^^
   -
     (`# <>`__)

   Enhancements
   ^^^^^^^^^^^^
   -
     (`# <>`__)

   Bug Fixes
   ^^^^^^^^^
   -
     (`# <>`__)

   Internal Changes
   ^^^^^^^^^^^^^^^^
   -
     (`# <>`__)


v0.21.1 (2022-11-29)
--------------------

New Features
^^^^^^^^^^^^
- add ``ModelVersion.log_dataset_version()``, ``get_dataset_version()``, and ``del_dataset_version()``
  (`#3335 <https://github.com/VertaAI/modeldb/pull/3335>`__)

Enhancements
^^^^^^^^^^^^
- have ``endpoint.get_deployed_model()`` use a backend-provided prediction URL when available
  (`#3290 <https://github.com/VertaAI/modeldb/pull/3290>`__)
- alphabetically sort artifact, dataset version, and code version keys in model version repr
  (`#3340 <https://github.com/VertaAI/modeldb/pull/3340>`__)

Internal Changes
^^^^^^^^^^^^^^^^
- apply Black formatting to the full codebase
  (`#3258 <https://github.com/VertaAI/modeldb/pull/3258>`__)


v0.21.0 (2022-09-29)
--------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- remove support (and prevent installation) for Python 2.7, 3.5, and 3.6
  (`#3149 <https://github.com/VertaAI/modeldb/pull/3149>`__,
  `#3152 <https://github.com/VertaAI/modeldb/pull/3152>`__,
  `#3220 <https://github.com/VertaAI/modeldb/pull/3220>`__)
- remove ``ExperimentRun.log_artifact_path()`` and ``ExperimentRun.log_image_path()``
  (`#3159 <https://github.com/VertaAI/modeldb/pull/3159>`__)
- remove clientside artifact storage (and the ``VERTA_ARTIFACT_DIR`` environment variable)
  (`#3160 <https://github.com/VertaAI/modeldb/pull/3160>`__)
- remove ``ModelVersion.get_artifact_parts()`` and ``ExperimentRun.get_artifact_parts()``
  (`#3162 <https://github.com/VertaAI/modeldb/pull/3162>`__)
- remove ``ExperimentRun.log_dataset()``
  (`#3165 <https://github.com/VertaAI/modeldb/pull/3165>`__)

New Features
^^^^^^^^^^^^
- add support for Python 3.10
  (`#3161 <https://github.com/VertaAI/modeldb/pull/3161>`__)
- add support for registered model versions' ``input_description``,
  ``output_description``, ``hide_input_label``, and ``hide_output_label`` fields
  (`#3214 <https://github.com/VertaAI/modeldb/pull/3214>`__,
  `#3250 <https://github.com/VertaAI/modeldb/pull/3250>`__)

Enhancements
^^^^^^^^^^^^
- return a friendlier error message when passing incorrect Verta credentials
  (`#3136 <https://github.com/VertaAI/modeldb/pull/3136>`__)
- remove ``pathlib2`` dependency
  (`#3151 <https://github.com/VertaAI/modeldb/pull/3151>`__)
- bump ``pyyaml`` dependency version upper constraint from ``<6.0`` to ``<7.0``
  (`#3112 <https://github.com/VertaAI/modeldb/pull/3112>`__)
- bump ``cloudpickle`` dependency upper version constraint from ``<2.0`` to ``<3.0``
  (`#3106 <https://github.com/VertaAI/modeldb/pull/3106>`__)


v0.20.4 (2022-09-12)
--------------------

New Features
^^^^^^^^^^^^
- add ``VERTA_DISABLE_CLIENT_CONFIG`` environment variable to disable client
  config file discovery
  (`#3208 <https://github.com/VertaAI/modeldb/pull/3208>`__)

Bug Fixes
^^^^^^^^^
- during client config file discovery, skip directories that cannot be read
  (`#3208 <https://github.com/VertaAI/modeldb/pull/3208>`__)


v0.20.3 (2022-08-31)
--------------------

New Features
^^^^^^^^^^^^
- add ``.url`` property to most entities
  (`#3071 <https://github.com/VertaAI/modeldb/pull/3071>`__)
- add support for registered models' ``data_type`` and ``task_type`` fields
  (`#3079 <https://github.com/VertaAI/modeldb/pull/3079>`__,
  `#3086 <https://github.com/VertaAI/modeldb/pull/3086>`__)

Enhancements
^^^^^^^^^^^^
- bump ``protobuf`` dependency version upper constraint from ``<3.18`` to ``<4.0``
  (`#3070 <https://github.com/VertaAI/modeldb/pull/3070>`__)


v0.20.2 (2022-04-27)
--------------------

Enhancements
^^^^^^^^^^^^
- add ``cluster_config_id`` parameter to ``KafkaSettings``
  (`#2988 <https://github.com/VertaAI/modeldb/pull/2988>`__)
- bump click version constraint to ``<9.0``
  (`#3014 <https://github.com/VertaAI/modeldb/pull/3014>`__)


v0.20.1 (2022-04-11)
--------------------

New Features
^^^^^^^^^^^^
- add model monitoring interface to deployment
  (`#2962 <https://github.com/VertaAI/modeldb/pull/2962>`__)


v0.20.0.post0 (2022-04-11)
--------------------------

Bug Fixes
^^^^^^^^^
- fix issue where the package couldn't be built from source
  (`#2986 <https://github.com/VertaAI/modeldb/pull/2986>`__)


v0.20.0 (2022-04-08)
--------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- ``DeployedModel`` can no longer be directly instantiated from a run ID
  (though it usually shouldn't be directly instantiated anyway)
  (`#2727 <https://github.com/VertaAI/modeldb/pull/2727>`__)
- ``ExperimentRun.deploy()`` and related methods have been removed in favor of
  ``Endpoint``
  (`#2740 <https://github.com/VertaAI/modeldb/pull/2740>`__)
- custom module collection now favors pip-installed libraries over local
  directories with the same name
  (`#2805 <https://github.com/VertaAI/modeldb/pull/2805>`__)
- ``verta.monitoring`` and ``Client.monitoring`` have been removed
  (`#2812 <https://github.com/VertaAI/modeldb/pull/2812>`__)

New Features
^^^^^^^^^^^^
- support JWT cookies as an authn mechanism
  (`#2716 <https://github.com/VertaAI/modeldb/pull/2716>`__,
  `#2738 <https://github.com/VertaAI/modeldb/pull/2738>`__,
  `#2737 <https://github.com/VertaAI/modeldb/pull/2737>`__,
  `#2928 <https://github.com/VertaAI/modeldb/pull/2928>`__)
- add explicit credentials parameter to ``get_deployed_model()``
  (`#2727 <https://github.com/VertaAI/modeldb/pull/2727>`__)
- add ``ModelError`` exception for use in Verta Standard Models
  (`#2735 <https://github.com/VertaAI/modeldb/pull/2735>`__)
- support ``pip install verta[unit_tests]`` for optional testing dependencies
  (`#2788 <https://github.com/VertaAI/modeldb/pull/2788>`__)
- add ``RegisteredModelVersion.log_setup_script()``
  (`#2873 <https://github.com/VertaAI/modeldb/pull/2873>`__)

Enhancements
^^^^^^^^^^^^
- don't include spaCy models and ``anaconda-client`` in
  ``read_pip_environment()``
  (`#2709 <https://github.com/VertaAI/modeldb/pull/2709>`__)
- bump PyYAML version constraint to ``<6.0``
  (`#2718 <https://github.com/VertaAI/modeldb/pull/2718>`__)
- warn instead of error on version mismatch of ``verta`` and ``cloudpickle``
  in environment versioning
  (`#2723 <https://github.com/VertaAI/modeldb/pull/2723>`__)

Bug Fixes
^^^^^^^^^
- make sure everything subclasses ``object``
  (`#2748 <https://github.com/VertaAI/modeldb/pull/2748>`__)
- add validation for the ``artifacts`` parameter in
  ``RegisteredModelVersion.log_model()``
  (`#2783 <https://github.com/VertaAI/modeldb/pull/2783>`__)
- support directly deploying models that come from ``keras`` rather than
  ``tensorflow.python.keras``
  (`#2872 <https://github.com/VertaAI/modeldb/pull/2872>`__)
- properly fall back to client config file if an environment variable isn't
  set
  (`#2875 <https://github.com/VertaAI/modeldb/pull/2875>`__)


v0.19.3 (2021-10-29)
--------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- validate ``VertaModelBase.__init__()`` parameter names in ``RegisteredModel.create_standard_model()``
  (`#2570 <https://github.com/VertaAI/modeldb/pull/2570>`__)
- remove ``ExperimentRun.log_requirements()``
  (`#2584 <https://github.com/VertaAI/modeldb/pull/2584>`__)
- remove ``ExperimentRun.log_model_for_deployment()``
  (`#2584 <https://github.com/VertaAI/modeldb/pull/2584>`__)
- ``Python.read_pip_file()`` and ``Python.read_pip_environment()`` now include previously-unsupported lines rather than skipping them by default
  (`#2584 <https://github.com/VertaAI/modeldb/pull/2584>`__)

New Features
^^^^^^^^^^^^
- add ``SummarySample.is_aggregate``
  (`#2555 <https://github.com/VertaAI/modeldb/pull/2555>`__)
- fall back to logging raw requirements if manual parsing fails
  (`#2584 <https://github.com/VertaAI/modeldb/pull/2584>`__,
  `#2643 <https://github.com/VertaAI/modeldb/pull/2643>`__,
  `#2676 <https://github.com/VertaAI/modeldb/pull/2676>`__)
- support setting custom environment variables in environment objects
  (`#2634 <https://github.com/VertaAI/modeldb/pull/2634>`__)
- add ``Docker`` environment object
  (`#2636 <https://github.com/VertaAI/modeldb/pull/2636>`__)
- support ``Docker`` in ``log_environment()``/``get_environment()``
  (`#2637 <https://github.com/VertaAI/modeldb/pull/2637>`__)
- add ``DockerImage`` and ``RegisteredModelVersion.log_docker()``
  (`#2641 <https://github.com/VertaAI/modeldb/pull/2641>`__)
- add ``RegisteredModel.create_containerized_model()``
  (`#2648 <https://github.com/VertaAI/modeldb/pull/2648>`__)
- add support for updating endpoints with existing builds
  (`#2685 <https://github.com/VertaAI/modeldb/pull/2685>`__)

Bug Fixes
^^^^^^^^^
- return zero-valued samples from ``profile_point()``
  (`#2556 <https://github.com/VertaAI/modeldb/pull/2556>`__)
- avoid divide-by-zero when a histogram's buckets add to zero
  (`#2554 <https://github.com/VertaAI/modeldb/pull/2554>`__)

Internal Changes
^^^^^^^^^^^^^^^^
- rename ``remove_public_version_identifier()`` to ``remove_local_version_identifier()``
  (`#2601 <https://github.com/VertaAI/modeldb/pull/2601>`__)
- move ``_get_artifact_msg()`` to ``_DeployableEntity`` interface
  (`#2626 <https://github.com/VertaAI/modeldb/pull/2626>`__)
- add ``_MODEL_KEY`` to ``_DeployableEntity`` interface
  (`#2628 <https://github.com/VertaAI/modeldb/pull/2628>`__)
- save metadata in ``Artifact`` proto when logging model
  (`#2592 <https://github.com/VertaAI/modeldb/pull/2592>`__)
- add ``ensure_starts_with_slash()``
  (`#2640 <https://github.com/VertaAI/modeldb/pull/2640>`__)


v0.19.2 (2021-09-30)
--------------------

New Features
^^^^^^^^^^^^
- add ``RegisteredModelVersion.change_stage()``
  (`#2654 <https://github.com/VertaAI/modeldb/pull/2654>`__)

Bug Fixes
^^^^^^^^^
- restrict ``protobuf`` dependency version upper constraint from ``<4.0`` to ``<3.18`` `for Python 2 compatibility
  <https://github.com/protocolbuffers/protobuf/issues/8984>`__
  (`#2633 <https://github.com/VertaAI/modeldb/pull/2633>`__)


v0.19.1 (2021-08-09)
--------------------

New Features
^^^^^^^^^^^^
- add ``@verify_io`` decorator for use with ``VertaModelBase.predict()``
  (`#2540 <https://github.com/VertaAI/modeldb/pull/2540>`__)

Enhancements
^^^^^^^^^^^^
- paginate ``alerts.list()``
  (`#2525 <https://github.com/VertaAI/modeldb/pull/2525>`__)

Bug Fixes
^^^^^^^^^
- filter out ``lib32/`` and ``lib64/`` when collecting custom modules
  (`#2534 <https://github.com/VertaAI/modeldb/pull/2534>`__)
- pick up environment variables (such as ``REQUESTS_CA_BUNDLE``) for HTTP
  requests
  (`#2535 <https://github.com/VertaAI/modeldb/pull/2535>`__)


v0.19.0 (2021-08-03)
--------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- remove Repository
  (`#2498 <https://github.com/VertaAI/modeldb/pull/2498>`__)

New Features
^^^^^^^^^^^^
- enable extra headers to be passed on requests
  (`#2510 <https://github.com/VertaAI/modeldb/pull/2510>`__)
- add ``RegisteredModelVersion.log_code_version()``
  (`#2523 <https://github.com/VertaAI/modeldb/pull/2523>`__)
- enable Kafka configuration on endpoints (if supported by Verta backend)
  (`#2488 <https://github.com/VertaAI/modeldb/pull/2488>`__,
  `#2520 <https://github.com/VertaAI/modeldb/pull/2520>`__)

Enhancements
^^^^^^^^^^^^
- batch requests in ``log_training_data_profile()`` for a drastic speedup
  (`#2511 <https://github.com/VertaAI/modeldb/pull/2511>`__)
- allow ``RegisteredModel.create_version_from_run()`` to take a run object
  (`#2500 <https://github.com/VertaAI/modeldb/pull/2500>`__)

Bug Fixes
^^^^^^^^^
- properly promote naïve datetimes in Python 2
  (`#2506 <https://github.com/VertaAI/modeldb/pull/2506>`__)

Internal Changes
^^^^^^^^^^^^^^^^
- include non-public fields in ``Alert.summary_sample_base``
  (`#2519 <https://github.com/VertaAI/modeldb/pull/2519>`__)


v0.18.2 (2021-07-14)
--------------------

New Features
^^^^^^^^^^^^
- add convenience functions to create ready-to-deploy standard Verta model
  versions
  (`#2397 <https://github.com/VertaAI/modeldb/pull/2397>`__,
  `#2450 <https://github.com/VertaAI/modeldb/pull/2450>`__,
  `#2486 <https://github.com/VertaAI/modeldb/pull/2486>`__)
- add model_version.log_training_data_profile() for deployment monitoring
  (`#2434 <https://github.com/VertaAI/modeldb/pull/2434>`__,
  `#2446 <https://github.com/VertaAI/modeldb/pull/2446>`__,
  `#2457 <https://github.com/VertaAI/modeldb/pull/2457>`__,
  `#2484 <https://github.com/VertaAI/modeldb/pull/2484>`__)
- `add profile_point() to profilers
  <https://github.com/VertaAI/modeldb/pull/2433>`__
- `support specifying start_time and end_time on experiment runs
  <https://github.com/VertaAI/modeldb/pull/2479>`__

Enhancements
^^^^^^^^^^^^
- `propagate model logs when a deployment fails during initialization
  <https://github.com/VertaAI/modeldb/pull/2444>`__
- `hide internal attributes from histogram __repr__()s
  <https://github.com/VertaAI/modeldb/pull/2442>`__

Bug Fixes
^^^^^^^^^
- `resolve a ParseError for the "UNASSIGNED" model_version stage
  <https://github.com/VertaAI/modeldb/commit/2e5a67d#diff-948fe60>`__
- `fix bug where NumericValues were deserialized inconsistently when "unit" is
  empty
  <https://github.com/VertaAI/modeldb/pull/2428>`__
- `fix bug where ContinuousHistogramProfiler was unable to handle missing data
  <https://github.com/VertaAI/modeldb/pull/2440>`__
- `allow alerts to properly handle samples of past time windows
  <https://github.com/VertaAI/modeldb/pull/2478>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `deprecate verta._internal_utils._histogram_utils
  <https://github.com/VertaAI/modeldb/pull/2436>`__


v0.18.1 (2021-06-17)
--------------------

Enhancements
^^^^^^^^^^^^
- `raise warning when duplicate attributes are ignored in model versions
  <https://github.com/VertaAI/modeldb/pull/2405>`__

Bug Fixes
^^^^^^^^^
- `fix zip-unzip magic for directories logged with run.log_model()
  <https://github.com/VertaAI/modeldb/pull/2420>`__


v0.18.0 (2021-06-11)
--------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- `rename with_workspace(workspace_name) parameter to
  with_workspace(workspace)
  <https://github.com/VertaAI/modeldb/pull/2352>`__
- move formerly-private modules and entity classes to public import paths
  (`#2011 <https://github.com/VertaAI/modeldb/pull/2011>`__,
  `#2308 <https://github.com/VertaAI/modeldb/pull/2308>`__,
  `#2313 <https://github.com/VertaAI/modeldb/pull/2313>`__,
  `#2314 <https://github.com/VertaAI/modeldb/pull/2314>`__)
- `set upper version bounds on dependency libraries
  <https://github.com/VertaAI/modeldb/pull/2293>`__
- `add pytimeparse as a dependency
  <https://github.com/VertaAI/modeldb/pull/2348/files#diff-6890bb89ea3cf891e88298d45a9a377077ca81742d1675fb72b11c5043b99e37R33>`__
- `hide sklearn integration's internally-used functions
  <https://github.com/VertaAI/modeldb/pull/2307/files#diff-8393105a4ae4d198e065ad00cf00f62b64ebb4ac6bb7695b1aedbaa077c4cf22>`__

Deprecations
^^^^^^^^^^^^
- `log_training_data(), being superseded by new monitoring functionality
  <https://github.com/VertaAI/modeldb/pull/2253>`__
- `log_requirements(), being superseded by log_environment()
  <https://github.com/VertaAI/modeldb/pull/2258>`__
- `TFSavedModel utility, long-since superseded by Standard Verta Models
  <https://github.com/VertaAI/modeldb/pull/2307/files#diff-38dbfbb4b30b23b1fa5af3f91dc2046c18f405169c49865db152d0a37558072a>`__

New Features
^^^^^^^^^^^^
- add monitoring sub-client
  (`#2077 <https://github.com/VertaAI/modeldb/pull/2077>`__,
  `#2096 <https://github.com/VertaAI/modeldb/pull/2096>`__,
  `#2097 <https://github.com/VertaAI/modeldb/pull/2097>`__,
  `#2095 <https://github.com/VertaAI/modeldb/pull/2095>`__,
  `#2091 <https://github.com/VertaAI/modeldb/pull/2091>`__,
  `#2133 <https://github.com/VertaAI/modeldb/pull/2133>`__,
  `#2120 <https://github.com/VertaAI/modeldb/pull/2120>`__,
  `#2126 <https://github.com/VertaAI/modeldb/pull/2126>`__,
  `#2134 <https://github.com/VertaAI/modeldb/pull/2134>`__,
  `#2145 <https://github.com/VertaAI/modeldb/pull/2145>`__,
  `#2159 <https://github.com/VertaAI/modeldb/pull/2159>`__,
  `#2162 <https://github.com/VertaAI/modeldb/pull/2162>`__,
  `#2164 <https://github.com/VertaAI/modeldb/pull/2164>`__,
  `#2182 <https://github.com/VertaAI/modeldb/pull/2182>`__,
  `#2186 <https://github.com/VertaAI/modeldb/pull/2186>`__,
  `#2187 <https://github.com/VertaAI/modeldb/pull/2187>`__,
  `#2184 <https://github.com/VertaAI/modeldb/pull/2184>`__,
  `#2200 <https://github.com/VertaAI/modeldb/pull/2200>`__,
  `#2201 <https://github.com/VertaAI/modeldb/pull/2201>`__,
  `#2212 <https://github.com/VertaAI/modeldb/pull/2212>`__,
  `#2252 <https://github.com/VertaAI/modeldb/pull/2252>`__,
  `#2262 <https://github.com/VertaAI/modeldb/pull/2262>`__,
  `#2263 <https://github.com/VertaAI/modeldb/pull/2263>`__,
  `#2269 <https://github.com/VertaAI/modeldb/pull/2269>`__,
  `#2317 <https://github.com/VertaAI/modeldb/pull/2317>`__,
  `#2318 <https://github.com/VertaAI/modeldb/pull/2318>`__,
  `#2332 <https://github.com/VertaAI/modeldb/pull/2332>`__,
  `#2326 <https://github.com/VertaAI/modeldb/pull/2326>`__,
  `#2348 <https://github.com/VertaAI/modeldb/pull/2348>`__,
  `#2355 <https://github.com/VertaAI/modeldb/pull/2355>`__,
  `#2356 <https://github.com/VertaAI/modeldb/pull/2356>`__,
  `#2360 <https://github.com/VertaAI/modeldb/pull/2360>`__,
  `#2370 <https://github.com/VertaAI/modeldb/pull/2370>`__,
  `#2374 <https://github.com/VertaAI/modeldb/pull/2374>`__,
  `#2399 <https://github.com/VertaAI/modeldb/pull/2399>`__)
- `allow setting workspace through environment variable
  <https://github.com/VertaAI/modeldb/pull/2351>`__
- `add apt_packages to Python()
  <https://github.com/VertaAI/modeldb/pull/2385>`__
- `add NumericValue and StringValue to verta.data_types
  <https://github.com/VertaAI/modeldb/pull/2085>`__
- `add verta.registry.VertaModelBase for Standard Verta Models
  <https://github.com/VertaAI/modeldb/pull/2378>`__

Enhancements
^^^^^^^^^^^^
- `add __repr__()s to verta.data_types
  <https://github.com/VertaAI/modeldb/pull/2087>`__

Bug Fixes
^^^^^^^^^
- `return path from download_model()
  <https://github.com/VertaAI/modeldb/pull/2325>`__
- `support mismatched buckets for discrete histogram data_type
  <https://github.com/VertaAI/modeldb/pull/2215>`__
- `remove local version identifier from captured library version numbers to
  unblock deployment
  <https://github.com/VertaAI/modeldb/pull/2257>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `use __subclasses__() instead of a manual list
  <https://github.com/VertaAI/modeldb/pull/2102>`__
- `add client._conn.email
  <https://github.com/VertaAI/modeldb/pull/2254>`__
- `rewrite __module__s to public import paths
  <https://github.com/VertaAI/modeldb/pull/2307>`__
- `move verta._dataset_versioning to verta.dataset.entities
  <https://github.com/VertaAI/modeldb/pull/2313>`__


v0.17.6 (2021-04-23)
--------------------

New Features
^^^^^^^^^^^^
- `add download_artifact() and download_model() to RegisteredModelVersion
  <https://github.com/VertaAI/modeldb/pull/2222>`__


v0.17.5 (2021-04-14)
--------------------

Bug Fixes
^^^^^^^^^
- `unzip directory models in run.download_model()
  <https://github.com/VertaAI/modeldb/pull/2121>`__


v0.17.4 (2021-03-26)
--------------------

New Features
^^^^^^^^^^^^
- `support logging structured data types as run attributes
  <https://github.com/VertaAI/modeldb/pull/2057>`__
- `support getting back structured data type attributes
  <https://github.com/VertaAI/modeldb/pull/2062>`__
- `enable manually specifying page limit for _LazyList iteration
  <https://github.com/VertaAI/modeldb/pull/2064>`__

Bug Fixes
^^^^^^^^^
- `properly paginate _LazyList requests
  <https://github.com/VertaAI/modeldb/pull/2063>`__


v0.17.3 (2021-03-17)
--------------------

New Features
^^^^^^^^^^^^
- `set and get lock levels on model versions
  <https://github.com/VertaAI/modeldb/pull/2016>`__

Enhancements
^^^^^^^^^^^^
- `add stage to model version repr
  <https://github.com/VertaAI/modeldb/pull/2015>`__
- `follow symlinks when collecting custom modules
  <https://github.com/VertaAI/modeldb/pull/2026>`__

Bug Fixes
^^^^^^^^^
- `properly propagate 403s/404s when updating registry entities
  <https://github.com/VertaAI/modeldb/pull/2018>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `move verta._registry to verta.registry._entities
  <https://github.com/VertaAI/modeldb/pull/2011>`__
- `make client's debug cURL util more readable
  <https://github.com/VertaAI/modeldb/pull/2030>`__


v0.17.2 (2021-02-26)
--------------------

New Features
^^^^^^^^^^^^
- `enable finding model versions based on stage
  <https://github.com/VertaAI/modeldb/pull/2006>`__


v0.17.1 (2021-02-24)
--------------------

New Features
^^^^^^^^^^^^
- `add run.log_environment()
  <https://github.com/VertaAI/modeldb/pull/1972>`__
- `add run.download_model()
  <https://github.com/VertaAI/modeldb/pull/1973>`__

Enhancements
^^^^^^^^^^^^
- `support arbitrary models in run.log_model()
  <https://github.com/VertaAI/modeldb/pull/1971>`__
- `skip custom modules and model API when logging an arbitrary model
  <https://github.com/VertaAI/modeldb/pull/1987>`__
- `unzip artifact directories in run.download_artifact()
  <https://github.com/VertaAI/modeldb/pull/1973>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `rename artifact key blocklist
  <https://github.com/VertaAI/modeldb/pull/1974>`__
- `consolidate chunk sizes with named constants
  <https://github.com/VertaAI/modeldb/pull/1988>`__


v0.17.0 (2021-02-16)
--------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- `in newer backends, an entity created in an organization will use that
  organization's permissions settings by default, instead of defaulting to
  private
  <https://github.com/VertaAI/modeldb/pull/1993>`__

New Features
^^^^^^^^^^^^
- `add client.set_workspace() and client.get_workspace()
  <https://github.com/VertaAI/modeldb/pull/1916>`__
- `enable new visibility values for newer backends
  <https://github.com/VertaAI/modeldb/pull/1896>`__
- `enable passing PySpark models to run.log_model()
  <https://github.com/VertaAI/modeldb/pull/1935>`__
- `add Path.with_spark()
  <https://github.com/VertaAI/modeldb/pull/1941>`__

Enhancements
^^^^^^^^^^^^
- `for custom modules files, grant non-owners read access
  <https://github.com/VertaAI/modeldb/pull/1939>`__
- `remove "file:" prefix from path datasets
  <https://github.com/VertaAI/modeldb/pull/1940>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `consolidate workspace helper methods into Connection
  <https://github.com/VertaAI/modeldb/pull/1914>`__
- `add Connection methods for personal and default workspace
  <https://github.com/VertaAI/modeldb/pull/1915>`__
- `prevent test teardowns from resulting in 403s
  <https://github.com/VertaAI/modeldb/pull/1930>`__


v0.16.5 (2021-01-26)
--------------------

New Features
^^^^^^^^^^^^
- `add parameter to disable autocapture in Git() and run.log_code()
  <https://github.com/VertaAI/modeldb/pull/1897>`__
- `add is_dirty parameter to Git()
  <https://github.com/VertaAI/modeldb/pull/1900>`__
- `add is_dirty parameter to run.log_code()
  <https://github.com/VertaAI/modeldb/pull/1901>`__
- `add public attributes to Git() objects
  <https://github.com/VertaAI/modeldb/pull/1899>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `constrain pyyaml to <5.4 to avoid build dependency on C
  <https://github.com/VertaAI/modeldb/pull/1895>`__


v0.16.4 (2021-01-14)
--------------------

Internal Changes
^^^^^^^^^^^^^^^^
- `send Grpc-Metadata-developer-key (hyphen instead of underscore) as an
  additional auth header
  <https://github.com/VertaAI/modeldb/pull/1865>`__


v0.16.3 (2020-12-18)
--------------------

Bug Fixes
^^^^^^^^^
- `fix AttributeError when using public_within_org=True
  <https://github.com/VertaAI/modeldb/pull/1785>`__


v0.16.2 (2020-12-16)
--------------------

Bug Fixes
^^^^^^^^^
- `fix bug where set_registered_model() and set_dataset() unset the client's
  active project
  <https://github.com/VertaAI/modeldb/pull/1780>`__


v0.16.1 (2020-12-14)
--------------------

New Features
^^^^^^^^^^^^
- `enable managed versioning for dataset versions
  <https://github.com/VertaAI/modeldb/pull/1766>`__


v0.16.0 (2020-12-09)
--------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- `overhaul dataset versioning API
  <https://github.com/VertaAI/modeldb/pull/1699>`__

New Features
^^^^^^^^^^^^
- `add HDFS dataset blob type
  <https://github.com/VertaAI/modeldb/pull/1691>`__


v0.15.9 (2020-11-21)
--------------------

New Features
^^^^^^^^^^^^
- `enable find() to take *args rather than a single list
  <https://github.com/VertaAI/modeldb/pull/1680>`__

Bug Fixes
^^^^^^^^^
- `remove limitation on searching for runs by tag
  <https://github.com/VertaAI/modeldb/pull/1666>`__
- `temporarily disable continuing interrupted multipart uploads
  <https://github.com/VertaAI/modeldb/pull/1687>`__


v0.15.8 (2020-11-17)
--------------------

New Features
^^^^^^^^^^^^
- `add public_within_org param to Client.get_or_create_endpoint()
  <https://github.com/VertaAI/modeldb/pull/1661>`__

Enhancements
^^^^^^^^^^^^
- `propagate HTTP error messages for Client init errors
  <https://github.com/VertaAI/modeldb/pull/1640>`__
- `display a simpler error message for backend errors
  <https://github.com/VertaAI/modeldb/pull/1650>`__


v0.15.7 (2020-11-05)
--------------------

New Features
^^^^^^^^^^^^
- `support logging models serialized with torch.save()
  <https://github.com/VertaAI/modeldb/pull/1589>`__
- `enable continuing previously-interrupted multipart uploads
  <https://github.com/VertaAI/modeldb/pull/1585>`__

Enhancements
^^^^^^^^^^^^
- `ignore folders themselves in old-style S3 dataset versioning
  <https://github.com/VertaAI/modeldb/pull/1573>`__
- `ignore .git/ for custom modules
  <https://github.com/VertaAI/modeldb/pull/1578>`__
- `raise warning when metadata are provided to get_or_create_*()
  <https://github.com/VertaAI/modeldb/pull/1582>`__
- `add print to Dataset.get_latest_version()
  <https://github.com/VertaAI/modeldb/pull/1527>`__
- `have custom modules ignore libraries in __pycache__/
  <https://github.com/VertaAI/modeldb/pull/1536>`__
- `catch Keras serialization error with h5py v3.0.0
  <https://github.com/VertaAI/modeldb/pull/1625>`__


v0.15.6 (2020-10-02)
--------------------

New Features
^^^^^^^^^^^^
- `add ModelVersions.with_workspace()
  <https://github.com/VertaAI/modeldb/pull/1367>`__
- `add public_within_org param to Client.get_or_create_repository()
  <https://github.com/VertaAI/modeldb/pull/1540>`__

Enhancements
^^^^^^^^^^^^
- `allow custom modules to handle non-PyPI pip-installed packages
  <https://github.com/VertaAI/modeldb/pull/1554>`__
- `propagate build errors during endpoint updates
  <https://github.com/VertaAI/modeldb/pull/1358>`__
- `use DirectUpdateStrategy as a default strategy for endpoint updates
  <https://github.com/VertaAI/modeldb/pull/1541>`__
- `add broader log-requirements support for modules whose PyPI names use dashes
  instead of underscores
  <https://github.com/VertaAI/modeldb/pull/1553>`__
- `raise more informative error parsing attributes in CLI
  <https://github.com/VertaAI/modeldb/pull/1430>`__

Bug Fixes
^^^^^^^^^
- `fix bug where CPU could not be empty for endpoint resources
  <https://github.com/VertaAI/modeldb/pull/1504>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `refactor client.set_repository() to attempt get before create
  <https://github.com/VertaAI/modeldb/pull/1428>`__
- `use backend implementation of experiment run clone
  <https://github.com/VertaAI/modeldb/pull/1561>`__


v0.15.4 (2020-09-09)
--------------------

Bug Fixes
^^^^^^^^^
- `fix bug where a DatasetVersion could not be retrieved for certain protobuf
  configurations
  <https://github.com/VertaAI/modeldb/pull/1471>`__


v0.15.3 (2020-09-08)
--------------------

New Features
^^^^^^^^^^^^
- `add DatasetVersion.list_components() and DatasetVersion.base_path
  <https://github.com/VertaAI/modeldb/pull/1448>`__
- `enable cloning a run into another experiment
  <https://github.com/VertaAI/modeldb/pull/1420>`__
- `add delete() to most entities
  <https://github.com/VertaAI/modeldb/pull/1372>`__

Enhancements
^^^^^^^^^^^^
- `support Python 3.8
  <https://github.com/VertaAI/modeldb/pull/1418>`__
- `add self_contained param to download_docker_context()
  <https://github.com/VertaAI/modeldb/pull/1425>`__

Bug Fixes
^^^^^^^^^
- `fix bug where log_model(overwrite=True) didn't work with custom model
  artifacts
  <https://github.com/VertaAI/modeldb/pull/1447>`__
- `properly raise an error when re-logging an environment to a model version
  <https://github.com/VertaAI/modeldb/pull/1439>`__


v0.15.2 (2020-08-28)
--------------------

Enhancements
^^^^^^^^^^^^
- `add retries for all client connection errors
  <https://github.com/VertaAI/modeldb/pull/1407>`__


v0.15.1 (2020-08-24)
--------------------

Bug Fixes
^^^^^^^^^
- `correctly log model artifacts attribute to enable custom models with dependencies
  <https://github.com/VertaAI/modeldb/pull/1399>`__


v0.15.0 (2020-08-24)
--------------------

New Features
^^^^^^^^^^^^
- `Verta model registry
  <https://verta.readthedocs.io/en/master/_autogen/verta.registry.html>`__
- `Verta endpoints
  <https://verta.readthedocs.io/en/master/_autogen/verta.endpoint.html>`__

Enhancements
^^^^^^^^^^^^
- `expand custom modules virtual environment filter
  <https://github.com/VertaAI/modeldb/pull/1392>`__

Bug Fixes
^^^^^^^^^
- `fix bug where multipart upload loop may have an undefined variable in the
  event of connection errors
  <https://github.com/VertaAI/modeldb/pull/1362>`__
- `fix bug where getting a dataset version from another workspace may fail
  <https://github.com/VertaAI/modeldb/pull/1349>`__


v0.14.17 (2020-08-13)
---------------------

New Features
^^^^^^^^^^^^
- `add DeployedModel.get_curl()
  <https://github.com/VertaAI/modeldb/pull/1287>`__
- `add ExperimentRun.get_artifact_keys()
  <https://github.com/VertaAI/modeldb/pull/1296>`__

Bug Fixes
^^^^^^^^^
- `fix inability to get others' shared personal Projects and Datasets
  <https://github.com/VertaAI/modeldb/pull/1286>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `add util to convert requests to cURL
  <https://github.com/VertaAI/modeldb/pull/1268>`__


v0.14.16 (2020-08-06)
---------------------

New Features
^^^^^^^^^^^^
- `add URL to Project.__repr__()
  <https://github.com/VertaAI/modeldb/pull/1160>`__
- `add client.create_*() for core ModelDB entities
  <https://github.com/VertaAI/modeldb/pull/1152>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `make model container paths configurable through environment variables
  <https://github.com/VertaAI/modeldb/pull/1219>`__


v0.14.15 (2020-07-28)
---------------------

Bug Fixes
^^^^^^^^^
- `nicely display stack traces from prediction errors
  <https://github.com/VertaAI/modeldb/pull/1166>`__


v0.14.14 (2020-07-28)
---------------------

Bug Fixes
^^^^^^^^^
- `fix error when accessing a shared project in an organization
  <https://github.com/VertaAI/modeldb/pull/1163>`__


v0.14.13 (2020-07-24)
---------------------

Enhancements
^^^^^^^^^^^^
- `add more possible keys for _LazyList.find()
  <https://github.com/VertaAI/modeldb/pull/1038>`__
- `print full response body for HTTP errors
  <https://github.com/VertaAI/modeldb/pull/1083>`__

Bug Fixes
^^^^^^^^^
- `fix artifact download when $TMPDIR is in a different filesystem
  <https://github.com/VertaAI/modeldb/pull/1130>`__
- `clear cache after every ExperimentRun logs
  <https://github.com/VertaAI/modeldb/pull/1101>`__
- `fix TypeError in _LazyList.with_workspace(None)
  <https://github.com/VertaAI/modeldb/pull/1098>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `move verta/deployment.py to verta/deployment/
  <https://github.com/VertaAI/modeldb/pull/1085>`__


v0.14.12 (2020-07-16)
---------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- `run.log_dataset() now only accepts DatasetVersion objects
  <https://github.com/VertaAI/modeldb/pull/907>`__
- `verta.environment.Python now requires its requirements parameter
  <https://github.com/VertaAI/modeldb/pull/952>`__

New Features
^^^^^^^^^^^^
- `add client.get_project(), get_experiment(), and get_experiment_run()
  <https://github.com/VertaAI/modeldb/pull/966>`__
- `add client.projects and client.experiments
  <https://github.com/VertaAI/modeldb/pull/979>`__
- `add expt_runs.as_dataframe()
  <https://github.com/VertaAI/modeldb/pull/968>`__
- `add list_components() to data versioning blobs
  <https://github.com/VertaAI/modeldb/pull/903>`__
- `implement addition for data versioning blobs
  <https://github.com/VertaAI/modeldb/pull/938>`__
- `add add() to data versioning blobs
  <https://github.com/VertaAI/modeldb/pull/939>`__
- `add run.download_deployment_crd()
  <https://github.com/VertaAI/modeldb/pull/918>`__
- `add run.download_docker_context()
  <https://github.com/VertaAI/modeldb/pull/919>`__

Enhancements
^^^^^^^^^^^^
- `speed up verta import time by deferring external imports
  <https://github.com/VertaAI/modeldb/pull/999>`__
- `cache calls to get metrics and hyperparameters
  <https://github.com/VertaAI/modeldb/pull/967>`__
- `include UTC timestamps in HTTPError messages
  <https://github.com/VertaAI/modeldb/pull/909>`__
- `attempt torch.load() first in run.get_artifact()
  <https://github.com/VertaAI/modeldb/pull/947>`__

Bug Fixes
^^^^^^^^^
- `use parent for commit.log() if the commit is unsaved
  <https://github.com/VertaAI/modeldb/pull/940>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `partially refactor versioning blobs
  <https://github.com/VertaAI/modeldb/pull/901>`__
- `split ModelDB entities into their own files
  <https://github.com/VertaAI/modeldb/pull/964>`__
- `refactor ModelDB entity gets/creates
  <https://github.com/VertaAI/modeldb/pull/965>`__
- `add pylint config to repo
  <https://github.com/VertaAI/modeldb/pull/974>`__


v0.14.11 (2020-06-26)
---------------------

New Features
^^^^^^^^^^^^
- `add visibility for date created & updated on ExperimentRuns
  <https://github.com/VertaAI/modeldb/pull/843>`__


v0.14.10 (2020-06-22)
---------------------

Bug Fixes
^^^^^^^^^
- `use a proper default for VERTA_ARTIFACT_DIR
  <https://github.com/VertaAI/modeldb/pull/844>`__


v0.14.9 (2020-06-22)
--------------------

Bug Fixes
^^^^^^^^^
- `expand user directory for clientside artifact storage environment variable
  <https://github.com/VertaAI/modeldb/pull/840>`__


v0.14.8 (2020-06-22)
--------------------

New Features
^^^^^^^^^^^^
- `enable clientside artifact storage
  <https://github.com/VertaAI/modeldb/pull/823>`__
- `add epoch_num parameter to run.log_observations()
  <https://github.com/VertaAI/modeldb/pull/827>`__
- `add run.download_artifact()
  <https://github.com/VertaAI/modeldb/pull/828>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `add fixture for running tests in isolated directories
  <https://github.com/VertaAI/modeldb/pull/822>`__


v0.14.7 (2020-06-18)
--------------------

New Features
^^^^^^^^^^^^
- `add workspace parameter to find_datasets()
  <https://github.com/VertaAI/modeldb/pull/758>`__

Enhancements
^^^^^^^^^^^^
- `print part numbers during multipart upload
  <https://github.com/VertaAI/modeldb/pull/688>`__
- `retry part uploads on connection errors
  <https://github.com/VertaAI/modeldb/pull/729>`__
- `read bytestreams in chunks
  <https://github.com/VertaAI/modeldb/pull/706>`__
- `enable fuzzy find by name in find_datasets()
  <https://github.com/VertaAI/modeldb/pull/793>`__
- `raise more informative error on non-JSON response bodies
  <https://github.com/VertaAI/modeldb/pull/799>`__

Bug Fixes
^^^^^^^^^
- `always set grpc-metadata-source header, even in no-auth
  <https://github.com/VertaAI/modeldb/pull/794>`__
- `typecheck tags
  <https://github.com/VertaAI/modeldb/pull/761>`__
- `don't follow 302s
  <https://github.com/VertaAI/modeldb/pull/798>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `add _path_component_blobs attribute for dataset blobs
  <https://github.com/VertaAI/modeldb/pull/777>`__
- `handle protos refactor
  <https://github.com/VertaAI/modeldb/pull/749>`__


v0.14.6 (2020-05-29)
--------------------

New Features
^^^^^^^^^^^^
- `enable multipart artifact uploads
  <https://github.com/VertaAI/modeldb/pull/643>`__

Bug Fixes
^^^^^^^^^
- `fix Notebook Blob repr
  <https://github.com/VertaAI/modeldb/pull/629>`__
- `support NumPy bool_ and pandas 1.X in ModelAPI
  <https://github.com/VertaAI/modeldb/pull/630>`__
- `ignore folders in S3 versioning Blobs
  <https://github.com/VertaAI/modeldb/pull/631>`__
- `inject verta and cloudpickle into Python environment Blobs
  <https://github.com/VertaAI/modeldb/pull/644>`__
- `blocklist deployment artifact keys
  <https://github.com/VertaAI/modeldb/pull/648>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `remove logic duplication for fixing NFS URLs
  <https://github.com/VertaAI/modeldb/pull/659>`__
- `calculate SHA-256 checksums for artifacts in chunks
  <https://github.com/VertaAI/modeldb/pull/670>`__


v0.14.5 (2020-05-13)
--------------------

New Features
^^^^^^^^^^^^
- `support logging Keras models in TensorFlow 2.X
  <https://github.com/VertaAI/modeldb/pull/621>`__
- `support eagerly-executed TensorFlow Tensors in ModelAPI
  <https://github.com/VertaAI/modeldb/pull/626>`__

Bug Fixes
^^^^^^^^^
- `filter out spaCy models when versioning pip requirements files
  <https://github.com/VertaAI/modeldb/pull/627>`__


v0.14.4 (2020-05-04)
--------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- `run.log_training_data() no longer uploads a "train_data" artifact, and instead directly
  generates a histogram for deployment data monitoring
  <https://github.com/VertaAI/modeldb/pull/576>`__


v0.14.3 (2020-04-20)
--------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- `replace commit.branch() with commit.new_branch()
  <https://github.com/VertaAI/modeldb/pull/494>`__

New Features
^^^^^^^^^^^^
- `enable passing in ~ as part of filepaths
  <https://github.com/VertaAI/modeldb/pull/493>`__
- `enable setting host from $VERTA_HOST
  <https://github.com/VertaAI/modeldb/pull/537>`__
- `capture versioning information from S3
  <https://github.com/VertaAI/modeldb/pull/526>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `add click as a dependency in preparation for CLI development
  <https://github.com/VertaAI/modeldb/pull/482>`__
- `use back end for commit merges
  <https://github.com/VertaAI/modeldb/pull/485>`__
- `use back end for commit reverts
  <https://github.com/VertaAI/modeldb/pull/510>`__


v0.14.2 (2020-04-01)
--------------------

New Features
^^^^^^^^^^^^
- `use friendly default messages for merges and reverts
  <https://github.com/VertaAI/modeldb/pull/355>`__
- `implement __repr__ for Blobs
  <https://github.com/VertaAI/modeldb/pull/434>`__

Bug Fixes
^^^^^^^^^
- `filter out spaCy models from pip freeze
  <https://github.com/VertaAI/modeldb/pull/367>`__
- `make dataset.dataset_type friendlier
  <https://github.com/VertaAI/modeldb/pull/419>`__
- `enable e.g. Notebook Blobs to be retrieved from a Commit outside of Notebooks
  <https://github.com/VertaAI/modeldb/pull/424>`__
- `enable set_repository() without Verta authentication credentials
  <https://github.com/VertaAI/modeldb/pull/451>`__
- `validate Client config file against protobuf spec
  <https://github.com/VertaAI/modeldb/pull/420>`__
- `add more helpful typechecks on Commit methods
  <https://github.com/VertaAI/modeldb/pull/415>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `update protobufs for diffapi changes
  <https://github.com/VertaAI/modeldb/pull/431>`__
- `fix race condition when running parallel tests
  <https://github.com/VertaAI/modeldb/pull/401>`__
- `update tests to delete children Commits first
  <https://github.com/VertaAI/modeldb/pull/421>`__


v0.14.1 (2020-03-17)
--------------------

New Features
^^^^^^^^^^^^
- `add complete versioning system
  <api/api/versioning.html>`__
- `enable going directly from Client to ExperimentRun using Verta config
  <https://github.com/VertaAI/modeldb-verta/pull/96>`__
- `add public_within_org option for set_project() and set_dataset()
  <https://github.com/VertaAI/modeldb-verta/pull/121>`__
- `add aliases for Client's set/get-or-create methods
  <https://github.com/VertaAI/modeldb-verta/pull/272/files>`__

Bug Fixes
^^^^^^^^^
- `enable larger sets of ExperimentRuns to be queried
  <https://github.com/VertaAI/modeldb-verta/pull/72>`__
- `enable ZIPing files that have invalid timestamps
  <https://github.com/VertaAI/modeldb-verta/pull/154>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `move cloned dependencies to their own submodule
  <https://github.com/VertaAI/modeldb-verta/pull/22>`__
- `move internal utils into their own submodule
  <https://github.com/VertaAI/modeldb-verta/pull/217>`__


v0.14.0 (2020-02-11)
--------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- `require token in DeployedModel::from_url(), even if it's None
  <https://github.com/VertaAI/modeldb-client/pull/335>`__

New Features
^^^^^^^^^^^^
- `add a workspace parameter to client.set_project() and client.set_dataset()
  <https://github.com/VertaAI/modeldb-client/pull/328>`__
- `enable logging singleton scalar NumPy arrays as metrics
  <https://github.com/VertaAI/modeldb-client/pull/338>`_
- `implement Keras Client integration
  <https://github.com/VertaAI/modeldb-client/pull/330>`__
- `implement PyTorch Client integration
  <https://github.com/VertaAI/modeldb-client/pull/337>`__
- `implement scikit-learn Client integration
  <https://github.com/VertaAI/modeldb-verta/pull/23>`__
- `implement TensorFlow Client integration
  <https://github.com/VertaAI/modeldb-client/pull/331>`__
- `implement TensorBoard Client integration
  <https://github.com/VertaAI/modeldb-verta/pull/38>`__
- `implement XGBoost Client intergation
  <https://github.com/VertaAI/modeldb-client/pull/334>`__

Bug Fixes
^^^^^^^^^
- `allow negative numbers in Python 3 expt_runs.find() queries
  <https://github.com/VertaAI/modeldb-verta/pull/77>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `interpret HTTP 403s on getProject as project not found
  <https://github.com/VertaAI/modeldb-verta/pull/10>`__
- `include gorilla as internal dependency
  <https://github.com/VertaAI/modeldb-verta/pull/22>`__
- `explicitly include ModelDB in RPC endpoints
  <https://github.com/VertaAI/modeldb-verta/pull/28>`__


v0.13.19 (2020-01-08)
---------------------

New Features
^^^^^^^^^^^^
- `enable overwriting code and dataset versions
  <https://github.com/VertaAI/modeldb-client/pull/323>`__
- `unpack tarballs in run.fetch_artifacts()
  <https://github.com/VertaAI/modeldb-client/pull/316>`__

Bug Fixes
^^^^^^^^^
- `include virtual environment-like directories when automatically logging custom modules
  <https://github.com/VertaAI/modeldb-client/pull/324>`__


v0.13.18 (2019-12-12)
---------------------

New Features
^^^^^^^^^^^^
- `add run.clone()
  <https://github.com/VertaAI/modeldb-client/pull/312>`__
- `add a decorator for models' predict() to handle argument unpacking
  <https://github.com/VertaAI/modeldb-client/pull/318>`__

Bug Fixes
^^^^^^^^^
- `properly propagate deployment error messages
  <https://github.com/VertaAI/modeldb-client/pull/320>`__
- `enable calling run.deploy() and run.undeploy() even if the run is already deployed / not deployed
  <https://github.com/VertaAI/modeldb-client/pull/319>`__
- `properly handle Python 2 string types in querying methods
  <https://github.com/VertaAI/modeldb-client/pull/317>`__


v0.13.17 (2019-12-05)
---------------------

Deprecations
^^^^^^^^^^^^
- `utils.TFSavedModel, in favor of the class-as-model system
  <https://github.com/VertaAI/modeldb-client/pull/306/files>`__

New Features
^^^^^^^^^^^^
- `enable passing more datatypes into DeployedModel.predict()
  <https://github.com/VertaAI/modeldb-client/pull/307>`__
- `add overwrite flag to most artifact logging functions
  <https://github.com/VertaAI/modeldb-client/pull/308>`__
- `enable deployment through ExperimentRun objects
  <https://github.com/VertaAI/modeldb-client/pull/309>`__
- `add a decorator for models' predict() to handle datatype conversion
  <https://github.com/VertaAI/modeldb-client/pull/313>`__
- `only default to https for endpoints hosted by Verta
  <https://github.com/VertaAI/modeldb-client/pull/311>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `remove external dependency on six
  <https://github.com/VertaAI/modeldb-client/pull/310>`__


v0.13.16 (2019-12-02)
---------------------

New Features
^^^^^^^^^^^^
- `enable logging directories as ZIP archives with log_artifact()
  <https://github.com/VertaAI/modeldb-client/pull/304>`__


v0.13.15 (2019-11-27)
---------------------

New Features
^^^^^^^^^^^^
- `support logging classes as models
  <https://github.com/VertaAI/modeldb-client/pull/298>`__
- `support associating artifact dependencies with Standard Verta Models
  <https://github.com/VertaAI/modeldb-client/pull/299>`__
- `enable downloading artifacts into a local cache for use with Standard Verta Models
  <https://github.com/VertaAI/modeldb-client/pull/300>`__


v0.13.14 (2019-11-19)
---------------------

New Features
^^^^^^^^^^^^
- `enable indefinite retries on prediction 404s
  <https://github.com/VertaAI/modeldb-client/pull/297>`__


v0.13.13 (2019-11-18)
---------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- `ExperimentRun.log_model() now no longer accepts a user-defined key, and is intended for deployment
  <https://github.com/VertaAI/modeldb-client/pull/292>`__

Deprecations
^^^^^^^^^^^^
- `ExperimentRun.log_model_for_deployment(), in favor of more modular logging functions
  <https://github.com/VertaAI/modeldb-client/blob/f3b84ca/verta/verta/client.py#L2399>`__

New Features
^^^^^^^^^^^^
- `implement ExperimentRun.log_requirements()
  <https://github.com/VertaAI/modeldb-client/pull/291>`__
- `implement ExperimentRun.log_training_data()
  <https://github.com/VertaAI/modeldb-client/pull/293>`__
- `make prediction token optional in DeployedModel::from_url()
  <https://github.com/VertaAI/modeldb-client/pull/290>`__

Bug Fixes
^^^^^^^^^
- `retry predictions on non-model 502s
  <https://github.com/VertaAI/modeldb-client/pull/289>`__


v0.13.12 (2019-11-07)
---------------------

New Features
^^^^^^^^^^^^
- `enable indefinite retries on prediction 429s
  <https://github.com/VertaAI/modeldb-client/pull/283>`__

Bug Fixes
^^^^^^^^^
- `accommodate external 502s on predictions
  <https://github.com/VertaAI/modeldb-client/pull/285>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `pass host URL scheme to back end
  <https://github.com/VertaAI/modeldb-client/pull/282>`__
- `reduce dataset version name collisions in tests
  <https://github.com/VertaAI/modeldb-client/pull/284>`__


v0.13.11 (2019-10-30)
---------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- `slightly bump dependency versions and remove grpcio
  <https://github.com/VertaAI/modeldb-client/pull/280>`__

Bug Fixes
^^^^^^^^^
- `obtain DatasetVersion timestamps robustly for Python 2
  <https://github.com/VertaAI/modeldb-client/pull/277>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `clean up Datasets generated during tests
  <https://github.com/VertaAI/modeldb-client/pull/278>`__
- `skip tests on missing imports instead of failing
  <https://github.com/VertaAI/modeldb-client/pull/279>`__


v0.13.10 (2019-10-27)
---------------------

Bug Fixes
^^^^^^^^^
- `fix bug with locally-hosted artifact stores
  <https://github.com/VertaAI/modeldb-client/compare/f32b5a0...8e13822>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `update notebooks
  <https://github.com/VertaAI/modeldb-client/compare/a6ccf9c...f32b5a0>`__


v0.13.9 (2019-10-17)
--------------------

Bug Fixes
^^^^^^^^^
- `replace json.JSONDecodeError for Python 2
  <https://github.com/VertaAI/modeldb-client/pull/262>`__
- `remove check for Verta credentials from DeployedModel::from_url()
  <https://github.com/VertaAI/modeldb-client/pull/268>`__
- `properly resolve relative paths in deployment for custom modules
  <https://github.com/VertaAI/modeldb-client/pull/267>`__
- `enable uploading non-Python artifacts
  <https://github.com/VertaAI/modeldb-client/pull/262>`__
- `enable consistent retrieval of models for Python 2
  <https://github.com/VertaAI/modeldb-client/pull/270>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `add retries for HTTP 502s
  <https://github.com/VertaAI/modeldb-client/pull/264/files>`__


v0.13.8 (2019-10-03)
--------------------

New Features
^^^^^^^^^^^^
- `enable logging a setup script for the beginning of model deployment
  <https://github.com/VertaAI/modeldb-client/pull/259>`__
- `add verta to uploaded requirements if not present
  <https://github.com/VertaAI/modeldb-client/pull/260>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `revise pytests
  <https://github.com/VertaAI/modeldb-client/pull/232>`__


v0.13.7 (2019-09-18)
--------------------

New Features
^^^^^^^^^^^^
- `accept key prefixes for S3DatasetVersion
  <https://github.com/VertaAI/modeldb-client/pull/216>`__
- `implement verta.deployment.DeployedModel
  <https://github.com/VertaAI/modeldb-client/pull/221>`__

Bug Fixes
^^^^^^^^^
- `enable code version to be downloaded as a ZIP archive through the Web App
  <https://github.com/VertaAI/modeldb-client/pull/207>`__
- `fix bug in run.get_dataset_version()
  <https://github.com/VertaAI/modeldb-client/pull/223>`__
- `fix bug in dataset.get_latest_version()
  <https://github.com/VertaAI/modeldb-client/pull/227>`__
- `catch all unpickling-related errors in get_artifact()
  <https://github.com/VertaAI/modeldb-client/pull/213>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `keep cell execution numbers in example notebooks
  <https://github.com/VertaAI/modeldb-client/pull/217>`__


v0.13.6 (2019-09-05)
--------------------

Bug Fixes
^^^^^^^^^
- `fix small bugs in the _dataset submodule
  <https://github.com/VertaAI/modeldb-client/pull/211>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `update protos
  <https://github.com/VertaAI/modeldb-client/pull/212>`__


v0.13.5 (2019-09-05)
--------------------

Bug Fixes
^^^^^^^^^
- `fix various bugs in the _dataset submodule
  <https://github.com/VertaAI/modeldb-client/commit/971a8c6>`__


v0.13.3 (2019-09-04)
--------------------

Deprecations
^^^^^^^^^^^^
- `client.expt_runs, because its meaning is ambiguous; proj.expt_runs and expt.expt_runs are preferred
  <https://github.com/VertaAI/modeldb-client/pull/193>`__
- `ret_all_info parameter in querying methods, because it returns user-unfriendly objects
  <https://github.com/VertaAI/modeldb-client/pull/201>`__

New Features
^^^^^^^^^^^^
- `implement client.set_experiment_run(id=…)
  <https://github.com/VertaAI/modeldb-client/pull/184>`__
- `implement dataset retrieval functions
  <https://github.com/VertaAI/modeldb-client/pull/205>`__
- `propagate error messages from the back end
  <https://github.com/VertaAI/modeldb-client/pull/196>`__

Bug Fixes
^^^^^^^^^
- `support run.get_*() when the value is None
  <https://github.com/VertaAI/modeldb-client/pull/191>`__
- `fix bug where Project, Experiment, and ExperimentRun objects couldn't be pickled
  <https://github.com/VertaAI/modeldb-client/pull/201>`__
- `fix bug when Datasets are created in Python 2
  <https://github.com/VertaAI/modeldb-client/pull/190>`__
- `log DatasetVersion timestamps as milliseconds, as expected by the Web App
  <https://github.com/VertaAI/modeldb-client/pull/182>`__
- `fix bug when the working directory is captured by run.log_modules()
  <https://github.com/VertaAI/modeldb-client/pull/187>`__
- `fix bug when run.log_modules() is used in Python 2
  <https://github.com/VertaAI/modeldb-client/pull/188>`__
- `fix bug when querying methods are called from an empty ExperimentRuns
  <https://github.com/VertaAI/modeldb-client/pull/195>`__
- `perform basic key validation in querying methods
  <https://github.com/VertaAI/modeldb-client/pull/194>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `create testing fixtures for deterministic input spaces
  <https://github.com/VertaAI/modeldb-client/pull/185>`__
- `fix data versioning tests
  <https://github.com/VertaAI/modeldb-client/pull/183>`__
- `fix non-artifact tests
  <https://github.com/VertaAI/modeldb-client/pull/186>`__
- `fix artifact tests
  <https://github.com/VertaAI/modeldb-client/pull/189>`__
- `implement model logging tests
  <https://github.com/VertaAI/modeldb-client/pull/192>`__
- `implement basic querying method tests
  <https://github.com/VertaAI/modeldb-client/pull/199>`__


v0.13.2 (2019-08-20)
--------------------

New Features
^^^^^^^^^^^^
- `add ExperimentRun.get_dataset_version()
  <https://github.com/VertaAI/modeldb-client/commit/f8831da>`__


v0.13.1 (2019-08-20)
--------------------

Bug Fixes
^^^^^^^^^
- `handle more states in DatasetVersion.__repr__()
  <https://github.com/VertaAI/modeldb-client/commit/801a3f3>`__


v0.13.0 (2019-08-20)
--------------------

New Features
^^^^^^^^^^^^
- `enable file extensions on artifacts in the Web App
  <https://github.com/VertaAI/modeldb-client/pull/144>`__
- `support basic data versioning
  <https://github.com/VertaAI/modeldb-client/compare/cfea45e...4bbfcd1>`__

Bug Fixes
^^^^^^^^^
- `convert everything to new-style classes for Python 2 compatibility
  <https://github.com/VertaAI/modeldb-client/pull/147/files>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `support dynamically fetching custom deployment URLs
  <https://github.com/VertaAI/modeldb-client/pull/145>`__
- `make Pillow an optional dependency
  <https://github.com/VertaAI/modeldb-client/pull/170>`__
- `support potentially handling a 401 on verifyConnection
  <https://github.com/VertaAI/modeldb-client/pull/152>`__


v0.12.9 (2019-08-13)
--------------------

New Features
^^^^^^^^^^^^
- `support passing in a full URL as the host parameter to Client()
  <https://github.com/VertaAI/modeldb-client/pull/166>`__

Bug Fixes
^^^^^^^^^
- `fix bugs regarding logging and retrieving datasets
  <https://github.com/VertaAI/modeldb-client/pull/167>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `propagate more deployment errors to the Client
  <https://github.com/VertaAI/modeldb-client/pull/165>`__


v0.12.8 (2019-08-08)
--------------------

Internal Changes
^^^^^^^^^^^^^^^^
- bump patch version to 8, to celebrate August 8th
- `handle getting Verta environment variables more consistently
  <https://github.com/VertaAI/modeldb-client/commit/ad99713>`__


v0.12.7 (2019-08-08)
--------------------

New Features
^^^^^^^^^^^^
- `support logging functions for deployment
  <https://github.com/VertaAI/modeldb-client/pull/157>`__
- `ignore virtual environment directories when logging custom modules for deployment
  <https://github.com/VertaAI/modeldb-client/pull/161>`__

Bug Fixes
^^^^^^^^^
- `define source code UTF-8 encoding for Python 2 compatibility
  <https://github.com/VertaAI/modeldb-client/pull/159>`__
- `use new-style classes for Python 2 compatibility
  <https://github.com/VertaAI/modeldb-client/commit/bbfa327>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `implement DeployedModel::from_url() factory method
  <https://github.com/VertaAI/modeldb-client/pull/163>`__
- `propagate runtime errors to the Client during DeployedModel.predict()
  <https://github.com/VertaAI/modeldb-client/commit/2f55d11>`__
- `add custom module logging example notebook
  <https://github.com/VertaAI/modeldb-client/pull/155>`__


v0.12.6 (2019-08-01)
--------------------

New Features
^^^^^^^^^^^^
- `implement a compress parameter on demo predict utility to enable request body compression
  <https://github.com/VertaAI/modeldb-client/pull/154>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `reduce redundancies in demo predict utility
  <https://github.com/VertaAI/modeldb-client/pull/153>`__


v0.12.5 (2019-07-26)
--------------------

New Features
^^^^^^^^^^^^
- `implement a debug parameter and attribute on Client to print verbose debugging information
  <https://github.com/VertaAI/modeldb-client/pull/149>`__


v0.12.4 (2019-07-25)
--------------------

New Features
^^^^^^^^^^^^
- `remove the need for log_modules()'s second argument (search_path)
  <https://github.com/VertaAI/modeldb-client/pull/148>`__


v0.12.3 (2019-07-17)
--------------------

Bug Fixes
^^^^^^^^^
- `ensure ModelAPI value names are cast to str
  <https://github.com/VertaAI/modeldb-client/commit/7cfb28e>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `identify model types by superclass
  <https://github.com/VertaAI/modeldb-client/commit/e3cc177>`__
- `update example notebooks with proper ModelAPI instantiation
  <https://github.com/VertaAI/modeldb-client/commit/fa868a1>`__
- `update demo notebook with log_code()
  <https://github.com/VertaAI/modeldb-client/commit/277f045>`__


v0.12.2 (2019-07-16)
--------------------

Bug Fixes
^^^^^^^^^
- `move Git repo check from Client init to log_code()
  <https://github.com/VertaAI/modeldb-client/commit/1fe9532>`__


v0.12.1 (2019-07-16)
--------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- `The non-public prediction utility now uses our updated REST prediction endpoint
  <https://github.com/VertaAI/modeldb-client/pull/128>`__

New Features
^^^^^^^^^^^^
- `implement log_code() and get_code() for code versioning
  <https://github.com/VertaAI/modeldb-client/pull/135>`__
- `allow periods in Artifact get functions
  <https://github.com/VertaAI/modeldb-client/pull/121>`__
- `enable retrieving integers as integers (instead of as floats) from the back end
  <https://github.com/VertaAI/modeldb-client/commit/cd34c94>`__

Bug Fixes
^^^^^^^^^
- `catch and raise duplicate column name error on ModelAPI initialization
  <https://github.com/VertaAI/modeldb-client/pull/123>`__
- `properly handle daylight saving time when logging observation timestamps
  <https://github.com/VertaAI/modeldb-client/pull/131>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `implement internal Configuration utility struct
  <https://github.com/VertaAI/modeldb-client/pull/134>`__
- `add PyTorch example notebook
  <https://github.com/VertaAI/modeldb/blob/master/client/workflows/examples/pytorch.ipynb>`__
- `implement internal utility for unwrapping directory paths into contained filepaths
  <https://github.com/VertaAI/modeldb-client/pull/124>`__
- `implement internal utilities for reading Git information from the local filesystem
  <https://github.com/VertaAI/modeldb-client/pull/126>`__
- `implement internal utilities for finding executing Python source files
  <https://github.com/VertaAI/modeldb-client/pull/133>`__
- `implement internal utility for getting the file extension from a filepath
  <https://github.com/VertaAI/modeldb-client/pull/129>`__
- `log file extensions with Artifacts
  <https://github.com/VertaAI/modeldb-client/pull/130>`__


v0.12.0 (2019-06-27)
--------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- `The dump() and load() functions have been removed from the public utils module.
  <https://github.com/VertaAI/modeldb-client/commit/c17013d>`__

New Features
^^^^^^^^^^^^
- `implement ignore_conn_err parameter and attribute to Client
  <https://github.com/VertaAI/modeldb-client/pull/118>`__
- `implement log_modules() for uploading custom Python modules for deployment
  <https://github.com/VertaAI/modeldb-client/pull/120>`__

Bug Fixes
^^^^^^^^^
- `enable logging lists, and dictionaries with string keys, as attributes on client.set_*() to match
  run.log_attribute()
  <https://github.com/VertaAI/modeldb-client/pull/113>`__
- `simplify stack traces by suppressing contexts during handling for a remaining handful of raise
  statements
  <https://github.com/VertaAI/modeldb-client/commit/886f3bb>`__
- `add missing error message to get_observation()
  <https://github.com/VertaAI/modeldb-client/commit/4c77343>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `use internal Connection utility object for connection configuration
  <https://github.com/VertaAI/modeldb-client/pull/118>`__
- `define Artifact Store bucket names using a checksum of the artifact
  <https://github.com/VertaAI/modeldb-client/pull/116>`__
- `check for dataset CSV existence before wget in census-end-to-end.ipynb
  <https://github.com/VertaAI/modeldb-client/commit/ccd7831>`__
- `expand and unify gitignores
  <https://github.com/VertaAI/modeldb-client/pull/119>`__


v0.11.7 (2019-06-10)
--------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- `The constructors for Project, Experiment, ExperimentRun, and ExperimentRuns—as well as with their
  _get() and _create() functions—now take an additional retry parameter, though these functions are
  all not intended for public use to begin with.
  <https://github.com/VertaAI/modeldb-client/pull/112>`__

New Features
^^^^^^^^^^^^
- `enable logging lists, and dictionaries with string keys, as attributes
  <https://github.com/VertaAI/modeldb-client/pull/109>`__
- `implement a max_retries parameter and attribute on Client to retry requests with exponential
  backoff on 403s, 503s, and 504s
  <https://github.com/VertaAI/modeldb-client/pull/112>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `delegate most REST calls to an internal utility function
  <https://github.com/VertaAI/modeldb-client/pull/112>`__
- `implement back end load test
  <https://github.com/VertaAI/modeldb-client/pull/110>`__
- `change Read the Docs sidebar from fixed to static
  <https://github.com/VertaAI/modeldb-client/commit/5f75fe6>`__
- `fix a bug that matplotlib has with macOS which was restricting testing
  <https://github.com/VertaAI/modeldb-client/commit/ddea440>`__


v0.11.6 (2019-06-07)
--------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- `Providing a cloudpickle version in the requirements for deployment that doesn't match the version
  used by the Client now raises an error instead of overwriting the line in the requirements.
  <https://github.com/VertaAI/modeldb-client/commit/871bef8>`__

New Features
^^^^^^^^^^^^
- `add ExperimentRun's Verta WebApp URL to its __repr__()
  <https://github.com/VertaAI/modeldb-client/pull/108>`__

Bug Fixes
^^^^^^^^^
- `use cloudpickle.__version__ instead of relying on pip
  <https://github.com/VertaAI/modeldb-client/commit/82c0f82>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `remove internal utility get_env_dependencies()
  <https://github.com/VertaAI/modeldb-client/commit/ce333bc>`__
- `update notebooks
  <https://github.com/VertaAI/modeldb-client/commit/0003f31>`__


v0.11.5 (2019-06-04)
--------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- `The dataset_csv parameter for log_model_for_deployment() has been replaced with two parameters
  for feature and target DataFrames.
  <https://github.com/VertaAI/modeldb-client/commit/4d11355>`__

Bug Fixes
^^^^^^^^^
- `properly render lists in docstrings
  <https://github.com/VertaAI/modeldb-client/commit/4f5c6c2>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `have the upload script clean out build directories after uploading
  <https://github.com/VertaAI/modeldb-client/commit/9d78662>`__


v0.11.4 (2019-05-31)
--------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- `The dataset_df parameter for log_model_for_deployment() has been renamed to dataset_csv.
  <https://github.com/VertaAI/modeldb-client/commit/ea49d06>`__

Bug Fixes
^^^^^^^^^
- `reset the correct streams in log_model_for_deployment() instead of model_api over and over again
  <https://github.com/VertaAI/modeldb-client/commit/d12fb6b>`__


v0.11.3 (2019-05-31)
--------------------

New Features
^^^^^^^^^^^^
- `implement __version__ attribute on package
  <https://github.com/VertaAI/modeldb-client/commit/31aee4b>`__

Bug Fixes
^^^^^^^^^
- `remove unsupported dependency on pandas and NumPy in utils module
  <https://github.com/VertaAI/modeldb-client/commit/659ceca>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `move package version string from verta/setup.py to verta/verta/__about__.py
  <https://github.com/VertaAI/modeldb-client/commit/31aee4b>`__
- `remove old model API tests that have been superseded by property-based tests
  <https://github.com/VertaAI/modeldb-client/commit/4a0c799>`__
- `add pandas as a testing dependency
  <https://github.com/VertaAI/modeldb-client/commit/cc47d85>`__


v0.11.2 (2019-05-30)
--------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- `Parameters for client.set_* functions have been renamed to name and id, from e.g. proj_name and
  _proj_id.
  <https://github.com/VertaAI/modeldb-client/commit/889130d>`__
- `The _id attribute of Project, Experiment, and ExperimentRun have been renamed to id.
  <https://github.com/VertaAI/modeldb-client/commit/eb832fb>`__
- `The default generated names for Project, Experiment, and ExperimentRun have been shortened.
  <https://github.com/VertaAI/modeldb-client/commit/3e515ab>`__

Bug Fixes
^^^^^^^^^
- `fix typos in client.set_* error messages
  <https://github.com/VertaAI/modeldb-client/commit/0b8e4f9>`__


v0.11.1 (2019-05-29)
--------------------

Bug Fixes
^^^^^^^^^
- `fix internal utility get_env_dependencies() for compatibility with Python 3.6 and earlier
  <https://github.com/VertaAI/modeldb-client/commit/03b4005>`__


v0.11.0 (2019-05-29)
--------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- `log_model_for_deployment() now no longer requires a dataset argument, but requires a model API
  argument. The order of parameters has changed, and dataset_csv has been renamed to dataset_df.
  <https://github.com/VertaAI/modeldb-client/pull/99>`__

New Features
^^^^^^^^^^^^
- `implement ModelAPI utility class for generating model APIs
  <https://github.com/VertaAI/modeldb-client/pull/102>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `create an example notebook that downloads our beloved Census data with wget
  <https://github.com/VertaAI/modeldb-client/blob/b998b6b/workflows/examples-without-verta/notebooks/sklearn-census.ipynb>`__
- `rename the "scikit" model type to "sklearn"
  <https://github.com/VertaAI/modeldb-client/pull/102>`__
- `delete old internal model API generation utility
  <https://github.com/VertaAI/modeldb-client/pull/102>`__
- `update demo utility predict function to simply dump the JSON input into the request body
  <https://github.com/VertaAI/modeldb-client/commit/094494d#diff-5ecfc26>`__
- `implement internal utility to check for exact version pins in a requirements.txt
  <https://github.com/VertaAI/modeldb-client/pull/100>`__
- `implement internal utility to obtain the local environment's Python version number
  <https://github.com/VertaAI/modeldb-client/pull/98>`__
- `update READMEs
  <https://github.com/VertaAI/modeldb-client/commit/f0579f2>`__
- `add utils module to API reference
  <https://github.com/VertaAI/modeldb-client/commit/f83a203>`__
- `implement tests for model API generation
  <https://github.com/VertaAI/modeldb-client/commit/5982221>`__
- `implement property-based tests for model API generation
  <https://github.com/VertaAI/modeldb-client/commit/d3e2a58>`__
- `add deepdiff to testing requirements
  <https://github.com/VertaAI/modeldb-client/commit/4edf10b>`__
- `add hypothesis to testing requirements
  <https://github.com/VertaAI/modeldb-client/commit/8044b6a>`__


v0.10.2 (2019-05-22)
--------------------
no functional changes


v0.10.1 (2019-05-22)
--------------------

Bug Fixes
^^^^^^^^^
- `properly expose intermediate subpackages for compatibility with Python 3.2 and earlier
  <https://github.com/VertaAI/modeldb-client/commit/d3037ac>`__


v0.10.0 (2019-05-16)
--------------------

Backwards Incompatibilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- `log_hyperparameters() now must take a single, whole dictionary as an argument and no longer accepts
  dictionary unpacking.
  <https://github.com/VertaAI/modeldb-client/pull/96>`__
- `Getting observations from an ExperimentRun now returns tuples pairing observations with their
  timestamps.
  <https://github.com/VertaAI/modeldb-client/pull/83>`__
- `Passing a string into artifact logging functions now attempts to open a file located at the path
  represented by that string, rather than simply logging the string itself.
  <https://github.com/VertaAI/modeldb-client/pull/94>`__
- `Attempting to log an unsupported datatype now throws a TypeError instead of a ValueError.
  <https://github.com/VertaAI/modeldb-client/pull/90/files>`__
- `Logging artifacts now uses cloudpickle by default, instead of pickle.
  <https://github.com/VertaAI/modeldb-client/pull/90/files>`__
- `The internal logic for getting a Project by name has changed, and will be incompatible with old
  versions of the Verta Back End.
  <https://github.com/VertaAI/modeldb-client/commit/595b707>`__
- `The internal logic for handling uploading custom models for deployment has changed, and will be
  incompatible with old versions of the Verta Back End.
  <https://github.com/VertaAI/modeldb-client/pull/93>`__
- `The internal logic for getting an ExperimentRun by name has changed, and may be incompatible with
  old versions of the Verta Back End.
  <https://github.com/VertaAI/modeldb-client/pull/89>`__

New Features
^^^^^^^^^^^^
- `associate user-specified or automatically-generated timestamps with observations
  <https://github.com/VertaAI/modeldb-client/pull/83>`__
- `implement methods on ExperimentRun for logging and getting tags
  <https://github.com/VertaAI/modeldb-client/pull/84/files>`__
- `implement methods on ExperimentRun for logging multiple attributes, metrics, or hyperparameters
  in a single transaction
  <https://github.com/VertaAI/modeldb-client/pull/87>`__
- `enable uploading custom model APIs for deployment
  <https://github.com/VertaAI/modeldb-client/pull/91>`__
- `create functions specifically for logging artifact paths without attempting uploads
  <https://github.com/VertaAI/modeldb-client/pull/94>`__

Bug Fixes
^^^^^^^^^
- `reset stream pointer on failed deserialization attempts
  <https://github.com/VertaAI/modeldb-client/pull/86>`__

Internal Changes
^^^^^^^^^^^^^^^^
- `convert pandas DataFrames into CSVs when logging for deployment for data monitoring
  <https://github.com/VertaAI/modeldb-client/pull/85>`__
- `implement a secondary predict function in demo utilities that returns the raw HTML response instead
  of a formatted response
  <https://github.com/VertaAI/modeldb-client/pull/92>`__
- `move our example notebooks from workflows/demos/ to workflows/examples/
  <https://github.com/VertaAI/modeldb-client/commit/de197f6>`__
- `change "unknown" model type to "custom" in model API
  <https://github.com/VertaAI/modeldb-client/pull/93>`__
- `add "keras" deserialization in model API
  <https://github.com/VertaAI/modeldb-client/pull/93>`__
- `add cloudpickle to requirements with the locally pinned version if it was used when logging for
  deployment
  <https://github.com/VertaAI/modeldb-client/pull/95>`__
- `implement handful of small fixes to maintain Python 2.7 compatibility
  <https://github.com/VertaAI/modeldb-client/pull/97>`__
