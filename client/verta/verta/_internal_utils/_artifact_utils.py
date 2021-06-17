# -*- coding: utf-8 -*-

import hashlib
import os
import shutil
import tempfile
import zipfile

import cloudpickle

from ..external import six
from ..external.six.moves import cPickle as pickle  # pylint: disable=import-error, no-name-in-module

from .. import __about__

from .importer import maybe_dependency, get_tensorflow_major_version


# default chunk sizes
# these values were all chosen arbitrarily at different times
_64MB = 64*(10**6)  # used for artifact uploads
_32MB = 32*(10**6)  # used in _request_utils
_5MB = 5*(10**6)  # used in this module


# for zip_dir()
# dirs zipped by client need an identifiable extension to unzip durig d/l
ZIP_EXTENSION = "dir.zip"


# NOTE: keep up-to-date with Deployment API
CUSTOM_MODULES_KEY = "custom_modules"
REGISTRY_MODEL_KEY = "model"
MODEL_KEY = "model.pkl"  # currently used by experiment run
MODEL_API_KEY = "model_api.json"
# TODO: maybe bind constants for other keys used throughout client
BLOCKLISTED_KEYS = {
    CUSTOM_MODULES_KEY,
    MODEL_KEY,
    MODEL_API_KEY,
    'requirements.txt',
    'train_data',
    'tf_saved_model',
    'setup_script',
}


KERAS_H5PY_ERROR = RuntimeError(  # https://github.com/h5py/h5py/issues/1732
    "Keras encountered an error saving/loading the model due to a bug in h5py v3.0.0;"
    " consider downgrading with `pip install \"h5py<3.0.0\"`"
)


def validate_key(key):
    """
    Validates user-specified artifact key.

    Parameters
    ----------
    key : str
        Name of artifact.

    Raises
    ------
    ValueError
        If `key` is blocklisted.

    """
    if key in BLOCKLISTED_KEYS:
        msg = "\"{}\" is reserved for internal use; please use a different key".format(key)
        raise ValueError(msg)


def get_file_ext(file):
    """
    Obtain the filename extension of `file`.

    This method assumes `file` is accessible on the user's filesystem.

    Parameters
    ----------
    file : str or file handle
        Filepath or on-disk file stream.

    Returns
    -------
    str
        Filename extension without the leading period.

    Raises
    ------
    TypeError
        If a filepath cannot be obtained from the argument.
    ValueError
        If the filepath lacks an extension.

    """
    if isinstance(file, six.string_types) and not os.path.isdir(file):
        filepath = file
    elif hasattr(file, 'read') and hasattr(file, 'name'):  # `open()` object
        filepath = file.name
    else:
        raise TypeError("unable to obtain filepath from object of type {}".format(type(file)))

    filename = os.path.basename(filepath).lstrip('.')
    try:
        _, extension = filename.split(os.extsep, 1)
    except ValueError:
        six.raise_from(ValueError("no extension found in \"{}\"".format(filepath)),
                       None)
    else:
        return extension


def ext_from_method(method):
    """
    Returns an appropriate file extension for a given model serialization method.

    Parameters
    ----------
    method : str
        The return value of `method` from ``serialize_model()``.

    Returns
    -------
    str or None
        Filename extension without the leading period.

    """
    if method == "keras":
        return 'hdf5'
    elif method in ("joblib", "cloudpickle", "pickle"):
        return 'pkl'
    elif method == "zip":
        return "zip"
    elif method == ZIP_EXTENSION:  # zipped by client
        return ZIP_EXTENSION
    elif method is None:
        return None
    else:
        raise ValueError("unrecognized method value: {}".format(method))


def reset_stream(stream):
    """
    Resets the cursor of a stream to the beginning.

    This is implemented with a try-except because not all file-like objects are guaranteed to have
    a ``seek()`` method, so we carry on if we cannot reset the pointer.

    Parameters
    ----------
    stream : file-like
        A stream that may or may not implement ``seek()``.

    """
    try:
        stream.seek(0)
    except AttributeError:
        pass


