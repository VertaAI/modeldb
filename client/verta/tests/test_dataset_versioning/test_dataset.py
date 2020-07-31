import verta
import requests
import pytest

from verta._dataset_versioning.dataset import Dataset


class TestDataset:
    def test_create(self, client):
        dataset = client.set_dataset2()
        assert dataset

        name = verta._internal_utils._utils.generate_default_name()
        dataset = client.create_dataset2(name)
        assert dataset

        # TODO: Dataset can have duplicate names. Uncomment these checks when this issue is fixed:
        # with pytest.raises(requests.HTTPError) as excinfo:
        #     assert client.create_dataset2(name)
        # excinfo_value = str(excinfo.value).strip()
        # assert "409" in excinfo_value
        # assert "already exists" in excinfo_value

    def test_get(self, client):
        name = verta._internal_utils._utils.generate_default_name()

        with pytest.raises(ValueError):
            client.get_dataset2(name)

        dataset = client.set_dataset2(name)

        assert dataset.id == client.get_dataset2(dataset.name).id
        assert dataset.id == client.get_dataset2(id=dataset.id).id
