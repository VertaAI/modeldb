# -*- coding: utf-8 -*-

from __future__ import print_function

import copy
import importlib
import re
import subprocess
import sys
import warnings

import cloudpickle

from .._vendored import six

from .. import __about__


# for set_version_pins()
PYPI_TO_IMPORT = {
    "scikit-learn": "sklearn",
    "tensorflow-gpu": "tensorflow",
    "beautifulsoup4": "bs4",
}
IMPORT_TO_PYPI = {  # separate mapping because PyPI to import is surjective
    "sklearn": "scikit-learn",
    "bs4": "beautifulsoup4",
}


PKG_NAME_PATTERN = r"([A-Z0-9][A-Z0-9._-]*[A-Z0-9]|[A-Z0-9])"  # https://www.python.org/dev/peps/pep-0508/#names
VER_SPEC_PATTERN = r"(~=|==|!=|<=|>=|<|>|===)"  # https://www.python.org/dev/peps/pep-0440/#version-specifiers
VER_NUM_PATTERN = r"([0-9]+(?:\.[0-9]+){0,2}[^\s]*)"  # https://www.python.org/dev/peps/pep-0440/#version-scheme
REQ_SPEC_PATTERN = (
    PKG_NAME_PATTERN + r"\s*" + VER_SPEC_PATTERN + r"\s*" + VER_NUM_PATTERN
)
SPACY_MODEL_PATTERN = (
    r"[a-z]{2}(?:[_-][a-z]+){2}[_-](?:sm|md|lg)"  # https://spacy.io/models#conventions
)
PKG_NAME_REGEX = re.compile(PKG_NAME_PATTERN, flags=re.IGNORECASE)
VER_SPEC_REGEX = re.compile(VER_SPEC_PATTERN)
VER_NUM_REGEX = re.compile(VER_NUM_PATTERN)
REQ_SPEC_REGEX = re.compile(REQ_SPEC_PATTERN, flags=re.IGNORECASE)
SPACY_MODEL_REGEX = re.compile(SPACY_MODEL_PATTERN)


def get_pip_freeze():
    """Get pip requirement specifiers returned by ``pip freeze``.

    .. versionchanged:: 0.19.2

        See note below.

    .. note::

        This function returns the output of ``pip freeze`` as-is, which could
        contain VCS-installed packages and spaCy-installed models (which are
        not themselves pip-installable). The caller is responsible for any
        necessary post-processing, e.g. calling :func:`clean_reqs_file_lines`.

    Returns
    -------
    requirements : list of str
        Requirement specifiers.

    """
    pip_freeze = subprocess.check_output([sys.executable, "-m", "pip", "freeze"])
    pip_freeze = six.ensure_str(pip_freeze)

    req_specs = pip_freeze.splitlines()

    return req_specs


def parse_req_spec(req_spec):
    """
    Parses a requirement specifier into its components.

    Parameters
    ----------
    req_spec : str
        e.g. "banana >= 3.6.0"

    Returns
    -------
    library : str
        e.g. "banana"
    constraint : str
        e.g. ">="
    version : str
        e.g. "3.6.0"

    """
    match = REQ_SPEC_REGEX.match(req_spec)
    if match is None:
        raise ValueError(
            '"{}" does not appear to be a valid pip requirement specifier;'
            " it may be misspelled or missing its version specifier".format(req_spec)
        )

    return match.groups()


