import verta
import pytest


class TestDataset:
    def test_repr(self, client, created_datasets):
        description = "this is a cool dataset"
        tags = [u"tag1", u"tag2"]
        dataset = client.set_dataset2(desc=description, tags=tags)
        created_datasets.append(dataset)

        str_repr = repr(dataset)
        
        assert "name: {}".format(dataset.name) in str_repr
        assert "id: {}".format(dataset.id) in str_repr
        assert "time created" in str_repr
        assert "time updated" in str_repr
        assert "description: {}".format(description) in str_repr
        assert "tags: {}".format(tags) in str_repr
