# -*- coding: utf-8 -*-

import hashlib
import os
import tempfile

import cloudpickle

from ..external import six
from ..external.six.moves import cPickle as pickle  # pylint: disable=import-error, no-name-in-module

from .. import __about__

from .importer import maybe_dependency, get_tensorflow_major_version


# default for chunked utils
CHUNK_SIZE = 5*10**6


# NOTE: changing this might break multipart upload continuation
MULTIPART_UPLOAD_PART_SIZE = 64*(10**6)  # 64 MB


# NOTE: keep up-to-date with Deployment API
BLACKLISTED_KEYS = {
    'model_api.json',
    'model.pkl',
    'requirements.txt',
    'train_data',
    'tf_saved_model',
    'custom_modules',
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
        If `key` is blacklisted.

    """
    if key in BLACKLISTED_KEYS:
        msg = "\"{}\" is reserved for internal use; please use a different key".format(key)
        raise ValueError(msg)


def get_file_ext(file):
    """
    Obtain the filename extension of `file`.

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
    if isinstance(file, six.string_types):
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
    if hasattr(model, 'read'):  # if `model` is file-like
        try:  # attempt to deserialize
            reset_stream(model)  # reset cursor to beginning in case user forgot
            model = deserialize_model(model.read())
        except pickle.UnpicklingError:  # unrecognized model
            bytestream, _ = ensure_bytestream(model)  # pass along file-like
            method = None
            model_type = "custom"
        finally:
            reset_stream(model)  # reset cursor to beginning as a courtesy

    # `model` is a class
    if isinstance(model, six.class_types):
        model_type = "class"
        bytestream, method = ensure_bytestream(model)
        return bytestream, method, model_type

    # `model` is an instance
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
            bytestream, method = ensure_bytestream(model)
        elif callable(model):
            model_type = "callable"
            bytestream, method = ensure_bytestream(model)
        else:
            raise TypeError("cannot determine the type for model argument")
    return bytestream, method, model_type


def deserialize_model(bytestring):
    """
    Deserializes a model from a bytestring, attempting various methods.

    If the model is unable to be deserialized, the bytes will be returned as a buffered bytestream.

    Parameters
    ----------
    bytestring : bytes
        Bytes representing the model.

    Returns
    -------
    model : obj or file-like
        Model or buffered bytestream representing the model.

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

    return bytestream


def get_stream_length(stream, chunk_size=CHUNK_SIZE):
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


def calc_sha256(bytestream, chunk_size=CHUNK_SIZE):
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
