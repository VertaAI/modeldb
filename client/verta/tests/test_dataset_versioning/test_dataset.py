import verta
import pytest


class TestDataset:
    def test_description(self, client):
        original_description = "this is a cool dataset"
        dataset = client.set_dataset2(desc=original_description)
        assert dataset.get_description() == original_description

        updated_description = "this is an uncool dataset"
        dataset.set_description(updated_description)
        assert dataset.get_description() == updated_description
