import pytest

import six

import glob
import json
import os
import shutil
import sys
import tarfile
import tempfile
import time
import zipfile

import requests

import verta
from verta._internal_utils import _histogram_utils
from verta._internal_utils import _utils


@pytest.fixture
def model_for_deployment(strs):
    np = pytest.importorskip("numpy")
    pd = pytest.importorskip("pandas")
    sklearn = pytest.importorskip("sklearn")
    from sklearn import linear_model

    num_rows, num_cols = 36, 6

    data = pd.DataFrame(np.tile(np.arange(num_rows).reshape(-1, 1),
                                num_cols),
                        columns=strs[:num_cols])
    X_train = data.iloc[:,:-1]  # pylint: disable=bad-whitespace
    y_train = data.iloc[:, -1]

    return {
        'model': sklearn.linear_model.LogisticRegression(),
        'model_api': verta.utils.ModelAPI(X_train, y_train),
        'requirements': six.StringIO("scikit-learn=={}".format(sklearn.__version__)),
        'train_features': X_train,
        'train_targets': y_train,
    }


@pytest.fixture
def model_packaging():
    """Additional items added to model API in log_model()."""
    return {
        'python_version': _utils.get_python_version(),
        'type': "sklearn",
        'deserialization': "cloudpickle",
    }


@pytest.mark.not_oss
class TestLogModelForDeployment:
    def test_model(self, experiment_run, model_for_deployment):
        experiment_run.log_model_for_deployment(**model_for_deployment)
        retrieved_model = experiment_run.get_model()

        assert model_for_deployment['model'].get_params() == retrieved_model.get_params()

    def test_model_api(self, experiment_run, model_for_deployment):
        experiment_run.log_model_for_deployment(**model_for_deployment)
        retrieved_model_api = verta.utils.ModelAPI.from_file(
            experiment_run.get_artifact("model_api.json"))

        assert all(item in six.viewitems(retrieved_model_api.to_dict())
                   for item in six.viewitems(model_for_deployment['model_api'].to_dict()))

    def test_reqs_on_disk(self, experiment_run, model_for_deployment, output_path):
        requirements_file = output_path.format("requirements.txt")
        with open(requirements_file, 'w') as f:
            f.write(model_for_deployment['requirements'].read())
        model_for_deployment['requirements'] = open(requirements_file, 'r')  # replace with on-disk file

        experiment_run.log_model_for_deployment(**model_for_deployment)
        retrieved_requirements = six.ensure_str(experiment_run.get_artifact("requirements.txt").read())

        with open(requirements_file, 'r') as f:
            assert set(f.read().split()) <= set(retrieved_requirements.split())

    def test_with_data(self, experiment_run, model_for_deployment):
        """`train_features` and `train_targets` are joined into a single CSV"""
        experiment_run.log_model_for_deployment(**model_for_deployment)

        data_csv = experiment_run.get_artifact("train_data").read()

        X_train = model_for_deployment['train_features']
        y_train = model_for_deployment['train_targets']
        assert X_train.join(y_train).to_csv(index=False) == six.ensure_str(data_csv)


