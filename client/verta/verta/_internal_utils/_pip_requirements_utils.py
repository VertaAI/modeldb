# -*- coding: utf-8 -*-

from __future__ import print_function

import importlib
import re
import subprocess
import sys
import warnings

import cloudpickle

from ..external import six

from .. import __about__


# for process_requirements()
PYPI_TO_IMPORT = {
    'scikit-learn': "sklearn",
    'tensorflow-gpu': "tensorflow",
    'beautifulsoup4': "bs4",
}
IMPORT_TO_PYPI = {  # separate mapping because PyPI to import is surjective
    'sklearn': "scikit-learn",
    'bs4': "beautifulsoup4",
}


PKG_NAME_PATTERN = r"([A-Z0-9][A-Z0-9._-]*[A-Z0-9]|[A-Z0-9])"  # https://www.python.org/dev/peps/pep-0508/#names
VER_SPEC_PATTERN = r"(~=|==|!=|<=|>=|<|>|===)"  # https://www.python.org/dev/peps/pep-0440/#version-specifiers
VER_NUM_PATTERN = r"([0-9]+(?:\.[0-9]+){0,2}[^\s]*)"  # https://www.python.org/dev/peps/pep-0440/#version-scheme
REQ_SPEC_PATTERN = (
    PKG_NAME_PATTERN + r"\s*"
    + VER_SPEC_PATTERN + r"\s*"
    + VER_NUM_PATTERN
)
SPACY_MODEL_PATTERN = r"[a-z]{2}(?:[_-][a-z]+){2}[_-](?:sm|md|lg)"  # https://spacy.io/models#conventions
PKG_NAME_REGEX = re.compile(PKG_NAME_PATTERN, flags=re.IGNORECASE)
VER_SPEC_REGEX = re.compile(VER_SPEC_PATTERN)
VER_NUM_REGEX = re.compile(VER_NUM_PATTERN)
REQ_SPEC_REGEX = re.compile(REQ_SPEC_PATTERN, flags=re.IGNORECASE)
SPACY_MODEL_REGEX = re.compile(SPACY_MODEL_PATTERN)


def get_pip_freeze():
    pip_freeze = subprocess.check_output([sys.executable, '-m', 'pip', 'freeze'])
    pip_freeze = six.ensure_str(pip_freeze)

    req_specs = pip_freeze.splitlines()

    req_specs = clean_reqs_file_lines(req_specs)

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
        raise ValueError("\"{}\" does not appear to be a valid pip requirement specifier;"
                         " it may be misspelled or missing its version specifier".format(req_spec))

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
        raise ValueError("\"{}\" does not appear to be a valid version number".format(version))

    MAJOR_REGEX = re.compile(r"^([0-9]+)")
    MINOR_OR_PATCH_REGEX = re.compile(r"^(\.[0-9]+)")

    # extract major version
    split = MAJOR_REGEX.split(version, maxsplit=1)[1:]  # first element is empty
    major = int(split[0])
    suffix = ''.join(split[1:])

    # extract minor version
    if MINOR_OR_PATCH_REGEX.match(suffix):
        split = MINOR_OR_PATCH_REGEX.split(suffix, maxsplit=1)[1:]  # first element is empty
        minor = int(split[0][1:])  # first character is period
        suffix = ''.join(split[1:])
    else:
        minor = 0

    # extract patch version
    if MINOR_OR_PATCH_REGEX.match(suffix):
        split = MINOR_OR_PATCH_REGEX.split(suffix, maxsplit=1)[1:]  # first element is empty
        patch = int(split[0][1:])  # first character is period
        suffix = ''.join(split[1:])
    else:
        patch = 0

    return major, minor, patch, suffix