def ensure_bytestream(obj):
    """
    Converts an object into a bytestream.

    If `obj` is file-like, its contents will be read into memory and then wrapped in a bytestream.
    This has a performance cost, but checking beforehand whether an arbitrary file-like object
    returns bytes rather than encoded characters is an implementation nightmare.

    If `obj` is not file-like, it will be serialized and then wrapped in a bytestream.

    Parameters
    ----------
    obj : file-like or object
        Object to convert into a bytestream.

    Returns
    -------
    bytestream : file-like
        Buffered bytestream of the serialized artifacts.
    method : {"joblib", "cloudpickle", "pickle", None}
        Serialization method used to produce the bytestream.

    Raises
    ------
    pickle.PicklingError
        If `obj` cannot be serialized.
    ValueError
        If `obj` contains no data.

    """
    if hasattr(obj, 'read'):  # if `obj` is file-like
        reset_stream(obj)  # reset cursor to beginning in case user forgot

        # read first element to check if bytes
        try:
            chunk = obj.read(1)
        except TypeError:  # read() doesn't take an argument
            pass  # fall through to read & cast full stream
        else:
            if chunk and isinstance(chunk, bytes):  # contents are indeed bytes
                reset_stream(obj)
                return obj, None
            else:
                pass  # fall through to read & cast full stream

        # read full stream and cast to bytes
        reset_stream(obj)
        contents = obj.read()  # read to cast into binary
        reset_stream(obj)  # reset cursor to beginning as a courtesy
        if not len(contents):
            # S3 raises unhelpful error on empty upload, so catch here
            raise ValueError("object contains no data")
        bytestring = six.ensure_binary(contents)
        bytestream = six.BytesIO(bytestring)
        bytestream.seek(0)
        return bytestream, None
    else:  # `obj` is not file-like
        bytestream = six.BytesIO()

        try:
            cloudpickle.dump(obj, bytestream)
        except pickle.PicklingError:  # can't be handled by cloudpickle
            pass
        else:
            bytestream.seek(0)
            return bytestream, "cloudpickle"

        if maybe_dependency("joblib"):
            try:
                maybe_dependency("joblib").dump(obj, bytestream)
            except (NameError,  # joblib not installed
                    pickle.PicklingError):  # can't be handled by joblib
                pass
            else:
                bytestream.seek(0)
                return bytestream, "joblib"

        try:
            pickle.dump(obj, bytestream)
        except pickle.PicklingError:  # can't be handled by pickle
            six.raise_from(pickle.PicklingError("unable to serialize artifact"), None)
        else:
            bytestream.seek(0)
            return bytestream, "pickle"