@pytest.mark.not_oss
class TestLogModel:
    def test_model(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'])

        assert model_for_deployment['model'].get_params() == experiment_run.get_model().get_params()

    def test_custom_modules(self, experiment_run, model_for_deployment):
        custom_modules_dir = "."

        experiment_run.log_model(
            model_for_deployment['model'],
            custom_modules=["."],
        )

        custom_module_filenames = {"__init__.py", "_verta_config.py"}
        for parent_dir, dirnames, filenames in os.walk(custom_modules_dir):
            # skip venvs
            #     This logic is from _utils.find_filepaths().
            exec_path_glob = os.path.join(parent_dir, "{}", "bin", "python*")
            dirnames[:] = [dirname for dirname in dirnames if not glob.glob(exec_path_glob.format(dirname))]

            custom_module_filenames.update(map(os.path.basename, filenames))

        with zipfile.ZipFile(experiment_run.get_artifact("custom_modules"), 'r') as zipf:
            assert custom_module_filenames == set(map(os.path.basename, zipf.namelist()))

    def test_no_custom_modules(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'])

        custom_module_filenames = {"__init__.py", "_verta_config.py"}
        for path in sys.path:
            # skip std libs and venvs
            #     This logic is from verta.client._log_modules().
            lib_python_str = os.path.join(os.sep, "lib", "python")
            i = path.find(lib_python_str)
            if i != -1 and glob.glob(os.path.join(path[:i], "bin", "python*")):
                continue

            for parent_dir, dirnames, filenames in os.walk(path):
                # skip venvs
                #     This logic is from _utils.find_filepaths().
                exec_path_glob = os.path.join(parent_dir, "{}", "bin", "python*")
                dirnames[:] = [dirname for dirname in dirnames if not glob.glob(exec_path_glob.format(dirname))]

                # only Python files
                filenames[:] = [filename for filename in filenames if filename.endswith(('.py', '.pyc', '.pyo'))]

                custom_module_filenames.update(map(os.path.basename, filenames))

        with zipfile.ZipFile(experiment_run.get_artifact("custom_modules"), 'r') as zipf:
            assert custom_module_filenames == set(map(os.path.basename, zipf.namelist()))

    def test_model_api(self, experiment_run, model_for_deployment, model_packaging):
        experiment_run.log_model(
            model_for_deployment['model'],
            model_api=model_for_deployment['model_api'],
        )

        model_api = model_for_deployment['model_api'].to_dict()
        model_api.update({
            'model_packaging': model_packaging,
        })
        assert model_api == json.loads(six.ensure_str(experiment_run.get_artifact('model_api.json').read()))

    def test_no_model_api(self, experiment_run, model_for_deployment, model_packaging):
        experiment_run.log_model(model_for_deployment['model'])

        model_api = {
            'version': "v1",
            'model_packaging': model_packaging,
        }
        assert model_api == json.loads(six.ensure_str(experiment_run.get_artifact('model_api.json').read()))

    def test_model_class(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'].__class__)

        assert model_for_deployment['model'].__class__ == experiment_run.get_model()

        retrieved_model_api = verta.utils.ModelAPI.from_file(experiment_run.get_artifact("model_api.json"))
        assert retrieved_model_api.to_dict()['model_packaging']['type'] == "class"

    def test_artifacts(self, experiment_run, model_for_deployment, strs, flat_dicts):
        for key, artifact in zip(strs, flat_dicts):
            experiment_run.log_artifact(key, artifact)

        experiment_run.log_model(
            model_for_deployment['model'].__class__,
            artifacts=strs,
        )

        assert experiment_run.get_attribute("verta_model_artifacts") == strs

    def test_no_artifacts(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'].__class__)

        with pytest.raises(KeyError):
            experiment_run.get_attribute("verta_model_artifacts")

    def test_wrong_type_artifacts_error(self, experiment_run, model_for_deployment, all_values):
        # remove Nones, because they're equivalent to unprovided
        all_values = [val for val in all_values
                      if val is not None]
        # remove lists of strings and empty lists, because they're valid arguments
        all_values = [val for val in all_values
                      if not (isinstance(val, list) and all(isinstance(el, six.string_types) for el in val))]

        for val in all_values:
            with pytest.raises(TypeError):
                experiment_run.log_model(
                    model_for_deployment['model'].__class__,
                    artifacts=val,
                )

    def test_not_class_model_artifacts_error(self, experiment_run, model_for_deployment, strs, flat_dicts):
        for key, artifact in zip(strs, flat_dicts):
            experiment_run.log_artifact(key, artifact)

        with pytest.raises(ValueError):
            experiment_run.log_model(
                model_for_deployment['model'],
                artifacts=strs,
            )

    def test_unlogged_keys_artifacts_error(self, experiment_run, model_for_deployment, strs, flat_dicts):
        with pytest.raises(ValueError):
            experiment_run.log_model(
                model_for_deployment['model'],
                artifacts=[strs[0]],
            )

        experiment_run.log_artifact(strs[0], flat_dicts[0])

        with pytest.raises(ValueError):
            experiment_run.log_model(
                model_for_deployment['model'],
                artifacts=[strs[1]],
            )

        with pytest.raises(ValueError):
            experiment_run.log_model(
                model_for_deployment['model'],
                artifacts=strs[1:],
            )


@pytest.mark.not_oss
class TestFetchArtifacts:
    def test_fetch_artifacts(self, experiment_run, strs, flat_dicts):
        for key, artifact in zip(strs, flat_dicts):
            experiment_run.log_artifact(key, artifact)

        try:
            artifacts = experiment_run.fetch_artifacts(strs)

            assert set(six.viewkeys(artifacts)) == set(strs)
            assert all(filepath.startswith(verta.client._CACHE_DIR)
                       for filepath in six.viewvalues(artifacts))

            for key, filepath in six.viewitems(artifacts):
                artifact_contents, _ = experiment_run._get_artifact(key)
                with open(filepath, 'rb') as f:
                    file_contents = f.read()

                assert file_contents == artifact_contents
        finally:
            shutil.rmtree(verta.client._CACHE_DIR, ignore_errors=True)

    def test_cached_fetch_artifacts(self, experiment_run, strs, flat_dicts):
        key = strs[0]

        experiment_run.log_artifact(key, flat_dicts[0])

        try:
            filepath = experiment_run.fetch_artifacts([key])[key]
            last_modified = os.path.getmtime(filepath)

            time.sleep(3)
            assert experiment_run.fetch_artifacts([key])[key] == filepath

            assert os.path.getmtime(filepath) == last_modified
        finally:
            shutil.rmtree(verta.client._CACHE_DIR, ignore_errors=True)

    def test_fetch_zip(self, experiment_run, strs, dir_and_files):
        dirpath, filepaths = dir_and_files
        key = strs[0]

        experiment_run.log_artifact(key, dirpath)

        try:
            dirpath = experiment_run.fetch_artifacts([key])[key]

            assert dirpath.startswith(verta.client._CACHE_DIR)

            retrieved_filepaths = set()
            for root, _, files in os.walk(dirpath):
                for filename in files:
                    filepath = os.path.join(root, filename)
                    filepath = os.path.relpath(filepath, dirpath)
                    retrieved_filepaths.add(filepath)

            assert filepaths == retrieved_filepaths
        finally:
            shutil.rmtree(verta.client._CACHE_DIR, ignore_errors=True)

    def test_cached_fetch_zip(self, experiment_run, strs, dir_and_files):
        dirpath, _ = dir_and_files
        key = strs[0]

        experiment_run.log_artifact(key, dirpath)

        try:
            dirpath = experiment_run.fetch_artifacts([key])[key]
            last_modified = os.path.getmtime(dirpath)

            time.sleep(3)
            assert experiment_run.fetch_artifacts([key])[key] == dirpath

            assert os.path.getmtime(dirpath) == last_modified
        finally:
            shutil.rmtree(verta.client._CACHE_DIR, ignore_errors=True)

    def test_fetch_tgz(self, experiment_run, strs, dir_and_files):
        dirpath, filepaths = dir_and_files
        key = strs[0]

        with tempfile.NamedTemporaryFile(suffix='.tgz') as tempf:
            # make archive
            with tarfile.open(tempf.name, 'w:gz') as tarf:
                tarf.add(dirpath, "")
            tempf.flush()  # flush object buffer
            os.fsync(tempf.fileno())  # flush OS buffer
            tempf.seek(0)

            experiment_run.log_artifact(key, tempf.name)

        try:
            dirpath = experiment_run.fetch_artifacts([key])[key]

            assert dirpath.startswith(verta.client._CACHE_DIR)

            retrieved_filepaths = set()
            for root, _, files in os.walk(dirpath):
                for filename in files:
                    filepath = os.path.join(root, filename)
                    filepath = os.path.relpath(filepath, dirpath)
                    retrieved_filepaths.add(filepath)

            assert filepaths == retrieved_filepaths
        finally:
            shutil.rmtree(verta.client._CACHE_DIR, ignore_errors=True)

    def test_fetch_tar(self, experiment_run, strs, dir_and_files):
        dirpath, filepaths = dir_and_files
        key = strs[0]

        with tempfile.NamedTemporaryFile(suffix='.tar') as tempf:
            # make archive
            with tarfile.open(tempf.name, 'w') as tarf:
                tarf.add(dirpath, "")
            tempf.flush()  # flush object buffer
            os.fsync(tempf.fileno())  # flush OS buffer
            tempf.seek(0)

            experiment_run.log_artifact(key, tempf.name)

        try:
            dirpath = experiment_run.fetch_artifacts([key])[key]

            assert dirpath.startswith(verta.client._CACHE_DIR)

            retrieved_filepaths = set()
            for root, _, files in os.walk(dirpath):
                for filename in files:
                    filepath = os.path.join(root, filename)
                    filepath = os.path.relpath(filepath, dirpath)
                    retrieved_filepaths.add(filepath)

            assert filepaths == retrieved_filepaths
        finally:
            shutil.rmtree(verta.client._CACHE_DIR, ignore_errors=True)

    def test_fetch_tar_gz(self, experiment_run, strs, dir_and_files):
        dirpath, filepaths = dir_and_files
        key = strs[0]

        with tempfile.NamedTemporaryFile(suffix='.tar.gz') as tempf:
            # make archive
            with tarfile.open(tempf.name, 'w:gz') as tarf:
                tarf.add(dirpath, "")
            tempf.flush()  # flush object buffer
            os.fsync(tempf.fileno())  # flush OS buffer
            tempf.seek(0)

            experiment_run.log_artifact(key, tempf.name)

        try:
            dirpath = experiment_run.fetch_artifacts([key])[key]

            assert dirpath.startswith(verta.client._CACHE_DIR)

            retrieved_filepaths = set()
            for root, _, files in os.walk(dirpath):
                for filename in files:
                    filepath = os.path.join(root, filename)
                    filepath = os.path.relpath(filepath, dirpath)
                    retrieved_filepaths.add(filepath)

            assert filepaths == retrieved_filepaths
        finally:
            shutil.rmtree(verta.client._CACHE_DIR, ignore_errors=True)

    def test_wrong_type_artifacts_error(self, experiment_run, all_values):
        # remove lists of strings and empty lists, because they're valid arguments
        all_values = [val for val in all_values
                      if not (isinstance(val, list) and all(isinstance(el, six.string_types) for el in val))]

        for val in all_values:
            with pytest.raises(TypeError):
                experiment_run.fetch_artifacts(val)

    def test_unlogged_keys_artifacts_error(self, experiment_run, strs, flat_dicts):
        with pytest.raises(ValueError):
            experiment_run.fetch_artifacts([strs[0]])

        experiment_run.log_artifact(strs[0], flat_dicts[0])

        with pytest.raises(ValueError):
            experiment_run.fetch_artifacts([strs[1]])

        with pytest.raises(ValueError):
            experiment_run.fetch_artifacts(strs[1:])


@pytest.mark.not_oss
class TestLogRequirements:
    NONSPECIFIC_REQ = "verta>0.1.0"
    INVALID_REQ = "@==1.2.3"
    UNIMPORTABLE_REQ = "bananacoconut"
    VERTA_MISMATCH_REQ = "verta==0.0.0"
    CLOUDPICKLE_MISMATCH_REQ = "cloudpickle==0.0.0"
    VALID_REQS = [
        'cloudpickle',
        'pytest',
        'verta',
    ]

    def test_nonspecific_ver_list_warning(self, experiment_run):
        with pytest.warns(UserWarning):
            experiment_run.log_requirements([self.NONSPECIFIC_REQ])

    def test_nonspecific_ver_file_warning(self, experiment_run):
        with tempfile.NamedTemporaryFile('w+') as tempf:
            tempf.write(self.NONSPECIFIC_REQ)
            tempf.seek(0)

            with pytest.warns(UserWarning):
                experiment_run.log_requirements(tempf.name)

    def test_invalid_pkg_name_list_error(self, experiment_run):
        with pytest.raises(ValueError):
            experiment_run.log_requirements([self.INVALID_REQ])

    def test_invalid_pkg_name_file_error(self, experiment_run):
        with tempfile.NamedTemporaryFile('w+') as tempf:
            tempf.write(self.INVALID_REQ)
            tempf.seek(0)

            with pytest.raises(ValueError):
                experiment_run.log_requirements(tempf.name)

    def test_unimportable_pkg_list_error(self, experiment_run):
        with pytest.raises(ValueError):
            experiment_run.log_requirements([self.UNIMPORTABLE_REQ])

    def test_unimportable_pkg_file_error(self, experiment_run):
        with tempfile.NamedTemporaryFile('w+') as tempf:
            tempf.write(self.UNIMPORTABLE_REQ)
            tempf.seek(0)

            with pytest.raises(ValueError):
                experiment_run.log_requirements(tempf.name)

    def test_verta_ver_mismatch_list_error(self, experiment_run):
        with pytest.raises(ValueError):
            experiment_run.log_requirements([self.VERTA_MISMATCH_REQ])

    def test_verta_ver_mismatch_file_error(self, experiment_run):
        with tempfile.NamedTemporaryFile('w+') as tempf:
            tempf.write(self.VERTA_MISMATCH_REQ)
            tempf.seek(0)

            with pytest.raises(ValueError):
                experiment_run.log_requirements(tempf.name)

    def test_cloudpickle_ver_mismatch_list_error(self, experiment_run):
        with pytest.raises(ValueError):
            experiment_run.log_requirements([self.CLOUDPICKLE_MISMATCH_REQ])

    def test_cloudpickle_ver_mismatch_file_error(self, experiment_run):
        with tempfile.NamedTemporaryFile('w+') as tempf:
            tempf.write(self.CLOUDPICKLE_MISMATCH_REQ)
            tempf.seek(0)

            with pytest.raises(ValueError):
                experiment_run.log_requirements(tempf.name)

    def test_injection_list(self, experiment_run):
        experiment_run.log_requirements([])

        reqs_txt = experiment_run.get_artifact("requirements.txt").read().decode()
        reqs = set(req.split('==')[0].strip() for req in reqs_txt.splitlines())
        assert {'cloudpickle', 'verta'} == reqs

    def test_injection_file(self, experiment_run):
        with tempfile.NamedTemporaryFile('w+') as tempf:
            experiment_run.log_requirements(tempf.name)

        reqs_txt = experiment_run.get_artifact("requirements.txt").read().decode()
        reqs = set(req.split('==')[0].strip() for req in reqs_txt.splitlines())
        assert {'cloudpickle', 'verta'} == reqs

    def test_list(self, experiment_run):
        experiment_run.log_requirements(self.VALID_REQS)

        reqs_txt = experiment_run.get_artifact("requirements.txt").read().decode()
        reqs = set(req.split('==')[0].strip() for req in reqs_txt.splitlines())
        assert set(self.VALID_REQS) == reqs

    def test_file(self, experiment_run):
        with tempfile.NamedTemporaryFile('w+') as tempf:
            tempf.write('\n'.join(self.VALID_REQS))
            tempf.seek(0)

            experiment_run.log_requirements(tempf.name)

        reqs_txt = experiment_run.get_artifact("requirements.txt").read().decode()
        reqs = set(req.split('==')[0].strip() for req in reqs_txt.splitlines())
        assert set(self.VALID_REQS) == reqs


@pytest.mark.not_oss
class TestLogTrainingData:
    def test_numpy_error(self, experiment_run, model_for_deployment):
        with pytest.raises(TypeError):
            experiment_run.log_training_data(
                model_for_deployment['train_features'].values,
                model_for_deployment['train_targets'].values,
            )

    def test_list_error(self, experiment_run, model_for_deployment):
        with pytest.raises(TypeError):
            experiment_run.log_training_data(
                model_for_deployment['train_features'].values.tolist(),
                model_for_deployment['train_targets'].values.tolist(),
            )

    def test_column_name_error(self, experiment_run, model_for_deployment):
        X_train = model_for_deployment['train_features']
        y_train = model_for_deployment['train_targets']

        y_train = y_train.rename(X_train.columns[0])

        with pytest.raises(ValueError):
            experiment_run.log_training_data(X_train, y_train)

    def test_series(self, experiment_run, model_for_deployment):
        X_train = model_for_deployment['train_features']
        y_train = model_for_deployment['train_targets']

        experiment_run.log_training_data(X_train, y_train)

        data_csv = experiment_run.get_artifact("train_data").read()
        assert X_train.join(y_train).to_csv(index=False) == six.ensure_str(data_csv)

    def test_dataframe(self, experiment_run, model_for_deployment):
        X_train = model_for_deployment['train_features']
        y_train = model_for_deployment['train_targets']

        y_train = y_train.to_frame()

        experiment_run.log_training_data(X_train, y_train)

        data_csv = experiment_run.get_artifact("train_data").read()
        assert X_train.join(y_train).to_csv(index=False) == six.ensure_str(data_csv)

    def test_histogram(self, experiment_run):
        np = pytest.importorskip("numpy")
        pd = pytest.importorskip("pandas")

        df = pd.concat(
            objs=[
                pd.Series([True]*10 + [False]*20, name='binary col'),
                pd.Series([0]*5 + [1]*10 + [2]*15, name='discrete col'),
                pd.Series(range(30), name='continuous col'),
            ],
            axis='columns',
        )
        X = df[['binary col', 'discrete col']]
        y = df['continuous col']

        histograms = {
            'features': {
                'binary col': {'histogram': {'binary': {'count': [20, 10]}}, 'type': 'binary'},
                'discrete col': {'histogram': {'discrete': {
                    'bucket_values': [0, 1, 2],
                    'count': [5, 10, 15]}}, 'type': 'discrete'},
                'continuous col': {'histogram': {'float': {
                    'bucket_limits': [0, 2.9, 5.8, 8.7, 11.6, 14.5, 17.4, 20.3, 23.2, 26.099999999999998, 29],
                    'count': [3, 3, 3, 3, 3, 3, 3, 3, 3, 2]}}, 'type': 'float'},
            },
            'total_count': 30,
        }

        experiment_run.log_training_data(X, y)
        endpoint = "{}://{}/api/v1/monitoring/data/references/{}".format(
            experiment_run._conn.scheme,
            experiment_run._conn.socket,
            experiment_run.id,
        )
        response = _utils.make_request("GET", endpoint, experiment_run._conn)
        _utils.raise_for_http_error(response)
        generated_histograms = response.json()

        assert generated_histograms == histograms


class TestHistogram:
    @staticmethod
    def assert_histograms_match_dataframe(histograms, df):
        """Common assertions for this test suite."""
        # features match
        assert set(histograms['features'].keys()) == set(df.columns)
        # all rows counted
        assert histograms['total_count'] == len(df.index)

        for feature_name, histogram in histograms['features'].items():
            series = df[feature_name]
            histogram_type = histogram['type']
            histogram_data = histogram['histogram'][histogram_type]

            # all data points counted
            assert sum(histogram_data['count']) == len(series)

            if histogram_type == "binary":
                num_false = sum(~series)
                num_true = sum(series)

                assert histogram_data['count'] == [num_false, num_true]
            elif histogram_type == "discrete":
                buckets = histogram_data['bucket_values']
                counts = histogram_data['count']

                # buckets in ascending order
                assert buckets == list(sorted(buckets))

                # data within buckets
                assert all(buckets[0] <= series)
                assert all(series <= buckets[-1])

                # counts correct
                for value, count in zip(buckets, counts):
                    assert sum(series == value) == count
            elif histogram_type == "float":
                buckets = histogram_data['bucket_limits']
                counts = histogram_data['count']

                # buckets in ascending order
                assert buckets == list(sorted(buckets))

                # data within buckets
                assert all(buckets[0] <= series)
                assert all(series <= buckets[-1])

                # counts correct
                bin_windows = list(zip(buckets[:-1], buckets[1:]))
                for i, (l, r) in enumerate(bin_windows[:-1]):
                    assert sum((l <= series) & (series < r)) == counts[i]
                assert sum(buckets[-2] <= series) == counts[-1]

    def test_binary(self):
        np = pytest.importorskip("numpy")
        pd = pytest.importorskip("pandas")
        num_rows = 90

        df = pd.concat(
            objs=[
                pd.Series(np.random.random(size=num_rows).round().astype(bool), name="A"),
                pd.Series(np.random.random(size=num_rows).round().astype(bool), name="B"),
                pd.Series(np.random.random(size=num_rows).round().astype(bool), name="C"),
            ],
            axis='columns',
        )
        histograms = _histogram_utils.calculate_histograms(df)

        assert all(
            histogram['type'] == "binary"
            for histogram
            in histograms['features'].values()
        )
        self.assert_histograms_match_dataframe(histograms, df)

    def test_discrete(self):
        np = pytest.importorskip("numpy")
        pd = pytest.importorskip("pandas")
        num_rows = 90

        df = pd.concat(
            objs=[
                pd.Series(np.random.randint(6, 12, size=num_rows), name="A"),
                pd.Series(np.random.randint(-12, -6, size=num_rows), name="B"),
                pd.Series(np.random.randint(-3, 3, size=num_rows), name="C"),
            ],
            axis='columns',
        )
        histograms = _histogram_utils.calculate_histograms(df)

        assert all(
            histogram['type'] == "discrete"
            for histogram
            in histograms['features'].values()
        )
        self.assert_histograms_match_dataframe(histograms, df)

    def test_float(self):
        np = pytest.importorskip("numpy")
        pd = pytest.importorskip("pandas")
        num_rows = 90

        df = pd.concat(
            objs=[
                pd.Series(np.random.normal(loc=9, size=num_rows), name="A"),
                pd.Series(np.random.normal(scale=12, size=num_rows), name="B"),
                pd.Series(np.random.normal(loc=-3, scale=6, size=num_rows), name="C"),
            ],
            axis='columns',
        )
        histograms = _histogram_utils.calculate_histograms(df)

        assert all(
            histogram['type'] == "float"
            for histogram
            in histograms['features'].values()
        )
        self.assert_histograms_match_dataframe(histograms, df)


@pytest.mark.not_oss
class TestDeploy:
    def test_auto_path_auto_token_deploy(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        status = experiment_run.deploy()

        assert 'url' in status
        assert 'token' in status

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_auto_path_given_token_deploy(self, experiment_run, model_for_deployment):
        token = "coconut"

        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        status = experiment_run.deploy(token=token)

        assert 'url' in status
        assert status['token'] == token

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_auto_path_no_token_deploy(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        status = experiment_run.deploy(no_token=True)

        assert 'url' in status
        assert status['token'] is None

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_given_path_auto_token_deploy(self, experiment_run, model_for_deployment):
        path = "banana"

        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        status = experiment_run.deploy(path=path)

        assert status['url'].endswith(path)
        assert 'token' in status

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_given_path_given_token_deploy(self, experiment_run, model_for_deployment):
        path, token = "banana", "coconut"

        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        status = experiment_run.deploy(path=path, token=token)

        assert status['url'].endswith(path)
        assert status['token'] == token

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_given_path_no_token_deploy(self, experiment_run, model_for_deployment):
        path = "banana"

        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        status = experiment_run.deploy(path=path, no_token=True)

        assert status['url'].endswith(path)
        assert status['token'] is None

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_wait_deploy(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        status = experiment_run.deploy(wait=True)

        assert 'url' in status
        assert 'token' in status
        assert status['status'] == "deployed"

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_already_deployed_deploy(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        experiment_run.deploy()

        # should not raise error
        experiment_run.deploy()

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_no_model_deploy_error(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        # delete model
        _utils.make_request(
            "DELETE",
            "{}://{}/api/v1/modeldb/experiment-run/deleteArtifact".format(experiment_run._conn.scheme,
                                                              experiment_run._conn.socket),
            experiment_run._conn, json={'id': experiment_run.id, 'key': "model.pkl"}
        ).raise_for_status()

        with pytest.raises(RuntimeError) as excinfo:
            experiment_run.deploy()
        assert str(excinfo.value).strip() == "unable to deploy due to missing artifact model.pkl"

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_no_api_deploy_error(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        # delete model API
        _utils.make_request(
            "DELETE",
            "{}://{}/api/v1/modeldb/experiment-run/deleteArtifact".format(experiment_run._conn.scheme,
                                                              experiment_run._conn.socket),
            experiment_run._conn, json={'id': experiment_run.id, 'key': "model_api.json"}
        ).raise_for_status()

        with pytest.raises(RuntimeError) as excinfo:
            experiment_run.deploy()
        assert str(excinfo.value).strip() == "unable to deploy due to missing artifact model_api.json"

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_no_reqs_deploy_error(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])

        with pytest.raises(RuntimeError) as excinfo:
            experiment_run.deploy()
        assert str(excinfo.value).strip() == "unable to deploy due to missing artifact requirements.txt"

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_deployment_failure_deploy_error(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements([])

        with pytest.raises(RuntimeError) as excinfo:
            experiment_run.deploy(wait=True)
        err_msg = str(excinfo.value).strip()
        assert err_msg.startswith("model deployment is failing;")
        assert "no error message available" not in err_msg

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )


@pytest.mark.not_oss
class TestUndeploy:
    def test_undeploy(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        experiment_run.deploy(wait=True)

        status = experiment_run.undeploy(wait=True)

        assert status['status'] == "not deployed"

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_already_undeployed_undeploy(self, experiment_run):
        # should not raise error
        experiment_run.undeploy()


@pytest.mark.not_oss
class TestGetDeployedModel:
    def test_get(self, experiment_run, model_for_deployment):
        model = model_for_deployment['model'].fit(
            model_for_deployment['train_features'],
            model_for_deployment['train_targets'],
        )

        experiment_run.log_model(model, custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        experiment_run.deploy(wait=True)

        x = model_for_deployment['train_features'].iloc[1].values
        experiment_run.get_deployed_model().predict([x])

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )

    def test_not_deployed_get_error(self, experiment_run, model_for_deployment):
        with pytest.raises(RuntimeError):
            experiment_run.get_deployed_model()

    def test_undeployed_get_error(self, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        experiment_run.deploy(wait=True)
        experiment_run.undeploy(wait=True)

        with pytest.raises(RuntimeError):
            experiment_run.get_deployed_model()

        conn = experiment_run._conn
        requests.delete(
            "{}://{}/api/v1/deployment/models/{}".format(conn.scheme, conn.socket, experiment_run.id),
            headers=conn.auth,
        )
