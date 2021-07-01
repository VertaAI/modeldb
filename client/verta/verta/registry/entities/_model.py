# -*- coding: utf-8 -*-

from __future__ import print_function
import requests

from verta._internal_utils._utils import check_unnecessary_params_warning
from verta.tracking import _Context
from verta.tracking.entities import _entity
from verta._internal_utils import _utils, model_validator

from verta._protos.public.common import CommonService_pb2 as _CommonCommonService
from verta._protos.public.registry import RegistryService_pb2 as _RegistryService

from .. import VertaModelBase
from ._modelversion import RegisteredModelVersion
from ._modelversions import RegisteredModelVersions


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
    versions : iterable of :class:`~verta.registry.entities.RegisteredModelVersion`
        Versions of this RegisteredModel.

    """
    def __init__(self, conn, conf, msg):
        super(RegisteredModel, self).__init__(conn, conf, _RegistryService, "registered_model", msg)

    def __repr__(self):
        self._refresh_cache()
        msg = self._msg

        return '\n'.join((
            "name: {}".format(msg.name),
            "url: {}://{}/{}/registry/{}".format(self._conn.scheme, self._conn.socket, self.workspace, self.id),
            "time created: {}".format(_utils.timestamp_to_str(int(msg.time_created))),
            "time updated: {}".format(_utils.timestamp_to_str(int(msg.time_updated))),
            "description: {}".format(msg.description),
            "labels: {}".format(msg.labels),
            "id: {}".format(msg.id),
        ))

    @property
    def name(self):
        self._refresh_cache()
        return self._msg.name

    @property
    def workspace(self):
        self._refresh_cache()

        if self._msg.workspace_id:
            return self._conn.get_workspace_name_from_id(self._msg.workspace_id)
        else:
            return self._conn._OSS_DEFAULT_WORKSPACE

    def get_or_create_version(self, name=None, desc=None, labels=None, attrs=None, time_created=None, lock_level=None, id=None):
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
        param_names = "`desc`, `labels`, `attrs`, `time_created`, or `lock_level`"
        params = (desc, labels, attrs, time_created, lock_level)
        if id is not None:
            check_unnecessary_params_warning(resource_name, "id {}".format(id),
                                             param_names, params)
            return RegisteredModelVersion._get_by_id(self._conn, self._conf, id)
        else:
            ctx = _Context(self._conn, self._conf)
            ctx.registered_model = self
            return RegisteredModelVersion._get_or_create_by_name(
                self._conn, name,
                lambda name: RegisteredModelVersion._get_by_name(self._conn, self._conf, name, self.id),
                lambda name: RegisteredModelVersion._create(
                    self._conn, self._conf, ctx,
                    name=name, desc=desc, tags=labels, attrs=attrs,
                    date_created=time_created, lock_level=lock_level,
                ),
                lambda: check_unnecessary_params_warning(
                    resource_name,
                    "name {}".format(name),
                    param_names, params,
                )
            )

    def set_version(self, *args, **kwargs):
        """
        Alias for :meth:`RegisteredModel.get_or_create_version()`.

        """
        return self.get_or_create_version(*args, **kwargs)

    def create_version(self, name=None, desc=None, labels=None, attrs=None, time_created=None, lock_level=None):
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

        Returns
        -------
        :class:`~verta.registry.entities.RegisteredModelVersion`

        """
        ctx = _Context(self._conn, self._conf)
        ctx.registered_model = self
        return RegisteredModelVersion._create(
            self._conn, self._conf, ctx,
            name=name, desc=desc, tags=labels, attrs=attrs,
            date_created=time_created, lock_level=lock_level,
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
    ):
        artifacts = artifacts or {}
        attrs = attrs or {}
        attrs.update({
            "__verta_reserved__model_language": "Python",
            "__verta_reserved__model_type": "StandardVertaModel",
        })

        model_ver = self.create_version(
            name=name,
            desc=desc,
            labels=labels,
            attrs=attrs,
            lock_level=lock_level,
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
    ):
        """Create a Standard Verta Model version from a Verta Model Specification.

        .. versionadded:: 0.18.2

        Parameters
        ----------
        model_cls : subclass of :class:`~verta.registry.VertaModelBase`
            Model class that implements ``VertaModelBase``.
        environment : :class:`~verta.environment.Python`
            pip and apt dependencies.
        code_dependencies : list of str, optional
            Paths to local Python code files that `model_cls` depends on.
        model_api : :class:`~verta.utils.ModelAPI`
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
    ):
        """Create a Standard Verta Model version from a TensorFlow-backend Keras model.

        .. versionadded:: 0.18.2

        Parameters
        ----------
        obj : `tf.keras.Sequential <https://keras.io/guides/sequential_model/>`__ or `functional API keras.Model <https://keras.io/guides/functional_api/>`__
            Keras model.
        environment : :class:`~verta.environment.Python`
            pip and apt dependencies.
        model_api : :class:`~verta.utils.ModelAPI`
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
    ):
        """Create a Standard Verta Model version from a scikit-learn model.

        .. versionadded:: 0.18.2

        Parameters
        ----------
        obj : `sklearn.base.BaseEstimator <https://scikit-learn.org/stable/modules/generated/sklearn.base.BaseEstimator.html>`__
            scikit-learn model.
        environment : :class:`~verta.environment.Python`
            pip and apt dependencies.
        model_api : :class:`~verta.utils.ModelAPI`
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
    ):
        """Create a Standard Verta Model version from a PyTorch model.

        .. versionadded:: 0.18.2

        Parameters
        ----------
        obj : `torch.nn.Module <https://pytorch.org/docs/stable/generated/torch.nn.Module.html>`__
            PyTorch model.
        environment : :class:`~verta.environment.Python`
            pip and apt dependencies.
        model_api : :class:`~verta.utils.ModelAPI`
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
    ):
        """Create a Standard Verta Model version from an XGBoost model.

        .. versionadded:: 0.18.2

        Parameters
        ----------
        obj : `xgboost.sklearn.XGBModel <https://xgboost.readthedocs.io/en/latest/python/python_api.html#module-xgboost.sklearn>`__
            XGBoost model using their scikit-learn wrapper interface.
        environment : :class:`~verta.environment.Python`
            pip and apt dependencies.
        model_api : :class:`~verta.utils.ModelAPI`
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
        )

    def create_version_from_run(self, run_id, name=None, lock_level=None):
        """Create a model version copied from an experiment run.

        Parameters
        ----------
        run_id : str
            ID of the run from which to create the model version.
        name : str, optional
            Name of the model version. If no name is provided, one will be generated.
        lock_level : :mod:`~verta.registry.lock`, default :class:`~verta.registry.lock.Open`
            Lock level to set when creating this model version.

        Returns
        -------
        :class:`~verta.registry.entities.RegisteredModelVersion`

        """
        ctx = _Context(self._conn, self._conf)
        ctx.registered_model = self
        return RegisteredModelVersion._create(
            self._conn, self._conf, ctx,
            name=name, experiment_run_id=run_id, lock_level=lock_level,
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
            version = RegisteredModelVersion._get_by_name(self._conn, self._conf, name, self.id)
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
        response = conn.make_proto_request("GET",
                                           "/api/v1/registry/registered_models/{}".format(id))
        return conn.maybe_proto_response(response, Message.Response).registered_model

    @classmethod
    def _get_proto_by_name(cls, conn, name, workspace):
        Message = _RegistryService.GetRegisteredModelRequest
        response = conn.make_proto_request("GET",
                                           "/api/v1/registry/workspaces/{}/registered_models/{}".format(workspace, name))
        return conn.maybe_proto_response(response, Message.Response).registered_model

    @classmethod
    def _create_proto_internal(cls, conn, ctx, name, desc=None, tags=None, attrs=None, date_created=None, public_within_org=None, visibility=None):
        Message = _RegistryService.RegisteredModel
        msg = Message(name=name, description=desc, labels=tags, time_created=date_created, time_updated=date_created)
        if (public_within_org
                and ctx.workspace_name is not None  # not user's personal workspace
                and _utils.is_org(ctx.workspace_name, conn)):  # not anyone's personal workspace
            msg.visibility = _CommonCommonService.VisibilityEnum.ORG_SCOPED_PUBLIC
        msg.custom_permission.CopyFrom(visibility._custom_permission)
        msg.resource_visibility = visibility._visibility

        response = conn.make_proto_request("POST",
                                           "/api/v1/registry/workspaces/{}/registered_models".format(ctx.workspace_name),
                                           body=msg)
        registered_model = conn.must_proto_response(response, _RegistryService.SetRegisteredModel.Response).registered_model

        if ctx.workspace_name is not None:
            WORKSPACE_PRINT_MSG = "workspace: {}".format(ctx.workspace_name)
        else:
            WORKSPACE_PRINT_MSG = "personal workspace"

        print("created new RegisteredModel: {} in {}".format(registered_model.name, WORKSPACE_PRINT_MSG))
        return registered_model

    RegisteredModelMessage = _RegistryService.RegisteredModel

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
        response = self._conn.make_proto_request(method, "/api/v1/registry/registered_models/{}".format(self.id),
                                           body=msg, include_default=False)
        self._conn.must_proto_response(response, _RegistryService.SetRegisteredModel.Response)
        self._clear_cache()

    def _get_info_list(self):
        return [self._msg.name, str(self.id), _utils.timestamp_to_str(self._msg.time_updated)]

    def delete(self):
        """
        Deletes this registered model.

        """
        request_url = "{}://{}/api/v1/registry/registered_models/{}".format(self._conn.scheme, self._conn.socket, self.id)
        response = requests.delete(request_url, headers=self._conn.auth)
        _utils.raise_for_http_error(response)