def serialize_model(model):
    """
    Serializes a model into a bytestream, attempting various methods.

    Parameters
    ----------
    model : object or file-like
        Model to convert into a bytestream.

    Returns
    -------
    bytestream : file-like
        Buffered bytestream of the serialized model.
    method : {"joblib", "cloudpickle", "pickle", "keras", None}
        Serialization method used to produce the bytestream.
    model_type : {"torch", "sklearn", "xgboost", "tensorflow", "custom", "callable"}
        Framework with which the model was built.

    """
    # if `model` is filesystem path
    if isinstance(model, six.string_types):
        if os.path.isdir(model):
            return zip_dir(model), ZIP_EXTENSION, None
        else:  # filepath
            # open and continue
            model = open(model, 'rb')

    # if `model` is file-like
    if hasattr(model, 'read'):
        try:  # attempt to deserialize
            reset_stream(model)  # reset cursor to beginning in case user forgot
            model = deserialize_model(model.read())
        except (TypeError, pickle.UnpicklingError):
            # unrecognized serialization method and model type
            return model, None, None  # return bytestream
        finally:
            reset_stream(model)  # reset cursor to beginning as a courtesy

    # if `model` is a class
    if isinstance(model, six.class_types):
        model_type = "class"
        bytestream, method = ensure_bytestream(model)
        return bytestream, method, model_type

    # if`model` is an instance
    pyspark_ml_base = maybe_dependency("pyspark.ml.base")
    if pyspark_ml_base:
        # https://spark.apache.org/docs/latest/api/python/_modules/pyspark/ml/base.html
        pyspark_base_classes = (
            pyspark_ml_base.Estimator,
            pyspark_ml_base.Model,
            pyspark_ml_base.Transformer,
        )
        if isinstance(model, pyspark_base_classes):
            temp_dir = tempfile.mkdtemp()
            try:
                spark_model_dir = os.path.join(temp_dir, "spark-model")
                model.save(spark_model_dir)
                bytestream = zip_dir(spark_model_dir)
            finally:
                shutil.rmtree(temp_dir)
            # TODO: see if more info would be needed to deserialize in model service
            return bytestream, "zip", "pyspark"
    for class_obj in model.__class__.__mro__:
        module_name = class_obj.__module__
        if not module_name:
            continue
        elif module_name.startswith("torch"):
            model_type = "torch"
            bytestream, method = ensure_bytestream(model)
            break
        elif module_name.startswith("sklearn"):
            model_type = "sklearn"
            bytestream, method = ensure_bytestream(model)
            break
        elif module_name.startswith("xgboost"):
            model_type = "xgboost"
            bytestream, method = ensure_bytestream(model)
            break
        elif module_name.startswith("tensorflow.python.keras"):
            model_type = "tensorflow"
            tempf = tempfile.NamedTemporaryFile()
            try:
                if get_tensorflow_major_version() == 2:  # save_format param may not exist in TF 1.X
                    model.save(tempf.name, save_format='h5')  # TF 2.X uses SavedModel by default
                else:
                    model.save(tempf.name)
            except TypeError as e:
                h5py = maybe_dependency("h5py")
                if (str(e) == "a bytes-like object is required, not 'str'"
                        and h5py is not None and h5py.__version__ == "3.0.0"):
                    # h5py v3.0.0 improperly checks if a `bytes` contains a `str`.
                    # Encountering this generic error message here plus the fact
                    # that h5py==3.0.0 suggests that this is the problem.
                    six.raise_from(KERAS_H5PY_ERROR, e)
                else:
                    six.raise_from(e, None)
            tempf.seek(0)
            bytestream = tempf
            method = "keras"
            break
    else:
        if hasattr(model, 'predict'):
            model_type = "custom"
        elif callable(model):
            model_type = "callable"
        else:
            model_type = None
        bytestream, method = ensure_bytestream(model)
    return bytestream, method, model_type


def deserialize_model(bytestring, error_ok=False):
    """
    Deserializes a model from a bytestring, attempting various methods.

    If the model is unable to be deserialized, the bytes will be returned as a buffered bytestream.

    Parameters
    ----------
    bytestring : bytes
        Bytes representing the model.
    error_ok : bool, default False
        Whether to return the serialized bytes if the model cannot be
        deserialized. If False, an ``UnpicklingError`` is raised instead.

    Returns
    -------
    model : obj or file-like
        Model or buffered bytestream representing the model.

    Raises
    ------
    pickle.UnpicklingError
        If `bytestring` cannot be deserialized into an object, and `error_ok`
        is False.

    """
    keras = maybe_dependency("tensorflow.keras")
    if keras is not None:
        # try deserializing with Keras (HDF5)
        with tempfile.NamedTemporaryFile() as tempf:
            tempf.write(bytestring)
            tempf.seek(0)
            try:
                return keras.models.load_model(tempf.name)
            except AttributeError as e:
                h5py = maybe_dependency("h5py")
                if (str(e) == "'str' object has no attribute 'decode'"
                        and h5py is not None and h5py.__version__ == "3.0.0"):
                    # h5py v3.0.0 returns a `str` instead of a `bytes` to Keras.
                    # Encountering this generic error message here plus the fact
                    # that h5py==3.0.0 suggests that this is the problem.
                    six.raise_from(KERAS_H5PY_ERROR, e)
                else:
                    six.raise_from(e, None)
            except (NameError,  # Tensorflow not installed
                    IOError, OSError):  # not a Keras model
                pass

    bytestream = six.BytesIO(bytestring)
    torch = maybe_dependency("torch")
    if torch is not None:
        try:
            return torch.load(bytestream)
        except:  # not something torch can deserialize
            bytestream.seek(0)

    # try deserializing with cloudpickle
    try:
        return cloudpickle.load(bytestream)
    except:  # not a pickled object
        bytestream.seek(0)

    if error_ok:
        return bytestream
    else:
        raise pickle.UnpicklingError("unable to deserialize model")


