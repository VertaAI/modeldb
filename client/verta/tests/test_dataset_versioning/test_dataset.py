import verta
import pytest


class TestDataset:
    def test_create(self, client, created_datasets):
        dataset = client.set_dataset2()
        assert dataset
        created_datasets.append(dataset)

        name = verta._internal_utils._utils.generate_default_name()
        dataset = client.create_dataset2(name)
        assert dataset
        created_datasets.append(dataset)

        # TODO: Dataset can have duplicate names. Uncomment these checks when this issue is fixed:
        # with pytest.raises(requests.HTTPError) as excinfo:
        #     assert client.create_dataset2(name)
        # excinfo_value = str(excinfo.value).strip()
        # assert "409" in excinfo_value
        # assert "already exists" in excinfo_value

    def test_get(self, client, created_datasets):
        name = verta._internal_utils._utils.generate_default_name()

        with pytest.raises(ValueError):
            client.get_dataset2(name)

        dataset = client.set_dataset2(name)
        created_datasets.append(dataset)

        assert dataset.id == client.get_dataset2(dataset.name).id
        assert dataset.id == client.get_dataset2(id=dataset.id).id

    def test_tags(self, client, created_datasets):
        name = verta._internal_utils._utils.generate_default_name()
        dataset = client.set_dataset2(name, tags=["tag1", "tag2"])
        created_datasets.append(dataset)
        assert dataset.get_tags() == ["tag1", "tag2"]

        dataset.add_tag("tag3")
        assert dataset.get_tags() == ["tag1", "tag2", "tag3"]

        dataset.add_tags(["tag1", "tag4", "tag5"])
        assert dataset.get_tags() == ["tag1", "tag2", "tag3", "tag4", "tag5"]

        dataset.del_tag("tag2")
        assert dataset.get_tags() == ["tag1", "tag3", "tag4", "tag5"]

        dataset.del_tag("tag100")