def parse_version(version):
    """
    Parses a version number into its components.

    A missing component will be returned as a ``0``.

    Parameters
    ----------
    version : str
        e.g. "3.6"

    Returns
    -------
    major : int
        e.g. 3
    minor : int
        e.g. 6
    patch : int
        e.g. 0
    suffix : str
        Additional characters, such as build metadata or sub-patch release numbers.

    """
    if VER_NUM_REGEX.match(version) is None:
        raise ValueError(
            '"{}" does not appear to be a valid version number'.format(version)
        )

    MAJOR_REGEX = re.compile(r"^([0-9]+)")
    MINOR_OR_PATCH_REGEX = re.compile(r"^(\.[0-9]+)")

    # extract major version
    split = MAJOR_REGEX.split(version, maxsplit=1)[1:]  # first element is empty
    major = int(split[0])
    suffix = "".join(split[1:])

    # extract minor version
    if MINOR_OR_PATCH_REGEX.match(suffix):
        split = MINOR_OR_PATCH_REGEX.split(suffix, maxsplit=1)[
            1:
        ]  # first element is empty
        minor = int(split[0][1:])  # first character is period
        suffix = "".join(split[1:])
    else:
        minor = 0

    # extract patch version
    if MINOR_OR_PATCH_REGEX.match(suffix):
        split = MINOR_OR_PATCH_REGEX.split(suffix, maxsplit=1)[
            1:
        ]  # first element is empty
        patch = int(split[0][1:])  # first character is period
        suffix = "".join(split[1:])
    else:
        patch = 0

    return major, minor, patch, suffix


def must_all_valid_package_names(requirements):
    """Assert that each line in `requirements` is for a valid pip package.

    Parameters
    ----------
    requirements : list of str

    Raises
    ------
    ValueError
        If a line in `requirements` does not have a valid pip package name.

    """
    for req in requirements:
        if not req:
            continue  # allow empty lines
        if not PKG_NAME_REGEX.match(req):
            raise ValueError(
                "'{}' does not appear to be a valid PyPI-installable package;"
                " please check its spelling,"
                " or file an issue if you believe it is in error".format(req)
            )


def strip_inexact_specifiers(requirements):
    """
    Removes any version specifier that is not ``==``, leaving just the package name.

    Parameters
    ----------
    requirements : list of str

    Warns
    -----
    UserWarning
        If a requirement specifier uses version specifier other than ``==``, to inform the user
        that it will be replaced with an exact version pin.

    """
    for i, req in enumerate(requirements):
        _, pkg, ver_spec = PKG_NAME_REGEX.split(req, maxsplit=1)
        if not ver_spec:
            continue
        elif "==" in ver_spec:
            continue
        else:
            msg = (
                "'{}' does not use '=='; for reproducibility in deployment, it will be replaced"
                " with an exact pin of the currently-installed version".format(req)
            )
            warnings.warn(msg)
            requirements[i] = pkg


def set_version_pins(requirements):
    """
    Sets version pins for packages in `requirements`.

    Parameters
    ----------
    requirements : list of str

    Notes
    -----
    This function attempts an import of each package and checks its version using the module's
    ``__version__`` attribute. This can lead to problems if the package is not importable (e.g.
    PyPI name is different from its package module name) or if it does not have supply
    ``__version__``.

    This approach is taken because Python package management is complete anarchy, and the Client
    can't determine whether the environment is using pip or conda in order to check the installed
    version directly from the environment.

    """
    # map of packages to their versions according to pip
    pip_pkg_vers = dict(
        req_spec.split("==") for req_spec in get_pip_freeze() if "==" in req_spec
    )

    # replace importable module names with PyPI package names in case of user error
    for i, req in enumerate(requirements):
        requirements[i] = IMPORT_TO_PYPI.get(req, req)

    for i, req in enumerate(requirements):
        error = ValueError(
            "unable to determine a version number for requirement '{}';"
            " it might not be installed;"
            " please manually specify it as '{}==x.y.z'".format(req, req)
        )
        if VER_SPEC_REGEX.search(req) is None:
            # obtain package version
            try:
                mod_name = PYPI_TO_IMPORT.get(req, req)
                mod = importlib.import_module(mod_name)
                ver = mod.__version__
            except (ImportError, AttributeError):
                # fall back to checking pip
                req_with_dash = req.replace("_", "-")
                req_with_underscore = req.replace("-", "_")
                if req_with_dash in pip_pkg_vers:
                    ver = pip_pkg_vers[req_with_dash]
                elif req_with_underscore in pip_pkg_vers:
                    ver = pip_pkg_vers[req_with_underscore]
                else:
                    six.raise_from(error, None)

            requirements[i] = req + "==" + ver


