## A note on running tests against our OSS images

Our test suite is intended to cover the full breadth of our platform's functionality as experienced through the client. As a result, several of the integration tests **may fail with missing endpoints / `404`s** when run against our open-source images as we work on new functionality.

A subset of tests can be run solely on open-source functionality (detailed in **Running Tests**), but certain features may not be testable at all on our open-source platform.

## Installation

To install the packages needed for running tests, use [`requirements-dev.txt`](../requirements-dev.txt).

Also, set the following environment variables, which are used by the test suite for ModelDB integration tests:
- `VERTA_HOST` e.g. `http://localhost:3000` or `app.verta.ai`
- `VERTA_EMAIL`
- `VERTA_DEV_KEY`
- `VERTA_S3_TEST_BUCKET` (specified bucket must exist, the tests will not create it)
- `VERTA_S3_TEST_OBJECT` (specified object must exist, the tests will not create it)

## Running Tests

To execute the full test suite, run:
```bash
pytest
```
`pytest` automatically runs any `test_*()` method in any `Test*` class in any `test_*.py` file.

Add the `--oss` option to only run ModelDB OSS-compatible tests:
```bash
pytest --oss
```

You can also specify what group of tests to run:
```bash
pytest test_entities.py # specific script
pytest test_entities.py::TestProject # specific class
pytest test_entities.py::TestProject::test_create # specific function within specific class
```

Tests can also be run by specifying markers (defined in [`pytest.ini`](pytest.ini)):
```bash
pytest -m deployment
```

### Pytest invocation flags

`pytest` has a few flags that can be mixed and matched to customize output while tests are running.

`-s` outputs `stdout`:

```bash
[TEST LOG] test setup begun 2021-09-29 20:06:22.250206 UTC
[DEBUG] using email: *****@**********.***
[DEBUG] using developer key: ********-****-****-****-************
connection successfully established
created new RegisteredModel: Model 689611632945983186997 in workspace: Testing
created new RegisteredModel: 689611632945984004339 in workspace: Testing
got existing RegisteredModel: 689611632945984004339
.
[TEST LOG] test teardown completed 2021-09-29 20:06:27.799351 UTC
```

`-v` increases verbosity for a handful of aspects, including displaying the name of each test:

```bash
bases/test_deployable_entity.py::TestBuildArtifactStorePath::test_with_ext PASSED                [ 33%]
bases/test_deployable_entity.py::TestBuildArtifactStorePath::test_no_ext PASSED                  [ 66%]
bases/test_deployable_entity.py::TestCreateArtifactMsg::test_with_ext PASSED                     [100%]
```

`-rfE` outputs a summary (`r`) when tests are complete, listing failures (`f`) and errors (`E`):

```bash
================================== short test summary info ===================================
FAILED versioning/environment/test_environment.py::TestEnvironmentVariables::test_empty[None]
```

## Writing tests

Tests are loosely organized by files and classes of related functionality. See [`versioning/`](https://github.com/VertaAI/modeldb/tree/main/client/verta/tests/versioning) or [`registry/`](https://github.com/VertaAI/modeldb/tree/main/client/verta/tests/registry) for decent examples.

Note that for CLI tests, `click` provides [its own testing utilities](https://click.palletsprojects.com/en/7.x/testing/). See [`test_cli.py`](test_cli.py) for examples.

### Fixtures

[`pytest` fixtures](https://docs.pytest.org/en/stable/fixture.html) are reusable items that are passed to test functions.
Most fixtures are defined in [`conftest.py`](conftest.py).

To use a fixture: simply write the name of the fixture as a parameter to your test function; `pytest` will automatically pass it in at runtime.

To write a fixture: write code for setup, `yield` the object that should be passed to the test function, then write code for cleanup.

### Parametrization

The `pytest.mark.parametrize` decorator is used to re-run a test function for a series of similar arguments.

For example, we use it [here](https://github.com/VertaAI/modeldb/blob/b1c0106/client/verta/tests/registry/test_model.py#L137-L150) to verify that every one of our registry task type objects can be correctly set and retrieved from a registered model.

### Hypothesis

`hypothesis` is used to test against a large space of possible arguments. This is useful for catching unexpected edge cases, since we pick an input space for the test—rather than specific input values—and `hypothesis` will draw values from that space which often surpass the human imagination.

For example, we define strategies [here](https://github.com/VertaAI/modeldb/blob/b1c0106/client/verta/tests/test_utils/test_pip_requirements.py#L21-L35) for valid Python library names and version numbers, and use those strategies [here](https://github.com/VertaAI/modeldb/blob/b1c0106/client/verta/tests/test_utils/test_pip_requirements.py#L151-L166) to assert that our pip parsing utilities can handle any valid value.

Because `hypothesis` will run tests a large number of times, it should only be used for unit tests, and not for any tests that make backend calls.
