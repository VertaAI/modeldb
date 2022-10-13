# -*- coding: utf-8 -*-

import pytest

from verta._internal_utils import _histogram_utils


class TestHistogram:
    @staticmethod
    def assert_histograms_match_dataframe(histograms, df):
        """Common assertions for this test suite."""
        np = pytest.importorskip("numpy")

        # features match
        assert set(histograms["features"].keys()) == set(df.columns)
        # all rows counted
        assert histograms["total_count"] == len(df.index)

        for feature_name, histogram in histograms["features"].items():
            series = df[feature_name]
            histogram_type = histogram["type"]
            histogram_data = histogram["histogram"][histogram_type]

            # all data points counted
            counts = histogram_data["count"]
            assert sum(counts) == len(series)

            if histogram_type == "binary":
                num_false = sum(~series)
                num_true = sum(series)

                assert counts == [num_false, num_true]
            elif histogram_type == "discrete":
                buckets = histogram_data["bucket_values"]

                # buckets in ascending order
                assert buckets == list(sorted(buckets))

                # data within buckets
                assert all(buckets[0] <= series)
                assert all(series <= buckets[-1])

                # appropriate leftmost and rightmost buckets
                assert buckets[0] == series.min()
                assert buckets[-1] == series.max()

                # all buckets have data
                # NOTE: this might not be behavior that we want in the future
                assert all(counts)

                # counts correct
                for value, count in zip(buckets, counts):
                    assert sum(series == value) == count
            elif histogram_type == "float":
                limits = histogram_data["bucket_limits"]

                # limits in ascending order
                assert limits == list(sorted(limits))

                # data within limits
                assert all(limits[0] <= series)
                assert all(series <= limits[-1])

                # appropriate leftmost and rightmost limits
                assert np.isclose(limits[0], series.min())
                assert np.isclose(limits[-1], series.max())

                # buckets equal in width
                bucket_widths = np.diff(limits)
                assert np.allclose(bucket_widths, bucket_widths[0])

                # correct number of buckets
                assert len(limits) == 11

                # counts correct
                bin_windows = list(zip(limits[:-1], limits[1:]))
                for i, (l, r) in enumerate(bin_windows[:-1]):
                    assert sum((l <= series) & (series < r)) == counts[i]
                assert sum(limits[-2] <= series) == counts[-1]

    def test_binary(self):
        np = pytest.importorskip("numpy")
        pd = pytest.importorskip("pandas")
        num_rows = 90

        df = pd.concat(
            objs=[
                pd.Series(
                    np.random.random(size=num_rows).round().astype(bool), name="A"
                ),
                pd.Series(
                    np.random.random(size=num_rows).round().astype(bool), name="B"
                ),
                pd.Series(
                    np.random.random(size=num_rows).round().astype(bool), name="C"
                ),
            ],
            axis="columns",
        )
        histograms = _histogram_utils.calculate_histograms(df)

        assert all(
            histogram["type"] == "binary"
            for histogram in histograms["features"].values()
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
            axis="columns",
        )
        histograms = _histogram_utils.calculate_histograms(df)

        assert all(
            histogram["type"] == "discrete"
            for histogram in histograms["features"].values()
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
            axis="columns",
        )
        histograms = _histogram_utils.calculate_histograms(df)

        assert all(
            histogram["type"] == "float"
            for histogram in histograms["features"].values()
        )
        self.assert_histograms_match_dataframe(histograms, df)