def pin_verta_and_cloudpickle(requirements):
    """Add verta and cloudpickle to `requirements`, pinning their versions from the environment.

    Model deserilization in most cases requires that ``verta`` and
    ``cloudpickle`` have the same versions as when the model was serialized.

    Parameters
    ----------
    requirements : list of str

    Returns
    -------
    list of str
        Copy of `requirements` with pinned verta and cloudpickle.

    """
    requirements = copy.copy(requirements)

    # replace git-installed verta (`pip install -e verta`) with "verta"
    for i, req in enumerate(requirements):
        if "VertaAI/modeldb.git" in req and "#egg=verta" in req:
            # git+git is not installable
            #     https://github.com/pypa/pip/issues/7554
            #     https://github.com/pypa/pip/issues/9625
            #     https://github.com/pypa/pip/pull/9436
            #     https://github.com/pypa/pip/pull/9822
            # git+ssh is not installable without ssh credentials
            # TODO: maybe allow git+https because it's usable
            requirements[i] = __about__.__title__
            continue

    for library, version in [
        (__about__.__title__, __about__.__version__),  # verta
        (cloudpickle.__name__, cloudpickle.__version__),  # cloudpickle
    ]:
        requirements = inject_requirement(
            requirements, library, version, error_on_conflict=False
        )

    return requirements


def inject_requirement(requirements, library, version, error_on_conflict=True):
    """Return a copy of `requirements` with `library` pinned to `version`.

    If `library` is already in `requirements` and has a version pin, this
    function asserts that the pinned version matches the the current
    environment's version.

    If `library` is already in `requirements` without a version pin, it will
    be pinned to the current environment's version.

    Parameters
    ----------
    requirements : list of str
        pip requirements.
    library : str
        Python library, e.g. "verta".
    version : str
        Desired version number, e.g. "0.20.0".
    error_on_conflict : bool, default True
        If true, raise a `ValueError` on version mismatches. Otherwise issue a
        warning. Defaults to true.

    Returns
    -------
    list of str
        Copy of `requirements` with `library` pinned to `version`.

    Raises
    ------
    ValueError
        If the provided library version conflicts with an already pinned version
        of the library and `error_on_conflict` is True.

    """
    requirements = copy.copy(requirements)
    pinned_library_req = "{}=={}".format(library, version)

    if not any(req.startswith(library) for req in requirements):  # not present: add
        requirements.append(pinned_library_req)
    else:  # present: pin version
        for i, req in enumerate(requirements):
            if req.startswith(library):
                if "==" in req:  # version pin: check version
                    their_ver = req.split("==")[-1]
                    if version != their_ver:  # versions conflict: raise exception
                        msg = (
                            "Client is running with {} v{}, but the provided requirements specify v{};"
                            " these should match"
                        ).format(library, version, their_ver)
                        if error_on_conflict:
                            raise ValueError(msg)
                        else:
                            warnings.warn(msg, stacklevel=2)
                    else:  # versions match: no action needed
                        continue
                # TODO: check other operators (>=, >, ...)
                else:  # no version pin: set
                    requirements[i] = preserve_req_suffixes(
                        req,
                        pinned_library_req,
                    )
                    continue

    return requirements


def remove_pinned_requirements(requirements, library_patterns):
    """Remove pinned libraries matching `library_patterns` from `requirements`.

    Parameters
    ----------
    requirements : list of str
        pip requirements, usually directly from :meth:`get_pip_freeze`.
    library_patterns : list of str
        Regex patterns for libraries to remove.

    Returns
    -------
    list of str
        Copy of `requirements` with `library_patterns` removed.

    """
    # append == to specifically match version pins (and not @-style VCS installs)
    library_patterns = map("{}==".format, library_patterns)
    # compile regex to slightly speed up pattern-matching
    library_patterns = list(map(re.compile, library_patterns))

    return list(
        filter(
            lambda req: not any(pattern.match(req) for pattern in library_patterns),
            requirements,
        )
    )


