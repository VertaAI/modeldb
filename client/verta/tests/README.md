## Installation
To install the packages needed for running tests, refer to the **Developer Installation** instructions in [the contribution guide](../../CONTRIBUTING.md).

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

Add the `-vvs` flag to output `stdout` while tests are running:
```bash
pytest -vvs
```

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

## Writing Tests
Tests are loosely organized by files and classes of related functionality. See [`test_versioning/test_repository.py`](test_versioning/test_repository.py) or [`test_entities.py`](test_entities.py) for decent examples.

Note that for CLI tests, `click` provides [its own testing utilities](https://click.palletsprojects.com/en/7.x/testing/). See [`test_cli.py`](test_cli.py) for examples.

### Fixtures 
[`pytest` fixtures](https://docs.pytest.org/en/stable/fixture.html) are reusable items that are passed to test functions.
Most fixtures are defined in [`conftest.py`](conftest.py).  
To use a fixture: simply write the name of the fixture as a parameter to your test function; `pytest` will automatically pass it in at runtime.  
To write a fixture: write code for setup, `yield` the object that should be passed to the test function, then write code for cleanup.
