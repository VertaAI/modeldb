# -*- coding: utf-8 -*-

import pytest

import six

import filecmp
import hashlib
import os
import pickle
import shutil
import tempfile
import zipfile

import requests

from verta._internal_utils import (
    _artifact_utils,
    _file_utils,
    _request_utils,
    _utils,
)
from . import utils


class TestUtils:
    def test_calc_sha256(self):
        FILE_SIZE = 6*10**6  # 6 MB

        with tempfile.NamedTemporaryFile(suffix='.txt') as tempf:
            tempf.truncate(FILE_SIZE)  # zero-filled
            tempf.flush()  # flush object buffer
            os.fsync(tempf.fileno())  # flush OS buffer

            tempf.seek(0)
            piecewise_checksum = _artifact_utils.calc_sha256(tempf, FILE_SIZE//2)

            tempf.seek(0)
            whole_checksum = hashlib.sha256(tempf.read()).hexdigest()

            assert piecewise_checksum == whole_checksum

    def test_download_file_no_collision(self, experiment_run, dir_and_files, in_tempdir):
        source_dirpath, _ = dir_and_files
        key = "artifact"

        # create archive and move into cwd so it's deleted on teardown
        filepath = os.path.abspath("archive.zip")
        temp_zip = _artifact_utils.zip_dir(source_dirpath)
        os.rename(temp_zip.name, filepath)

        # upload and download file
        experiment_run.log_artifact(key, filepath)
        download_url = experiment_run._get_url_for_artifact(key, "GET").url
        response = requests.get(download_url)
        downloaded_filepath = _request_utils.download_file(
            response, filepath, overwrite_ok=False,
        )
        downloaded_filepath = os.path.abspath(downloaded_filepath)

        # different names
        assert filepath != downloaded_filepath
        # contents match
        assert filecmp.cmp(filepath, downloaded_filepath)

    def test_download_zipped_dir_no_collision(self, experiment_run, dir_and_files, in_tempdir):
        source_dirpath, _ = dir_and_files
        key = "artifact"

        # move directory into cwd so it's deleted on teardown
        dirpath = os.path.abspath("directory")
        os.rename(source_dirpath, dirpath)

        # upload and download directory
        experiment_run.log_artifact(key, dirpath)
        download_url = experiment_run._get_url_for_artifact(key, "GET").url
        response = requests.get(download_url)
        downloaded_dirpath = _request_utils.download_zipped_dir(
            response, dirpath, overwrite_ok=False,
        )
        downloaded_dirpath = os.path.abspath(downloaded_dirpath)

        # different names
        assert dirpath != downloaded_dirpath
        # contents match
        utils.assert_dirs_match(dirpath, downloaded_dirpath)


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

    @pytest.mark.not_oss
    def test_upload_multipart(self, experiment_run, in_tempdir):
        key = "large"

        # create artifact
        with tempfile.NamedTemporaryFile(suffix='.bin', dir=".", delete=False) as tempf:
            # write 6 MB file in 1 MB chunks
            for _ in range(6):
                tempf.write(os.urandom(1*(10**6)))

        # log artifact
        # TODO: set part size in config file when supported
        PART_SIZE = int(5.4*(10**6))  # 5.4 MB; S3 parts must be > 5 MB
        os.environ['VERTA_ARTIFACT_PART_SIZE'] = str(PART_SIZE)
        try:
            experiment_run.log_artifact(key, tempf.name)
        finally:
            del os.environ['VERTA_ARTIFACT_PART_SIZE']

        # get artifact parts
        committed_parts = experiment_run.get_artifact_parts(key)
        assert committed_parts

        # part checksums match actual file contents
        with open(tempf.name, 'rb') as f:
            file_parts = iter(lambda: f.read(PART_SIZE), b'')
            for file_part, committed_part in zip(file_parts, committed_parts):
                part_hash = hashlib.md5(file_part).hexdigest()
                assert part_hash == committed_part['etag'].strip('"')

        # retrieved artifact matches original file
        filepath = experiment_run.download_artifact(key, download_to_path=key)
        with open(filepath, 'rb') as f:
            file_parts = iter(lambda: f.read(PART_SIZE), b'')
            for file_part, committed_part in zip(file_parts, committed_parts):
                part_hash = hashlib.md5(file_part).hexdigest()
                assert part_hash == committed_part['etag'].strip('"')

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

    def test_blocklisted_key_error(self, experiment_run, all_values):
        all_values = (value  # log_artifact treats str value as filepath to open
                      for value in all_values if not isinstance(value, str))

        for key, artifact in zip(_artifact_utils.BLOCKLISTED_KEYS, all_values):
            with pytest.raises(ValueError):
                experiment_run.log_artifact(key, artifact)
            with pytest.raises(ValueError):
                experiment_run.log_artifact_path(key, artifact)

    def test_clientside_storage(self, experiment_run, strs, in_tempdir, random_data):
        key = strs[0]
        filename = strs[1]
        FILE_CONTENTS = random_data

        # TODO: be able to use existing env var for debugging
        # NOTE: there is an assertion of `== 1` artifact that would need to be changed
        VERTA_ARTIFACT_DIR_KEY = 'VERTA_ARTIFACT_DIR'
        PREV_VERTA_ARTIFACT_DIR = os.environ.pop(VERTA_ARTIFACT_DIR_KEY, None)
        try:
            VERTA_ARTIFACT_DIR = os.path.join(in_tempdir, "artifact-store")
            os.environ[VERTA_ARTIFACT_DIR_KEY] = VERTA_ARTIFACT_DIR

            # create file
            with open(filename, 'wb') as f:
                f.write(FILE_CONTENTS)
            # log artifact and delete file
            experiment_run.log_artifact(key, filename)
            os.remove(filename)
            # and then there was one
            assert len(os.listdir(VERTA_ARTIFACT_DIR)) == 1

            # artifact retrievable
            artifact = experiment_run.get_artifact(key)
            assert artifact.read() == FILE_CONTENTS

            # artifact downloadable
            filepath = experiment_run.download_artifact(key, filename)
            with open(filepath, 'rb') as f:
                assert f.read() == FILE_CONTENTS

            # object as well
            obj = {'some': ["arbitrary", "object"]}
            experiment_run.log_artifact(key, obj, overwrite=True)
            assert experiment_run.get_artifact(key) == obj
        finally:
            if PREV_VERTA_ARTIFACT_DIR is not None:
                os.environ[VERTA_ARTIFACT_DIR_KEY] = PREV_VERTA_ARTIFACT_DIR
            else:
                del os.environ[VERTA_ARTIFACT_DIR_KEY]

    def test_download(self, experiment_run, strs, in_tempdir, random_data):
        key = strs[0]
        filename = strs[1]
        new_filename = strs[2]
        FILE_CONTENTS = random_data

        # create file and upload as artifact
        with open(filename, 'wb') as f:
            f.write(FILE_CONTENTS)
        experiment_run.log_artifact(key, filename)
        os.remove(filename)

        # download artifact and verify contents
        new_filepath = experiment_run.download_artifact(key, new_filename)
        assert new_filepath == os.path.abspath(new_filename)
        with open(new_filepath, 'rb') as f:
            assert f.read() == FILE_CONTENTS

        # object as well
        obj = {'some': ["arbitrary", "object"]}
        experiment_run.log_artifact(key, obj, overwrite=True)
        new_filepath = experiment_run.download_artifact(key, new_filename)
        with open(new_filepath, 'rb') as f:
            assert pickle.load(f) == obj

    def test_download_directory(self, experiment_run, strs, dir_and_files, in_tempdir):
        key, download_path = strs[:2]
        dirpath, _ = dir_and_files

        experiment_run.log_artifact(key, dirpath)
        retrieved_path = experiment_run.download_artifact(key, download_path)

        # contents match
        utils.assert_dirs_match(dirpath, retrieved_path)

    def test_download_path_only_error(self, experiment_run, strs, in_tempdir):
        key = strs[0]
        path = strs[1]

        experiment_run.log_artifact_path(key, path)
        with pytest.raises(ValueError):
            experiment_run.download_artifact(key, path)


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

    def test_torch_state_dict(self, experiment_run, in_tempdir):
        torch = pytest.importorskip("torch")
        import torch.nn as nn
        import torch.nn.functional as F
        import torch.optim as optim

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

        # save state dict as artifact
        with open("buffer", 'wb') as buffer:
            torch.save(net.state_dict(), buffer)
        experiment_run.log_artifact("net_state", buffer.name)

        # retrieve and load state dict
        state_dict = experiment_run.get_artifact("net_state")
        new_net = Model()
        new_net.load_state_dict(state_dict)

        # weights are the same
        assert net.state_dict().keys() == state_dict.keys()
        for key, weight in net.state_dict().items():
            assert torch.allclose(weight, state_dict[key])

    @pytest.mark.tensorflow
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

    def test_pyspark(self, experiment_run, in_tempdir):
        data_filename = "census-train.csv"
        spark_model_dir = "spark-model"

        pytest.importorskip("boto3").client("s3").download_file("verta-starter", data_filename, data_filename)
        SparkSession = pytest.importorskip("pyspark.sql").SparkSession
        col = pytest.importorskip("pyspark.sql.functions").col
        LogisticRegression = pytest.importorskip("pyspark.ml.classification").LogisticRegression
        LogisticRegressionModel = pytest.importorskip("pyspark.ml.classification").LogisticRegressionModel
        VectorAssembler = pytest.importorskip("pyspark.ml.feature").VectorAssembler

        spark = SparkSession.builder.master("local").appName("parquet_example").getOrCreate()

        df = spark.read.csv(data_filename, header=True, inferSchema=True)
        df.repartition(5).write.mode("overwrite").parquet("datasets/census-train-parquet")
        df = VectorAssembler(
            inputCols=[c for c in df.columns if c != ">50k"],
            outputCol="features",
        ).transform(df)
        df = df.withColumn("label", col(">50k"))
        df = df["features", "label"]

        # log model
        model = LogisticRegression().fit(df)
        experiment_run.log_model(model, custom_modules=[])

        # get model
        with zipfile.ZipFile(experiment_run.get_model()) as zipf:
            zipf.extractall(spark_model_dir)
        assert LogisticRegressionModel.load(spark_model_dir).params == model.params

    def test_download_sklearn(self, experiment_run, in_tempdir):
        LogisticRegression = pytest.importorskip("sklearn.linear_model").LogisticRegression

        upload_path = "model.pkl"
        download_path = "retrieved_model.pkl"

        model = LogisticRegression(C=0.67, max_iter=178)  # set some non-default values
        with open(upload_path, 'wb') as f:
            pickle.dump(model, f)

        experiment_run.log_model(model, custom_modules=[])
        returned_path = experiment_run.download_model(download_path)
        assert returned_path == os.path.abspath(download_path)

        with open(download_path, 'rb') as f:
            downloaded_model = pickle.load(f)

        assert downloaded_model.get_params() == model.get_params()


class TestArbitraryModels:
    @staticmethod
    def _assert_no_deployment_artifacts(experiment_run):
        artifact_keys = experiment_run.get_artifact_keys()
        assert _artifact_utils.CUSTOM_MODULES_KEY not in artifact_keys
        assert _artifact_utils.MODEL_API_KEY not in artifact_keys

    def test_arbitrary_file(self, experiment_run, random_data):
        with tempfile.NamedTemporaryFile() as f:
            f.write(random_data)
            f.seek(0)

            experiment_run.log_model(f)

        assert experiment_run.get_model().read() == random_data

        self._assert_no_deployment_artifacts(experiment_run)

    def test_arbitrary_directory(self, experiment_run, dir_and_files):
        dirpath, filepaths = dir_and_files

        experiment_run.log_model(dirpath)

        with zipfile.ZipFile(experiment_run.get_model(), 'r') as zipf:
            assert set(zipf.namelist()) == filepaths

        self._assert_no_deployment_artifacts(experiment_run)

    def test_arbitrary_object(self, experiment_run):
        model = {'a': 1}

        experiment_run.log_model(model)

        assert experiment_run.get_model() == model

        self._assert_no_deployment_artifacts(experiment_run)

    def test_download_arbitrary_directory(self, experiment_run, dir_and_files, strs, in_tempdir):
        """Model that was originally a dir is unpacked on download."""
        dirpath, _ = dir_and_files
        download_path = strs[0]

        experiment_run.log_model(dirpath)
        returned_path = experiment_run.download_model(download_path)
        assert returned_path == os.path.abspath(download_path)

        # contents match
        utils.assert_dirs_match(dirpath, download_path)

    def test_download_arbitrary_zip(self, experiment_run, dir_and_files, strs, in_tempdir):
        """Model that was originally a ZIP is not unpacked on download."""
        model_dir, _ = dir_and_files
        upload_path, download_path = strs[:2]

        # zip `model_dir` into `upload_path`
        with open(upload_path, 'wb') as f:
            shutil.copyfileobj(
                _artifact_utils.zip_dir(model_dir),
                f,
            )

        experiment_run.log_model(upload_path)
        returned_path = experiment_run.download_model(download_path)
        assert returned_path == os.path.abspath(download_path)

        assert zipfile.is_zipfile(download_path)
        assert filecmp.cmp(upload_path, download_path)


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

    def test_blocklisted_key_error(self, experiment_run, all_values):
        all_values = (value  # log_artifact treats str value as filepath to open
                      for value in all_values if not isinstance(value, str))

        for key, artifact in zip(_artifact_utils.BLOCKLISTED_KEYS, all_values):
            with pytest.raises(ValueError):
                experiment_run.log_image(key, artifact)
            with pytest.raises(ValueError):
                experiment_run.log_image_path(key, artifact)


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

        assert experiment_run.get_artifact(_artifact_utils.MODEL_KEY) == new_model

    def test_setup_script(self, experiment_run):
        setup_script = "import verta"
        new_setup_script = "import cloudpickle"

        experiment_run.log_setup_script(setup_script)
        experiment_run.log_setup_script(new_setup_script, overwrite=True)

        assert experiment_run.get_artifact("setup_script").read() == six.ensure_binary(new_setup_script)