def preserve_req_suffixes(requirement, pinned_library_req):
    """Swap in `pinned_library_req` while preserving `requirement`'s environment marker and comment.

    Parameters
    ----------
    requirement : str
        A line from a pip requirements file.
    pinned_library_req : str
        Library + version pin, e.g. ``"verta==0.20.0"``.

    Returns
    -------
    str

    Examples
    --------
    .. code-block:: python

        assert preserve_req_suffixes(
            "verta;python_version>'3.8' and python_version<'3.10'  # very important!",
            "verta==0.20.0",
        ) == "verta==0.20.0;python_version>'3.8' and python_version<'3.10'  # very important!"

        assert preserve_req_suffixes(
            "verta;python_version<='3.8'",
            "verta==0.20.0",
        ) == "verta==0.20.0;python_version<='3.8'"

        assert preserve_req_suffixes(
            "verta  # very important!",
            "verta==0.20.0",
        ) == "verta==0.20.0 # very important!"

        assert preserve_req_suffixes(
            "verta",
            "verta==0.20.0",
        ) == "verta==0.20.0"

    """
    for delimiter in [
        ";",  # environment marker
        " #",  # comment
    ]:
        if delimiter not in requirement:
            continue
        split_req = requirement.split(delimiter)
        split_req[0] = pinned_library_req
        return delimiter.join(split_req)
    return pinned_library_req


def remove_local_version_identifier(requirements):
    """Removes local version identifiers from version pins if present.

    PyTorch in particular adds local build information to its pip environment
    version number during installation. This suffix results in a version
    specifier that won't be installable from PyPI [#]_ and therefore needs to
    be removed for model deployment.

    Parameters
    ----------
    requirements : list of str
        pip requirements with pinned version numbers.

    Examples
    --------
    .. code-block:: python

        before = ["torch==1.8.1+cu102"]
        after = ["torch==1.8.1"]

        remove_local_version_identifier(before)
        assert before == after

    References
    ----------
    .. [#] https://www.python.org/dev/peps/pep-0440/#local-version-identifiers

    """
    for i, req in enumerate(requirements):
        library, version = req.split("==", 1)
        requirements[i] = "==".join(
            [
                library,
                version.split("+")[0],
            ]
        )


def clean_reqs_file_lines(requirements, ignore_unsupported=True):
    """
    Performs basic preprocessing on a requirements file's lines so it's easier to handle downstream.

    Parameters
    ----------
    requirements : list of str
        ``requirements_file.readlines()``.
    ignore_unsupported : bool, default True
        If ``True``, skip unsupported lines in the requirements file. If
        ``False``, raise an exception instead.

    Returns
    -------
    cleaned_requirements : list of str
        Requirement specifiers.

    Raises
    ------
    ValueError
        If `ignore_unsupported` is ``False`` and an unsupported line is
        present in `requirements`.

    """
    requirements = [req.strip() for req in requirements]

    requirements = [req for req in requirements if req]  # empty line
    requirements = [
        req for req in requirements if not req.startswith("#")
    ]  # comment line

    # check for unsupported options
    supported_requirements = []
    for req in requirements:
        unsupported_reason = None

        # https://pip.pypa.io/en/stable/cli/pip_install/#requirements-file-format
        if req.startswith(("--", "-c ", "-f ", "-i ")):
            unsupported_reason = 'unsupported option "{}"'.format(req)
        # https://pip.pypa.io/en/stable/topics/vcs-support/
        elif req.startswith(("-e ", "git:", "git+", "hg+", "svn+", "bzr+")):
            unsupported_reason = 'unsupported VCS-installed package "{}"'.format(req)
        elif req.startswith("-r "):
            unsupported_reason = 'unsupported file reference "{}"'.format(req)
        # https://www.python.org/dev/peps/pep-0508/#environment-markers
        elif ";" in req:
            unsupported_reason = 'unsupported environment marker "{}"'.format(req)
        # non-PyPI-installable spaCy models
        elif SPACY_MODEL_REGEX.match(req):
            unsupported_reason = 'non-PyPI-installable spaCy model "{}"'.format(req)

        if unsupported_reason:
            if ignore_unsupported:
                print("skipping {}".format(unsupported_reason))
                continue
            else:
                raise ValueError(unsupported_reason)

        supported_requirements.append(req)

    return supported_requirements
