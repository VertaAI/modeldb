# -*- coding: utf-8 -*-

from __future__ import print_function

from verta._internal_utils._utils import check_unnecessary_params_warning
from verta.tracking import _Context
from verta.tracking.entities import _entity
from verta._internal_utils import _artifact_utils, _utils, arg_handler, model_validator

from verta._protos.public.common import CommonService_pb2 as _CommonCommonService
from verta._protos.public.registry import RegistryService_pb2 as _RegistryService

from .. import _constants, VertaModelBase
from ._modelversion import RegisteredModelVersion
from ._modelversions import RegisteredModelVersions
from .. import task_type as task_type_module
from .. import data_type as data_type_module
from .. import check_model_dependencies as check_model_dependencies_fn


class RegisteredModel(_entity._ModelDBEntity):
    """
    Object representing a registered model.

    There should not be a need to instantiate this class directly; please use
    :meth:`Client.get_or_create_registered_model()
    <verta.Client.get_or_create_registered_model>`

    Attributes
    ----------
    id : int
        ID of this Registered Model.
    name : str
        Name of this Registered Model.
    url : str
        Verta web app URL.
    versions : iterable of :class:`~verta.registry.entities.RegisteredModelVersion`
        Versions of this RegisteredModel.
    pii: bool
         Whether the registered_model ingests personally identifiable information.

    """

    def __init__(self, conn, conf, msg):
        super(RegisteredModel, self).__init__(
            conn, conf, _RegistryService, "registered_model", msg
        )

    def __repr__(self):
        self._refresh_cache()
        msg = self._msg

        return "\n".join(
            (
                "name: {}".format(msg.name),
                "url: {}".format(self.url),
                "time created: {}".format(
                    _utils.timestamp_to_str(int(msg.time_created))
                ),
                "time updated: {}".format(
                    _utils.timestamp_to_str(int(msg.time_updated))
                ),
                "description: {}".format(msg.description),
                "labels: {}".format(msg.labels),
                "id: {}".format(msg.id),
                "pii: {}".format(msg.pii),
            )
        )

    @property
    def name(self):
        self._refresh_cache()
        return self._msg.name

    @property
    def url(self):
        return "{}://{}/{}/registry/{}".format(
            self._conn.scheme,
            self._conn.socket,
            self.workspace,
            self.id,
        )

    @property
    def workspace(self):
        self._refresh_cache()

        if self._msg.workspace_id:
            return self._conn.get_workspace_name_from_id(self._msg.workspace_id)
        else:
            return self._conn._OSS_DEFAULT_WORKSPACE

    def get_or_create_version(
        self,
        name=None,
        desc=None,
        labels=None,
        attrs=None,
        time_created=None,
        lock_level=None,
        id=None,
        input_description=None,
        hide_input_label=False,
        output_description=None,
        hide_output_label=False,
    ):
        """
        Gets or creates a Model Version.

        If an accessible Model Version with name `name` does not already exist under this
        Registered Model, it will be created and initialized with specified metadata
        parameters. If such a Model Version does already exist, it will be retrieved;
        specifying metadata parameters in this case will raise a warning.

        Parameters
        ----------
        name : str, optional
            Name of the Model Version. If no name is provided, one will be generated.
        desc : str, optional
            Description of the Model Version.
        labels : list of str, optional
            Labels of the Model Version.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the Model Version.
        lock_level : :mod:`~verta.registry.lock`, default :class:`~verta.registry.lock.Open`
            Lock level to set when creating this model version.
        id : str, optional
            ID of the Model Version. This parameter cannot be provided alongside `name`, and other
            parameters will be ignored.
        input_description : str, optional
            Description of the model version's input.
        hide_input_label : bool, default False
            Whether to hide the model version's input label.
        output_description : str, optional
            Description of the model version's output.
        hide_output_label : bool, default False
            Whether to hide the model version's output label.

        Returns
        -------
        :class:`~verta.registry.entities.RegisteredModelVersion`

        Raises
        ------
        ValueError
            If `name` and `id` are both passed in.

        """
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")

        resource_name = "Model Version"
        param_names = "`desc`, `labels`, `attrs`, `time_created`, `lock_level`, `input_description`, `hide_input_label`, `output_description`, or `hide_output_label`"
        params = (
            desc,
            labels,
            attrs,
            time_created,
            lock_level,
            input_description,
            hide_input_label,
            output_description,
            hide_output_label,
        )
        if id is not None:
            check_unnecessary_params_warning(
                resource_name, "id {}".format(id), param_names, params
            )
            return RegisteredModelVersion._get_by_id(self._conn, self._conf, id)
        else:
            ctx = _Context(self._conn, self._conf)
            ctx.registered_model = self
            return RegisteredModelVersion._get_or_create_by_name(
                self._conn,
                name,
                lambda name: RegisteredModelVersion._get_by_name(
                    self._conn, self._conf, name, self.id
                ),
                lambda name: RegisteredModelVersion._create(
                    self._conn,
                    self._conf,
                    ctx,
                    name=name,
                    desc=desc,
                    tags=labels,
                    attrs=attrs,
                    date_created=time_created,
                    lock_level=lock_level,
                    input_description=input_description,
                    hide_input_label=hide_input_label,
                    output_description=output_description,
                    hide_output_label=hide_output_label,
                ),
                lambda: check_unnecessary_params_warning(
                    resource_name,
                    "name {}".format(name),
                    param_names,
                    params,
                ),
            )

    def set_version(self, *args, **kwargs):
        """
        Alias for :meth:`RegisteredModel.get_or_create_version()`.

        """
        return self.get_or_create_version(*args, **kwargs)

    def create_version(
        self,
        name=None,
        desc=None,
        labels=None,
        attrs=None,
        time_created=None,
        lock_level=None,
        input_description=None,
        hide_input_label=False,
        output_description=None,
        hide_output_label=False,
    ):
        """
        Creates a model registry entry.

        Parameters
        ----------
        name : str, optional
            Name of the Model Version. If no name is provided, one will be generated.
        desc : str, optional
            Description of the Model Version.
        labels : list of str, optional
            Labels of the Model Version.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the Model Version.
        lock_level : :mod:`~verta.registry.lock`, default :class:`~verta.registry.lock.Open`
            Lock level to set when creating this model version.
        input_description : str, optional
            Description of the model version's input.
        hide_input_label : bool, default False
            Whether to hide the model version's input label.
        output_description : str, optional
            Description of the model version's output.
        hide_output_label : bool, default False
            Whether to hide the model version's output label.

        Returns
        -------
        :class:`~verta.registry.entities.RegisteredModelVersion`

        """
        ctx = _Context(self._conn, self._conf)
        ctx.registered_model = self
        return RegisteredModelVersion._create(
            self._conn,
            self._conf,
            ctx,
            name=name,
            desc=desc,
            tags=labels,
            attrs=attrs,
            date_created=time_created,
            lock_level=lock_level,
            input_description=input_description,
            hide_input_label=hide_input_label,
            output_description=output_description,
            hide_output_label=hide_output_label,
        )

    def _create_standard_model_from_spec(
        self,
        model,
        environment,
        code_dependencies=None,
        model_api=None,
        artifacts=None,
        name=None,
        desc=None,
        labels=None,
        attrs=None,
        lock_level=None,
        input_description=None,
        hide_input_label=False,
        output_description=None,
        hide_output_label=False,
    ):
        artifacts = artifacts or {}
        for key in artifacts.keys():
            _artifact_utils.validate_key(key)
        attrs = attrs or {}
        attrs.update(
            {
                _constants.MODEL_LANGUAGE_ATTR_KEY: _constants.ModelLanguage.PYTHON,
                _constants.MODEL_TYPE_ATTR_KEY: _constants.ModelType.STANDARD_VERTA_MODEL,
            }
        )

        model_ver = self.create_version(
            name=name,
            desc=desc,
            labels=labels,
            attrs=attrs,
            lock_level=lock_level,
            input_description=input_description,
            hide_input_label=hide_input_label,
            output_description=output_description,
            hide_output_label=hide_output_label,
        )
        try:
            for key, artifact in artifacts.items():
                model_ver.log_artifact(key, artifact)
            model_ver.log_model(
                model=model,
                custom_modules=code_dependencies,
                model_api=model_api,
                artifacts=list(artifacts.keys()),
            )
            model_ver.log_environment(environment)
        except Exception as e:
            model_ver.delete()
            raise e

        return model_ver

    def create_standard_model(
        self,
        model_cls,
        environment,
        code_dependencies=None,
        model_api=None,
        artifacts=None,
        name=None,
        desc=None,
        labels=None,
        attrs=None,
        lock_level=None,
        input_description=None,
        hide_input_label=False,
        output_description=None,
        hide_output_label=False,
        check_model_dependencies=False,
    ):
        """Create a Standard Verta Model version from a Verta Model Specification.

        .. versionadded:: 0.22.2
            The `check_model_dependencies` parameter.
        .. versionadded:: 0.18.2

        .. note::

            The following artifact keys are reserved for internal use within the
            Verta system:

            - ``"custom_modules"``
            - ``"model"``
            - ``"model.pkl"``
            - ``"model_api.json"``
            - ``"requirements.txt"``
            - ``"train_data"``
            - ``"tf_saved_model"``
            - ``"setup_script"``

        .. note::

            If using an XGBoost model from their scikit-learn API,
            ``"scikit-learn"`` must also be specified in `environment`
            (in addition to ``"xgboost"``).

        Parameters
        ----------
        model_cls : subclass of :class:`~verta.registry.VertaModelBase`
            Model class that implements ``VertaModelBase``.
        environment : :class:`~verta.environment.Python`
            pip and apt dependencies.
        code_dependencies : list of str, optional
            Paths to local Python code files that `model_cls` depends on. This
            parameter has the same behavior as ``custom_modules`` in
            :meth:`RegisteredModelVersion.log_model`.
        model_api : :class:`~verta.utils.ModelAPI`, optional
            Model API specifying the model's expected input and output
        artifacts : dict of str to obj
            A mapping from artifact keys to artifacts. These will be logged
            and uploaded, then provided to the model when deployed.
        name : str, optional
            Name of the model version. If no name is provided, one will be
            generated.
        desc : str, optional
            Description of the model version.
        labels : list of str, optional
            Labels of the model version.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the model version.
        lock_level : :mod:`~verta.registry.lock`, default :class:`~verta.registry.lock.Open`
            Lock level to set when creating this model version.
        input_description : str, optional
            Description of the model version's input.
        hide_input_label : bool, default False
            Whether to hide the model version's input label.
        output_description : str, optional
            Description of the model version's output.
        hide_output_label : bool, default False
            Whether to hide the model version's output label.
        check_model_dependencies : bool, default False
            Whether to verify the model's dependencies are specified in the environment
            and raise an exception if any are missing.

        Raises
        ------
        RuntimeError
            If `check_model_dependencies` is ``True`` and any dependencies detected in
            the model class are not specified in the environment.

        Returns
        -------
        :class:`~verta.registry.entities.RegisteredModelVersion`

        Examples
        --------
        .. code-block:: python

            from verta.environment import Python
            from verta.registry import VertaModelBase

            class VertaModel(VertaModelBase):
                def __init__(self, artifacts):
                    import pickle

                    with open(artifacts["weights"], "rb") as f:
                        self.weights = pickle.load(f)

                def predict(self, input):
                    import numpy as np

                    return np.matmul(self.weights, input)

            model_ver = reg_model.create_standard_model(
                VertaModel,
                Python(["numpy"]),
                artifacts={"weights": np.array(weights)},
            )
            endpoint.update(model_ver, wait=True)
            endpoint.get_deployed_model().predict(input)

        """
        model_validator.must_verta(model_cls)

        if check_model_dependencies:
            check_model_dependencies_fn(
                model_cls=model_cls, environment=environment, raise_for_missing=True
            )

        return self._create_standard_model_from_spec(
            model=model_cls,
            environment=environment,
            code_dependencies=code_dependencies,
            model_api=model_api,
            artifacts=artifacts,
            name=name,
            desc=desc,
            labels=labels,
            attrs=attrs,
            lock_level=lock_level,
            input_description=input_description,
            hide_input_label=hide_input_label,
            output_description=output_description,
            hide_output_label=hide_output_label,
        )

    def create_standard_model_from_keras(
        self,
        obj,
        environment,
        model_api=None,
        name=None,
        desc=None,
        labels=None,
        attrs=None,
        lock_level=None,
        input_description=None,
        hide_input_label=False,
        output_description=None,
        hide_output_label=False,
    ):
        """Create a Standard Verta Model version from a TensorFlow-backend Keras model.

        .. versionadded:: 0.18.2

        Parameters
        ----------
        obj : `tf.keras.Sequential <https://keras.io/guides/sequential_model/>`__ or `functional API keras.Model <https://keras.io/guides/functional_api/>`__
            Keras model.
        environment : :class:`~verta.environment.Python`
            pip and apt dependencies.
        model_api : :class:`~verta.utils.ModelAPI`, optional
            Model API specifying the model's expected input and output
        name : str, optional
            Name of the model version. If no name is provided, one will be
            generated.
        desc : str, optional
            Description of the model version.
        labels : list of str, optional
            Labels of the model version.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the model version.
        lock_level : :mod:`~verta.registry.lock`, default :class:`~verta.registry.lock.Open`
            Lock level to set when creating this model version.
        input_description : str, optional
            Description of the model version's input.
        hide_input_label : bool, default False
            Whether to hide the model version's input label.
        output_description : str, optional
            Description of the model version's output.
        hide_output_label : bool, default False
            Whether to hide the model version's output label.

        Returns
        -------
        :class:`~verta.registry.entities.RegisteredModelVersion`

        Examples
        --------
        .. code-block:: python

            from tensorflow import keras
            from verta.environment import Python

            inputs = keras.Input(shape=(3,))
            x = keras.layers.Dense(2, activation="relu")(inputs)
            outputs = keras.layers.Dense(1, activation="sigmoid")(x)
            model = keras.Model(inputs=inputs, outputs=outputs)
            train(model, data)

            model_ver = reg_model.create_standard_model_from_keras(
                model,
                Python(["tensorflow"]),
            )
            endpoint.update(model_ver, wait=True)
            endpoint.get_deployed_model().predict(input)

        """
        model_validator.must_keras(obj)

        return self._create_standard_model_from_spec(
            model=obj,
            environment=environment,
            model_api=model_api,
            name=name,
            desc=desc,
            labels=labels,
            attrs=attrs,
            lock_level=lock_level,
            input_description=input_description,
            hide_input_label=hide_input_label,
            output_description=output_description,
            hide_output_label=hide_output_label,
        )

    def create_standard_model_from_sklearn(
        self,
        obj,
        environment,
        model_api=None,
        name=None,
        desc=None,
        labels=None,
        attrs=None,
        lock_level=None,
        input_description=None,
        hide_input_label=False,
        output_description=None,
        hide_output_label=False,
    ):
        """Create a Standard Verta Model version from a scikit-learn model.

        .. versionadded:: 0.18.2

        Parameters
        ----------
        obj : `sklearn.base.BaseEstimator <https://scikit-learn.org/stable/modules/generated/sklearn.base.BaseEstimator.html>`__
            scikit-learn model.
        environment : :class:`~verta.environment.Python`
            pip and apt dependencies.
        model_api : :class:`~verta.utils.ModelAPI`, optional
            Model API specifying the model's expected input and output
        name : str, optional
            Name of the model version. If no name is provided, one will be
            generated.
        desc : str, optional
            Description of the model version.
        labels : list of str, optional
            Labels of the model version.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the model version.
        lock_level : :mod:`~verta.registry.lock`, default :class:`~verta.registry.lock.Open`
            Lock level to set when creating this model version.
        input_description : str, optional
            Description of the model version's input.
        hide_input_label : bool, default False
            Whether to hide the model version's input label.
        output_description : str, optional
            Description of the model version's output.
        hide_output_label : bool, default False
            Whether to hide the model version's output label.

        Returns
        -------
        :class:`~verta.registry.entities.RegisteredModelVersion`

        Examples
        --------
        .. code-block:: python

            from sklearn.svm import LinearSVC
            from verta.environment import Python

            model = LinearSVC(**hyperparams)
            model.fit(X_train, y_train)

            model_ver = reg_model.create_standard_model_from_sklearn(
                model,
                Python(["scikit-learn"]),
            )
            endpoint.update(model_ver, wait=True)
            endpoint.get_deployed_model().predict(input)

        """
        model_validator.must_sklearn(obj)

        return self._create_standard_model_from_spec(
            model=obj,
            environment=environment,
            model_api=model_api,
            name=name,
            desc=desc,
            labels=labels,
            attrs=attrs,
            lock_level=lock_level,
            input_description=input_description,
            hide_input_label=hide_input_label,
            output_description=output_description,
            hide_output_label=hide_output_label,
        )

    def create_standard_model_from_torch(
        self,
        obj,
        environment,
        model_api=None,
        name=None,
        desc=None,
        labels=None,
        attrs=None,
        lock_level=None,
        input_description=None,
        hide_input_label=False,
        output_description=None,
        hide_output_label=False,
    ):
        """Create a Standard Verta Model version from a PyTorch model.

        .. versionadded:: 0.18.2

        Parameters
        ----------
        obj : `torch.nn.Module <https://pytorch.org/docs/stable/generated/torch.nn.Module.html>`__
            PyTorch model.
        environment : :class:`~verta.environment.Python`
            pip and apt dependencies.
        model_api : :class:`~verta.utils.ModelAPI`, optional
            Model API specifying the model's expected input and output
        name : str, optional
            Name of the model version. If no name is provided, one will be
            generated.
        desc : str, optional
            Description of the model version.
        labels : list of str, optional
            Labels of the model version.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the model version.
        lock_level : :mod:`~verta.registry.lock`, default :class:`~verta.registry.lock.Open`
            Lock level to set when creating this model version.
        input_description : str, optional
            Description of the model version's input.
        hide_input_label : bool, default False
            Whether to hide the model version's input label.
        output_description : str, optional
            Description of the model version's output.
        hide_output_label : bool, default False
            Whether to hide the model version's output label.

        Returns
        -------
        :class:`~verta.registry.entities.RegisteredModelVersion`

        Examples
        --------
        .. code-block:: python

            import torch
            from verta.environment import Python

            class Model(torch.nn.Module):
                def __init__(self):
                    super(Model, self).__init__()
                    self.layer1 = torch.nn.Linear(3, 2)
                    self.layer2 = torch.nn.Linear(2, 1)

                def forward(self, x):
                    x = torch.nn.functional.relu(self.layer1(x))
                    return torch.sigmoid(self.layer2(x))

            model = Model()
            train(model, data)

            model_ver = reg_model.create_standard_model_from_torch(
                model,
                Python(["torch"]),
            )
            endpoint.update(model_ver, wait=True)
            endpoint.get_deployed_model().predict(input)

        """
        model_validator.must_torch(obj)

        return self._create_standard_model_from_spec(
            model=obj,
            environment=environment,
            model_api=model_api,
            name=name,
            desc=desc,
            labels=labels,
            attrs=attrs,
            lock_level=lock_level,
            input_description=input_description,
            hide_input_label=hide_input_label,
            output_description=output_description,
            hide_output_label=hide_output_label,
        )

    def create_standard_model_from_xgboost(
        self,
        obj,
        environment,
        model_api=None,
        name=None,
        desc=None,
        labels=None,
        attrs=None,
        lock_level=None,
        input_description=None,
        hide_input_label=False,
        output_description=None,
        hide_output_label=False,
    ):
        """Create a Standard Verta Model version from an XGBoost model.

        .. versionadded:: 0.18.2

        .. note::

            If using an XGBoost model from their scikit-learn API,
            ``"scikit-learn"`` must also be specified in `environment`
            (in addition to ``"xgboost"``).

        Parameters
        ----------
        obj : `xgboost.sklearn.XGBModel <https://xgboost.readthedocs.io/en/latest/python/python_api.html#module-xgboost.sklearn>`__
            XGBoost model using their scikit-learn wrapper interface.
        environment : :class:`~verta.environment.Python`
            pip and apt dependencies.
        model_api : :class:`~verta.utils.ModelAPI`, optional
            Model API specifying the model's expected input and output
        name : str, optional
            Name of the model version. If no name is provided, one will be
            generated.
        desc : str, optional
            Description of the model version.
        labels : list of str, optional
            Labels of the model version.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the model version.
        lock_level : :mod:`~verta.registry.lock`, default :class:`~verta.registry.lock.Open`
            Lock level to set when creating this model version.
        input_description : str, optional
            Description of the model version's input.
        hide_input_label : bool, default False
            Whether to hide the model version's input label.
        output_description : str, optional
            Description of the model version's output.
        hide_output_label : bool, default False
            Whether to hide the model version's output label.

        Returns
        -------
        :class:`~verta.registry.entities.RegisteredModelVersion`

        Examples
        --------
        .. code-block:: python

            import xgboost as xgb
            from verta.environment import Python

            model = xgb.XGBClassifier(**hyperparams)
            model.fit(X_train, y_train)

            model_ver = reg_model.create_standard_model_from_xgboost(
                model,
                Python(["scikit-learn", "xgboost"]),
            )
            endpoint.update(model_ver, wait=True)
            endpoint.get_deployed_model().predict(input)

        """
        model_validator.must_xgboost_sklearn(obj)

        return self._create_standard_model_from_spec(
            model=obj,
            environment=environment,
            model_api=model_api,
            name=name,
            desc=desc,
            labels=labels,
            attrs=attrs,
            lock_level=lock_level,
            input_description=input_description,
            hide_input_label=hide_input_label,
            output_description=output_description,
            hide_output_label=hide_output_label,
        )

    def create_containerized_model(
        self,
        docker_image,
        model_api=None,
        name=None,
        desc=None,
        labels=None,
        attrs=None,
        lock_level=None,
        input_description=None,
        hide_input_label=False,
        output_description=None,
        hide_output_label=False,
    ):
        """Create a Containerized Model version from a Docker image.

        .. versionadded:: 0.20.0

        .. note::

            |experimental|

        Parameters
        ----------
        docker_image : :class:`~verta.registry.DockerImage`
            Docker image information.
        model_api : :class:`~verta.utils.ModelAPI`, optional
            Model API specifying the model's expected input and output
        name : str, optional
            Name of the model version. If no name is provided, one will be
            generated.
        desc : str, optional
            Description of the model version.
        labels : list of str, optional
            Labels of the model version.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the model version.
        lock_level : :mod:`~verta.registry.lock`, default :class:`~verta.registry.lock.Open`
            Lock level to set when creating this model version.
        input_description : str, optional
            Description of the model version's input.
        hide_input_label : bool, default False
            Whether to hide the model version's input label.
        output_description : str, optional
            Description of the model version's output.
        hide_output_label : bool, default False
            Whether to hide the model version's output label.

        Returns
        -------
        :class:`~verta.registry.entities.RegisteredModelVersion`

        Examples
        --------
        .. code-block:: python

            from verta.registry import DockerImage

            docker_image = DockerImage(
                port=5000,
                request_path="/predict_json",
                health_path="/health",

                repository="012345678901.dkr.ecr.apne2-az1.amazonaws.com/models/example",
                tag="example",

                env_vars={"CUDA_VISIBLE_DEVICES": "0,1"},
            )

            model_ver = reg_model.create_containerized_model(
                docker_image,
            )
            endpoint.update(model_ver, wait=True)
            endpoint.get_deployed_model().predict(input)

        """
        attrs = attrs or {}
        attrs.update(
            {
                _constants.MODEL_LANGUAGE_ATTR_KEY: _constants.ModelLanguage.UNKNOWN,
                _constants.MODEL_TYPE_ATTR_KEY: _constants.ModelType.USER_CONTAINERIZED_MODEL,
            }
        )

        model_ver = self.create_version(
            name=name,
            desc=desc,
            labels=labels,
            attrs=attrs,
            lock_level=lock_level,
            input_description=input_description,
            hide_input_label=hide_input_label,
            output_description=output_description,
            hide_output_label=hide_output_label,
        )
        try:
            model_ver.log_docker(
                docker_image=docker_image,
                model_api=model_api,
            )
        except Exception as e:
            model_ver.delete()
            raise e

        return model_ver

    def create_version_from_run(self, run_id, name=None, lock_level=None):
        """Create a model version copied from an experiment run.

        Parameters
        ----------
        run_id : str or :class:`~verta.tracking.entities.ExperimentRun`
            Run from which to create the model version.
        name : str, optional
            Name of the model version. If no name is provided, one will be generated.
        lock_level : :mod:`~verta.registry.lock`, default :class:`~verta.registry.lock.Open`
            Lock level to set when creating this model version.

        Returns
        -------
        :class:`~verta.registry.entities.RegisteredModelVersion`

        """
        run_id = arg_handler.extract_id(run_id)

        ctx = _Context(self._conn, self._conf)
        ctx.registered_model = self
        return RegisteredModelVersion._create(
            self._conn,
            self._conf,
            ctx,
            name=name,
            experiment_run_id=run_id,
            lock_level=lock_level,
        )

    def get_version(self, name=None, id=None):
        """
        Gets a Model Version of this Registered Model by `name` or `id`

        Parameters
        ----------
        name : str, optional
            Name of the Model Version. If no name is provided, one will be generated.
        id : str, optional
            ID of the Model Version. This parameter cannot be provided alongside `name`, and other
            parameters will be ignored.

        Returns
        -------
        :class:`~verta.registry.entities.RegisteredModelVersion`

        """
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")
        if name is None and id is None:
            raise ValueError("must specify either `name` or `id`")

        if id is not None:
            version = RegisteredModelVersion._get_by_id(self._conn, self._conf, id)
        else:
            version = RegisteredModelVersion._get_by_name(
                self._conn, self._conf, name, self.id
            )
        if version is None:
            raise ValueError("Registered model version not found")
        return version

    @property
    def versions(self):
        return RegisteredModelVersions(self._conn, self._conf).with_model(self)

    @classmethod
    def _generate_default_name(cls):
        return "Model {}".format(_utils.generate_default_name())

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        Message = _RegistryService.GetRegisteredModelRequest
        response = conn.make_proto_request(
            "GET", "/api/v1/registry/registered_models/{}".format(id)
        )
        return conn.maybe_proto_response(response, Message.Response).registered_model

    @classmethod
    def _get_proto_by_name(cls, conn, name, workspace):
        Message = _RegistryService.GetRegisteredModelRequest
        response = conn.make_proto_request(
            "GET",
            "/api/v1/registry/workspaces/{}/registered_models/{}".format(
                workspace, name
            ),
        )
        return conn.maybe_proto_response(response, Message.Response).registered_model

    @classmethod
    def _create_proto_internal(
        cls,
        conn,
        ctx,
        name,
        desc=None,
        tags=None,
        attrs=None,
        date_created=None,
        public_within_org=None,
        visibility=None,
        task_type=None,
        data_type=None,
        pii=None,
    ):
        if task_type is None:
            task_type = task_type_module._Unknown()
        if data_type is None:
            data_type = data_type_module._Unknown()
        Message = _RegistryService.RegisteredModel
        msg = Message(
            name=name,
            description=desc,
            labels=tags,
            time_created=date_created,
            time_updated=date_created,
            task_type=task_type._as_proto(),
            data_type=data_type._as_proto(),
            pii=pii,
        )
        if (
            public_within_org
            and ctx.workspace_name is not None  # not user's personal workspace
            and _utils.is_org(ctx.workspace_name, conn)
        ):  # not anyone's personal workspace
            msg.visibility = _CommonCommonService.VisibilityEnum.ORG_SCOPED_PUBLIC
        msg.custom_permission.CopyFrom(visibility._custom_permission)
        msg.resource_visibility = visibility._visibility

        response = conn.make_proto_request(
            "POST",
            "/api/v1/registry/workspaces/{}/registered_models".format(
                ctx.workspace_name
            ),
            body=msg,
        )
        registered_model = conn.must_proto_response(
            response, _RegistryService.SetRegisteredModel.Response
        ).registered_model

        if ctx.workspace_name is not None:
            WORKSPACE_PRINT_MSG = "workspace: {}".format(ctx.workspace_name)
        else:
            WORKSPACE_PRINT_MSG = "personal workspace"

        print(
            "created new RegisteredModel: {} in {}".format(
                registered_model.name, WORKSPACE_PRINT_MSG
            )
        )
        return registered_model

    RegisteredModelMessage = _RegistryService.RegisteredModel

    def set_pii(self, pii):
        """
        Updates the PII value of this Registered Model.


        Parameters
        ----------
        pii : bool
            ``True`` indicates that the model ingests personally identifiable information.

        Warnings
        --------
        You *MUST* update any live endpoints running this model in order to propagate this change.
        A simple no-op update is sufficient.
        """
        self._fetch_with_no_cache()
        self._msg.pii = pii
        self._update(self._msg, method="PUT")

    def get_pii(self):
        """
        Returns the PII value of this Registered Model.

        Returns
        -------
        bool
            ``True`` indicates that the model ingests personally identifiable information.

        """
        self._refresh_cache()
        return self._msg.pii

    def set_description(self, desc):
        if not desc:
            raise ValueError("desc is not specified")
        self._update(self.RegisteredModelMessage(description=desc))

    def get_description(self):
        self._refresh_cache()
        return self._msg.description

    def add_labels(self, labels):
        """
        Adds multiple labels to this Registered Model.

        Parameters
        ----------
        labels : list of str
            Labels to add.

        """
        if not labels:
            raise ValueError("label is not specified")

        self._update(self.RegisteredModelMessage(labels=labels))

    def add_label(self, label):
        """
        Adds a label to this Registered Model.

        Parameters
        ----------
        label : str
            Label to add.

        """
        if label is None:
            raise ValueError("label is not specified")
        self._update(self.RegisteredModelMessage(labels=[label]))

    def del_label(self, label):
        """
        Deletes a label from this Registered Model.

        Parameters
        ----------
        label : str
            Label to delete.

        """
        if label is None:
            raise ValueError("label is not specified")
        self._fetch_with_no_cache()
        if label in self._msg.labels:
            self._msg.labels.remove(label)
            self._update(self._msg, method="PUT")

    def get_labels(self):
        """
        Gets all labels of this Registered Model.

        Returns
        -------
        labels : list of str
            List of all labels of this Registered Model.

        """
        self._refresh_cache()
        return self._msg.labels

    def _update(self, msg, method="PATCH"):
        response = self._conn.make_proto_request(
            method,
            "/api/v1/registry/registered_models/{}".format(self.id),
            body=msg,
            include_default=False,
        )
        self._conn.must_proto_response(
            response, _RegistryService.SetRegisteredModel.Response
        )
        self._clear_cache()

    def _get_info_list(self):
        return [
            self._msg.name,
            str(self.id),
            _utils.timestamp_to_str(self._msg.time_updated),
        ]

    def delete(self):
        """
        Deletes this registered model.

        """
        request_url = "{}://{}/api/v1/registry/registered_models/{}".format(
            self._conn.scheme, self._conn.socket, self.id
        )
        response = _utils.make_request("DELETE", request_url, self._conn)
        _utils.raise_for_http_error(response)

    def set_task_type(self, task_type):
        """
        Sets this registered model task type

        Parameters
        ----------
        task_type : :mod:`~verta.registry.task_type`
            Task type to set.

        """
        if not isinstance(task_type, task_type_module._TaskType):
            raise ValueError(
                "`task_type` must be an object from verta.registry.task_type,"
                " not {}".format(type(task_type))
            )

        self._update(self.RegisteredModelMessage(task_type=task_type._as_proto()))

    def get_task_type(self):
        """
        Gets this registered model task type

        Returns
        -------
        task_type : :mod:`~verta.registry.task_type`
            This registered model task type.

        """
        self._refresh_cache()
        return task_type_module._TaskType._from_proto(self._msg.task_type)

    def set_data_type(self, data_type):
        """
        Sets this registered model data type

        Parameters
        ----------
        data_type : :mod:`~verta.registry.data_type`
            Data type to set.

        """
        if not isinstance(data_type, data_type_module._DataType):
            raise ValueError(
                "`data_type` must be an object from verta.registry.data_type,"
                " not {}".format(type(data_type))
            )

        self._update(self.RegisteredModelMessage(data_type=data_type._as_proto()))

    def get_data_type(self):
        """
        Gets this registered model data type

        Returns
        -------
        data_type : :mod:`~verta.registry.data_type`
            This registered model data type.

        """
        self._refresh_cache()
        return data_type_module._DataType._from_proto(self._msg.data_type)
