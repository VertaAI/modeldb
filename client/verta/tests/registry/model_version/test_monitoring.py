# -*- coding: utf-8 -*-

import pytest


class TestLogReferenceData:
    # TODO: tests for non-happy paths
    def test_log_reference_data(self, model_version):
        """log_reference_data() logs a CSV with expected columns."""
        np = pytest.importorskip("numpy")
        pd = pytest.importorskip("pandas")

        data = np.random.random((36, 12))
        X = pd.DataFrame(np.random.random((36, 8)))
        Y = pd.DataFrame(np.random.random((36, 2)))

        model_version.log_reference_data(X, Y)
        df = pd.read_csv(model_version.get_artifact("reference_data"))

        assert all(df["source"] == "reference")
        assert all(df["model_version_id"] == model_version.id)
        for c in X.columns:
            assert np.allclose(df["input." + str(c)], X[c])
        for c in Y.columns:
            assert np.allclose(df["output." + str(c)], Y[c])