def get_stream_length(stream, chunk_size=_5MB):
    """
    Get the length of the contents of a stream.

    Parameters
    ----------
    stream : file-like
        Stream.
    chunk_size : int, default 5 MB
        Number of bytes (or whatever `stream` contains) to read into memory at a time.

    Returns
    -------
    length : int
        Length of `stream`.

    """
    # if it's file handle, get file size without reading stream
    filename = getattr(stream, 'name', None)
    if filename is not None:
        try:
            return os.path.getsize(filename)
        except OSError:  # can't access file
            pass

    # read stream in chunks to get length
    length = 0
    try:
        part_lengths = iter(lambda: len(stream.read(chunk_size)), 0)
        for part_length in part_lengths:  # could be sum() but not sure GC runs during builtin one-liner
            length += part_length
    finally:
        reset_stream(stream)  # reset cursor to beginning as a courtesy

    return length


def calc_sha256(bytestream, chunk_size=_5MB):
    """
    Calculates the SHA-256 checksum of a bytestream.

    Parameters
    ----------
    bytestream : file-like opened in binary mode
        Bytestream.
    chunk_size : int, default 5 MB
        Number of bytes to read into memory at a time.

    Returns
    -------
    checksum : str
        SHA-256 hash of `bytestream`'s contents.

    Raises
    ------
    TypeError
        If `bytestream` is opened in text mode instead of binary mode.

    """
    checksum = hashlib.sha256()

    try:
        parts = iter(lambda: bytestream.read(chunk_size), b'')
        for part in parts:
            checksum.update(part)
    finally:
        reset_stream(bytestream)  # reset cursor to beginning as a courtesy

    return checksum.hexdigest()


def zip_dir(dirpath, followlinks=True):
    """
    ZIPs a directory.

    Parameters
    ----------
    dirpath : str
        Directory path.

    Returns
    -------
    tempf : :class:`tempfile.NamedTemporaryFile`
        ZIP file handle.

    """
    e_msg = "{} is not a directory".format(str(dirpath))
    if not isinstance(dirpath, six.string_types):
        raise TypeError(e_msg)
    if not os.path.isdir(dirpath):
        raise ValueError(e_msg)

    os.path.expanduser(dirpath)

    tempf = tempfile.NamedTemporaryFile(suffix='.'+ZIP_EXTENSION)
    with zipfile.ZipFile(tempf, 'w') as zipf:
        for root, _, files in os.walk(dirpath, followlinks=followlinks):
            for filename in files:
                filepath = os.path.join(root, filename)
                zipf.write(filepath, os.path.relpath(filepath, dirpath))

    tempf.seek(0)
    return tempf


def global_read_zipinfo(filename):
    """
    Returns a :class:`zipfile.ZipInfo` with ``644`` permissions.

    :meth:`zipfile.ZipFile.writestr` creates files with ``600`` [1]_ [2]_,
    which means non-owners are unable to read the file, which can be
    problematic for custom modules in deployment.

    Parameters
    ----------
    filename : str
        Name to assign to the file in the ZIP archive.

    Returns
    -------
    zip_info : :class:`zipfile.ZipInfo`
        File metadata; the first arg to :meth:`zipfile.ZipFile.writestr`.

    References
    ----------
    .. [1] https://github.com/python/cpython/blob/2.7/Lib/zipfile.py#L1244

    .. [2] https://bugs.python.org/msg69937

    """
    zip_info = zipfile.ZipInfo(filename)
    zip_info.external_attr = 0o644 << 16  # ?rw-r--r--

    return zip_info
