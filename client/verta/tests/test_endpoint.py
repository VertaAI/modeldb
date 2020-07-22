import pytest

from verta.utils import ModelAPI
from verta._deployment import Endpoint
from verta.deployment import DirectUpdateStrategy


class TestEndpoint:
    def test_update(self, experiment_run):
        np = pytest.importorskip("numpy")
        sklearn = pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression

        X_train = np.random.random((36, 12))
        y_train = np.random.random(36).round()
        classifier = LogisticRegression()
        classifier.fit(X_train, y_train)

        # create deployment artifacts
        model_api = ModelAPI(X_train, classifier.predict(X_train))
        requirements = ["scikit-learn"]

        # save and log model
        experiment_run.log_model(classifier, model_api=model_api)
        experiment_run.log_requirements(requirements)

        # TODO: remove hardcoding
        endpoint = Endpoint(experiment_run._conn, experiment_run._conf, "Nhat_Pham", 449)
        endpoint._path = "/string"

        endpoint.update(experiment_run, DirectUpdateStrategy)