def process_requirements(requirements):
    """
    Validates `requirements` against packages available in the current environment.

    Parameters
    ----------
    requirements : list of str
        PyPI package names.

    Raises
    ------
    ValueError
        If a package's name is invalid for PyPI, or its exact version cannot be determined.

    """
    # validate package names
    for req in requirements:
        if not PKG_NAME_REGEX.match(req):
            raise ValueError("'{}' does not appear to be a valid PyPI-installable package;"
                             " please check its spelling,"
                             " or file an issue if you believe it is in error".format(req))

    strip_inexact_specifiers(requirements)

    set_version_pins(requirements)

    add_verta_and_cloudpickle(requirements)


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
        elif '==' in ver_spec:
            continue
        else:
            msg = ("'{}' does not use '=='; for reproducibility in deployment, it will be replaced"
                   " with an exact pin of the currently-installed version".format(req))
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
        req_spec.split('==')
        for req_spec
        in six.ensure_str(subprocess.check_output([sys.executable, '-m', 'pip', 'freeze'])).splitlines()
        if '==' in req_spec
    )

    # replace importable module names with PyPI package names in case of user error
    for i, req in enumerate(requirements):
        requirements[i] = IMPORT_TO_PYPI.get(req, req)

    for i, req in enumerate(requirements):
        error = ValueError("unable to determine a version number for requirement '{}';"
                           " it might not be installed;"
                           " please manually specify it as '{}==x.y.z'".format(req, req))
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


def add_verta_and_cloudpickle(requirements):
    """
    Adds verta and cloudpickle to `requirements`, pinning their versions from the environment.

    verta and cloudpickle are required for deployment, but a user might not have specified them in
    their manual deployment requirements.

    Parameters
    ----------
    requirements : list of str

    Raises
    ------
    ValueError
        If verta or cloudpickle already have a version pin specified in `requirements`, but it
        conflicts with the version in the current environment.

    """
    # add verta
    verta_req = "verta=={}".format(__about__.__version__)
    for req in requirements:
        if req.startswith("verta"):  # if present, check version
            our_ver = verta_req.split('==')[-1]
            their_ver = req.split('==')[-1]
            if our_ver != their_ver:  # versions conflict, so raise exception
                raise ValueError("Client is running with verta v{}, but the provided requirements specify v{};"
                                 " these must match".format(our_ver, their_ver))
            else:  # versions match, so proceed
                break
    else:  # if not present, add
        requirements.append(verta_req)

    # add cloudpickle
    cloudpickle_req = "cloudpickle=={}".format(cloudpickle.__version__)
    for req in requirements:
        if req.startswith("cloudpickle"):  # if present, check version
            our_ver = cloudpickle_req.split('==')[-1]
            their_ver = req.split('==')[-1]
            if our_ver != their_ver:  # versions conflict, so raise exception
                raise ValueError("Client is running with cloudpickle v{}, but the provided requirements specify v{};"
                                 " these must match".format(our_ver, their_ver))
            else:  # versions match, so proceed
                break
    else:  # if not present, add
        requirements.append(cloudpickle_req)


def clean_reqs_file_lines(requirements):
    """
    Performs basic preprocessing on a requirements file's lines so it's easier to handle downstream.

    Parameters
    ----------
    requirements : list of str
        ``requirements_file.readlines()``.

    Returns
    -------
    cleaned_requirements : list of str
        Requirement specifiers.

    """
    requirements = [req.strip() for req in requirements]

    requirements = [req for req in requirements if req]  # empty line
    requirements = [req for req in requirements if not req.startswith('#')]  # comment line

    # remove unsupported options
    supported_requirements = []
    for req in requirements:
        # https://pip.pypa.io/en/stable/reference/pip_install/#requirements-file-format
        if req.startswith(('--', '-c ', '-f ', '-i ')):
            print("skipping unsupported option \"{}\"".format(req))
            continue
        # https://pip.pypa.io/en/stable/reference/pip_install/#vcs-support
        # TODO: upgrade protos and Client to handle VCS-installed packages
        if req.startswith(('-e ', 'git:', 'git+', 'hg+', 'svn+', 'bzr+')):
            print("skipping unsupported VCS-installed package \"{}\"".format(req))
            continue
        # TODO: follow references to other requirements files
        if req.startswith('-r '):
            print("skipping unsupported file reference \"{}\"".format(req))
            continue
        # non-PyPI-installable spaCy models
        if SPACY_MODEL_REGEX.match(req):
            print("skipping non-PyPI-installable spaCy model \"{}\"".format(req))
            continue

        supported_requirements.append(req)

    return supported_requirements
