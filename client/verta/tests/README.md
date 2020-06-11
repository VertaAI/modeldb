To install the packages needed for developing `verta`, run:

```bash
cd ..
python3 -m venv tests-venv # or use virtualenv in Python 2
source tests-venv/bin/activate
python -m pip install -r requirements.txt
cd tests
```

Note that on macOS, importing `matplotlib` can raise an error depending on how Python was installed.
Refer to [this StackOverflow post](https://stackoverflow.com/a/21789908/) for a solution.

---

Set the following environment variables:

- `VERTA_HOST` e.g. `http://localhost:3000`
- `VERTA_EMAIL`
- `VERTA_DEV_KEY`
- `VERTA_S3_TEST_BUCKET` (specified bucket must exist, the tests will not create it)
- `VERTA_S3_TEST_OBJECT` (specified object must exist, the tests will not create it)

---

To execute tests, run:

```
pytest
```

You can also run specific test scripts like so:

```
pytest test_entities.py
```

Pytest by default captures print statement outputs and only displays them when errors are encountered, but outputs can be unsuppressed:

```
pytest -s
```
