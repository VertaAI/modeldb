Release Notes
=============


.. This comment block is a template for version release notes.
   v.. (--)
   --------------------

   Backwards Incompatibilities
   ^^^^^^^^^^^^^^^^^^^^^^^^^^^
   - `
     <>`__

   Deprecations
   ^^^^^^^^^^^^
   - `
     <>`__

   New Features
   ^^^^^^^^^^^^
   - `
     <>`__

   Enhancements
   ^^^^^^^^^^^^
   - `
     <>`__

   Bug Fixes
   ^^^^^^^^^
   - `
     <>`__

   Internal Changes
   ^^^^^^^^^^^^^^^^
   - `
     <>`__


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
- `blacklist deployment artifact keys
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
- `support associating artifact dependencies with class models
  <https://github.com/VertaAI/modeldb-client/pull/299>`__
- `enable downloading artifacts into a local cache for use with class models
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
