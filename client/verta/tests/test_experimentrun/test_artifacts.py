# -*- coding: utf-8 -*-

import os

import pytest
import six

from verta._internal_utils import _artifact_utils


class TestImages:
    @staticmethod
    def matplotlib_to_pil(fig):
        Image = pytest.importorskip("PIL.Image")

        bytestream = six.BytesIO()
        fig.savefig(bytestream)
        return Image.open(bytestream)

    def test_upload_blank_warning(self, experiment_run, strs):
        Image = pytest.importorskip("PIL.Image")

        key = strs[0]
        img = Image.new("RGB", (64, 64), "white")

        with pytest.warns(UserWarning):
            experiment_run.log_image(key, img)

    def test_upload_plt(self, experiment_run, strs):
        np = pytest.importorskip("numpy")
        matplotlib = pytest.importorskip("matplotlib")
        matplotlib.use("Agg")  # https://stackoverflow.com/a/37605654
        import matplotlib.pyplot as plt

        key = strs[0]
        plt.scatter(*np.random.random((2, 10)))

        experiment_run.log_image(key, plt)
        assert np.array_equal(
            np.asarray(experiment_run.get_image(key).getdata()),
            np.asarray(self.matplotlib_to_pil(plt).getdata()),
        )

    def test_upload_fig(self, experiment_run, strs):
        np = pytest.importorskip("numpy")
        matplotlib = pytest.importorskip("matplotlib")
        matplotlib.use("Agg")  # https://stackoverflow.com/a/37605654
        import matplotlib.pyplot as plt

        key = strs[0]
        fig, ax = plt.subplots()
        ax.scatter(*np.random.random((2, 10)))

        experiment_run.log_image(key, fig)
        assert np.array_equal(
            np.asarray(experiment_run.get_image(key).getdata()),
            np.asarray(self.matplotlib_to_pil(fig).getdata()),
        )

    def test_upload_pil(self, experiment_run, strs):
        np = pytest.importorskip("numpy")
        Image = pytest.importorskip("PIL.Image")
        ImageDraw = pytest.importorskip("PIL.ImageDraw")

        key = strs[0]
        img = Image.new("RGB", (64, 64), "gray")
        ImageDraw.Draw(img).arc(
            np.r_[
                np.random.randint(32, size=(2)), np.random.randint(32, 64, size=(2))
            ].tolist(),
            np.random.randint(360),
            np.random.randint(360),
            "white",
        )

        experiment_run.log_image(key, img)
        assert np.array_equal(
            np.asarray(experiment_run.get_image(key).getdata()),
            np.asarray(img.getdata()),
        )

    def test_conflict(self, experiment_run, strs):
        Image = pytest.importorskip("PIL.Image")

        images = dict(zip(strs, [Image.new("RGB", (64, 64), "gray")] * 3))

        for key, image in six.viewitems(images):
            experiment_run.log_image(key, image)
            with pytest.raises(ValueError):
                experiment_run.log_image(key, image)

        for key, image in reversed(list(six.viewitems(images))):
            with pytest.raises(ValueError):
                experiment_run.log_image(key, image)

    def test_blocklisted_key_error(self, experiment_run, all_values):
        all_values = (
            value  # log_artifact treats str value as filepath to open
            for value in all_values
            if not isinstance(value, str)
        )

        for key, artifact in zip(_artifact_utils.BLOCKLISTED_KEYS, all_values):
            with pytest.raises(ValueError, match="please use a different key$"):
                experiment_run.log_image(key, artifact)
