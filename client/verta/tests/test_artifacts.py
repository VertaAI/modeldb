import pytest

import six

import os
import sys
import zipfile

from . import utils


class TestArtifacts:
    def test_log_path(self, experiment_run, strs):
        strs, holdout = strs[:-1], strs[-1]  # reserve last key

        for key, artifact_path in zip(strs, strs):
            experiment_run.log_artifact_path(key, artifact_path)

        for key, artifact_path in zip(strs, strs):
            assert experiment_run.get_artifact(key) == artifact_path

        with pytest.raises(KeyError):
            experiment_run.get_artifact(holdout)

    def test_upload_object(self, experiment_run, strs, all_values):
        strs, holdout = strs[:-1], strs[-1]  # reserve last key
        all_values = (value  # log_artifact treats str value as filepath to open
                      for value in all_values if not isinstance(value, str))

        for key, artifact in zip(strs, all_values):
            experiment_run.log_artifact(key, artifact)

        for key, artifact in zip(strs, all_values):
            assert experiment_run.get_artifact(key) == artifact

        with pytest.raises(KeyError):
            experiment_run.get_artifact(holdout)

    def test_upload_file(self, experiment_run, strs):
        filepaths = (
            filepath for filepath in os.listdir('.')
            if filepath.endswith('.py')
            and os.path.basename(filepath) != "__init__.py"
        )
        artifacts = list(zip(strs, filepaths))

        # log using file handle
        for key, artifact_filepath in artifacts[:len(artifacts)//2]:
            with open(artifact_filepath, 'r') as artifact_file:  # does not need to be 'rb'
                experiment_run.log_artifact(key, artifact_file)

        # log using filepath
        for key, artifact_filepath in artifacts[len(artifacts)//2:]:
            experiment_run.log_artifact(key, artifact_filepath)

        # get
        for key, artifact_filepath in artifacts:
            with open(artifact_filepath, 'rb') as artifact_file:
                assert experiment_run.get_artifact(key).read() == artifact_file.read()

    def test_upload_dir(self, experiment_run, strs, dir_and_files):
        dirpath, filepaths = dir_and_files
        key = strs[0]

        experiment_run.log_artifact(key, dirpath)

        with zipfile.ZipFile(experiment_run.get_artifact(key), 'r') as zipf:
            assert filepaths == set(zipf.namelist())

    def test_empty(self, experiment_run, strs):
        """uploading empty data, e.g. an empty file, raises an error"""

        with pytest.raises(ValueError):
            experiment_run.log_artifact(strs[0], six.BytesIO())

    def test_conflict(self, experiment_run, strs, all_values):
        all_values = (value  # log_artifact treats str value as filepath to open
                      for value in all_values if not isinstance(value, str))

        for key, artifact in zip(strs, all_values):
            experiment_run.log_artifact(key, artifact)
            with pytest.raises(ValueError):
                experiment_run.log_artifact(key, artifact)

        for key, artifact in reversed(list(zip(strs, all_values))):
            with pytest.raises(ValueError):
                experiment_run.log_artifact(key, artifact)


class TestModels:
    def test_sklearn(self, seed, experiment_run, strs):
        np = pytest.importorskip("numpy")
        sklearn = pytest.importorskip("sklearn")
        from sklearn import cluster, naive_bayes, pipeline, preprocessing

        np.random.seed(seed)
        key = strs[0]
        num_data_rows = 36
        X = np.random.random((num_data_rows, 2))
        y = np.random.randint(10, size=num_data_rows)

        pipeline = sklearn.pipeline.make_pipeline(
            sklearn.preprocessing.StandardScaler(),
            sklearn.cluster.KMeans(),
            sklearn.naive_bayes.GaussianNB(),
        )
        pipeline.fit(X, y)

        experiment_run.log_model(pipeline)
        retrieved_pipeline = experiment_run.get_model()

        assert np.allclose(pipeline.predict(X), retrieved_pipeline.predict(X))

        assert len(pipeline.steps) == len(retrieved_pipeline.steps)
        for step, retrieved_step in zip(pipeline.steps, retrieved_pipeline.steps):
            assert step[0] == retrieved_step[0]  # step name
            assert step[1].get_params() == retrieved_step[1].get_params()  # step model

    def test_torch(self, seed, experiment_run, strs):
        np = pytest.importorskip("numpy")
        torch = pytest.importorskip("torch")
        import torch.nn as nn
        import torch.nn.functional as F
        import torch.optim as optim

        np.random.seed(seed)
        key = strs[0]
        num_data_rows = 36
        X = torch.tensor(np.random.random((num_data_rows, 3, 32, 32)), dtype=torch.float)
        y = torch.tensor(np.random.randint(10, size=num_data_rows), dtype=torch.long)

        class Model(nn.Module):
            def __init__(self):
                super(Model, self).__init__()
                self.conv1 = nn.Conv2d(3, 6, 5)
                self.pool = nn.MaxPool2d(2, 2)
                self.conv2 = nn.Conv2d(6, 16, 5)
                self.fc1 = nn.Linear(16 * 5 * 5, 120)
                self.fc2 = nn.Linear(120, 84)
                self.fc3 = nn.Linear(84, 10)

            def forward(self, x):
                x = self.pool(F.relu(self.conv1(x)))
                x = self.pool(F.relu(self.conv2(x)))
                x = x.view(-1, 16 * 5 * 5)
                x = F.relu(self.fc1(x))
                x = F.relu(self.fc2(x))
                x = self.fc3(x)
                return x

        net = Model()
        criterion = torch.nn.CrossEntropyLoss()
        optimizer = torch.optim.Adam(net.parameters())
        for epoch in range(5):
            y_pred = net(X)
            loss = criterion(y_pred, y)
            loss.backward()
            optimizer.step()

        experiment_run.log_model(net)
        retrieved_net = experiment_run.get_model()

        assert torch.allclose(net(X), retrieved_net(X))

        assert net.state_dict().keys() == retrieved_net.state_dict().keys()
        for key, weight in net.state_dict().items():
            assert torch.allclose(weight, retrieved_net.state_dict()[key])

    def test_keras(self, seed, experiment_run, strs):
        np = pytest.importorskip("numpy")
        tf = pytest.importorskip("tensorflow")
        from tensorflow import keras

        np.random.seed(seed)
        key = strs[0]
        num_data_rows = 36
        X = np.random.random((num_data_rows, 28, 28))
        y = np.random.random(num_data_rows)

        net = keras.models.Sequential([
            keras.layers.Flatten(input_shape=(28, 28)),
            keras.layers.Dense(128, activation='relu'),
            keras.layers.Dropout(0.2),
            keras.layers.Dense(10, activation='softmax')
        ])
        net.compile(
            optimizer='adam',
            loss='sparse_categorical_crossentropy',
            metrics=['accuracy']
        )
        net.fit(X, y, epochs=5)

        experiment_run.log_model(net)
        retrieved_net = experiment_run.get_model()

        assert np.allclose(net.predict(X), retrieved_net.predict(X))
        # NOTE: history is purged when model is saved
        # assert np.allclose(net.history.history, retrieved_net.history.history)

        assert len(net.weights) == len(retrieved_net.weights)
        # NOTE: weight states have weird shenanigans when model is saved
        # for weight, retrieved_weight in zip(net.weights, retrieved_net.weights):

        tf.compat.v1.reset_default_graph()

    def test_function(self, experiment_run, strs, flat_lists, flat_dicts):
        key = strs[0]
        func_args = flat_lists[0]
        func_kwargs = flat_dicts[0]

        def func(is_func=True, _cache=set([1, 2, 3]), *args, **kwargs):
            return (args, kwargs)

        experiment_run.log_model(func)
        assert experiment_run.get_model().__defaults__ == func.__defaults__
        assert experiment_run.get_model()(*func_args, **func_kwargs) == func(*func_args, **func_kwargs)

    def test_custom_class(self, experiment_run, strs, flat_lists, flat_dicts):
        key = strs[0]
        init_args = flat_lists[0]
        init_kwargs = flat_dicts[0]

        class Custom(object):
            def __init__(self, *args, **kwargs):
                self.args = args
                self.kwargs = kwargs

            def predict(self, data):
                return (self.args, self.kwargs)

        custom = Custom(*init_args, **init_kwargs)

        experiment_run.log_model(custom)
        assert experiment_run.get_model().__dict__ == custom.__dict__
        assert experiment_run.get_model().predict(strs) == custom.predict(strs)


class TestImages:
    @staticmethod
    def matplotlib_to_pil(fig):
        PIL = pytest.importorskip("PIL")

        bytestream = six.BytesIO()
        fig.savefig(bytestream)
        return PIL.Image.open(bytestream)

    def test_log_path(self, experiment_run, strs):
        strs, holdout = strs[:-1], strs[-1]  # reserve last key

        for key, image_path in zip(strs, strs):
            experiment_run.log_image_path(key, image_path)

        for key, image_path in zip(strs, strs):
            assert experiment_run.get_image(key) == image_path

        with pytest.raises(KeyError):
            experiment_run.get_image(holdout)

    def test_upload_blank_warning(self, experiment_run, strs):
        PIL = pytest.importorskip("PIL")

        key = strs[0]
        img = PIL.Image.new('RGB', (64, 64), 'white')

        with pytest.warns(UserWarning):
            experiment_run.log_image(key, img)

    def test_upload_plt(self, experiment_run, strs):
        np = pytest.importorskip("numpy")
        matplotlib = pytest.importorskip("matplotlib")
        matplotlib.use("Agg")  # https://stackoverflow.com/a/37605654
        import matplotlib.pyplot as plt

        key = strs[0]
        plt.scatter(*np.random.random((2, 10)))

        experiment_run.log_image(key, plt)
        assert np.array_equal(np.asarray(experiment_run.get_image(key).getdata()),
                              np.asarray(self.matplotlib_to_pil(plt).getdata()))

    def test_upload_fig(self, experiment_run, strs):
        np = pytest.importorskip("numpy")
        matplotlib = pytest.importorskip("matplotlib")
        matplotlib.use("Agg")  # https://stackoverflow.com/a/37605654
        import matplotlib.pyplot as plt

        key = strs[0]
        fig, ax = plt.subplots()
        ax.scatter(*np.random.random((2, 10)))

        experiment_run.log_image(key, fig)
        assert np.array_equal(np.asarray(experiment_run.get_image(key).getdata()),
                              np.asarray(self.matplotlib_to_pil(fig).getdata()))

    def test_upload_pil(self, experiment_run, strs):
        np = pytest.importorskip("numpy")
        PIL = pytest.importorskip("PIL")
        import PIL.ImageDraw

        key = strs[0]
        img = PIL.Image.new('RGB', (64, 64), 'gray')
        PIL.ImageDraw.Draw(img).arc(np.r_[np.random.randint(32, size=(2)),
                                          np.random.randint(32, 64, size=(2))].tolist(),
                                    np.random.randint(360), np.random.randint(360),
                                    'white')

        experiment_run.log_image(key, img)
        assert(np.array_equal(np.asarray(experiment_run.get_image(key).getdata()),
                              np.asarray(img.getdata())))

    def test_conflict(self, experiment_run, strs):
        PIL = pytest.importorskip("PIL")

        images = dict(zip(strs, [PIL.Image.new('RGB', (64, 64), 'gray')]*3))

        for key, image in six.viewitems(images):
            experiment_run.log_image(key, image)
            with pytest.raises(ValueError):
                experiment_run.log_image(key, image)

        for key, image in reversed(list(six.viewitems(images))):
            with pytest.raises(ValueError):
                experiment_run.log_image(key, image)


class TestOverwrite:
    def test_artifact(self, experiment_run):
        artifact = ['banana']
        new_artifact = ["coconut"]

        experiment_run.log_artifact("date", artifact)
        experiment_run.log_artifact("date", new_artifact, overwrite=True)

        assert experiment_run.get_artifact("date") == new_artifact

    def test_model(self, experiment_run):
        model = TestArtifacts
        new_model = TestOverwrite

        experiment_run.log_model(model)
        experiment_run.log_model(new_model, overwrite=True)

        assert experiment_run.get_artifact("model.pkl") == new_model

    def test_requirements(self, experiment_run):
        requirements = ["banana==1"]
        new_requirements = ["coconut==1"]

        experiment_run.log_requirements(requirements)
        experiment_run.log_requirements(new_requirements, overwrite=True)

        assert six.ensure_binary('\n'.join(new_requirements)) in experiment_run.get_artifact("requirements.txt").read()

    def test_training_data(self, experiment_run):
        pd = pytest.importorskip("pandas")

        X = pd.DataFrame([[1, 1, 1],
                          [1, 1, 1],
                          [1, 1, 1]],
                         columns=["1_1", "1_2", "1_3"])
        y = pd.Series([1, 1, 1], name="1")
        new_X = pd.DataFrame([[2, 2, 2],
                              [2, 2, 2],
                              [2, 2, 2]],
                             columns=["2_1", "2_2", "2_3"])
        new_y = pd.Series([2, 2, 2], name="2")

        experiment_run.log_training_data(X, y)
        experiment_run.log_training_data(new_X, new_y, overwrite=True)

        assert pd.read_csv(experiment_run.get_artifact("train_data")).equals(new_X.join(new_y))

    def test_setup_script(self, experiment_run):
        setup_script = "import verta"
        new_setup_script = "import cloudpickle"

        experiment_run.log_setup_script(setup_script)
        experiment_run.log_setup_script(new_setup_script, overwrite=True)

        assert experiment_run.get_artifact("setup_script").read() == six.ensure_binary(new_setup_script)
