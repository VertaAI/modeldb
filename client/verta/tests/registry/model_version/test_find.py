# -*- coding: utf-8 -*-

import pytest

from verta.registry import stage_change


pytestmark = pytest.mark.not_oss  # skip if run in oss setup. Applied to entire module


class TestFind:
    def test_list_from_client(self, client, created_entities):
        """
        At some point, backend API was unexpectedly changed to require model ID
        in /model_versions/find, which broke client.registered_model_versions.

        """
        registered_model = client.create_registered_model()
        created_entities.append(registered_model)

        len(client.registered_model_versions)

    def test_find(self, client, created_entities):
        name = "registered_model_test"
        registered_model = client.set_registered_model()
        created_entities.append(registered_model)
        model_version = registered_model.get_or_create_version(name=name)

        find_result = registered_model.versions.find(["version == '{}'".format(name)])
        assert len(find_result) == 1
        for item in find_result:
            assert item._msg == model_version._msg

        tag_name = name + "_tag"
        versions = {
            name + "1": registered_model.get_or_create_version(name + "1"),
            name + "2": registered_model.get_or_create_version(name + "2"),
        }
        versions[name + "1"].add_label(tag_name)
        versions[name + "2"].add_label(tag_name)
        versions[name + "2"].add_label("label2")

        for version in versions:
            versions[version] = registered_model.get_version(version)

        find_result = registered_model.versions.find(
            ['labels == "{}"'.format(tag_name)]
        )
        assert len(find_result) == 2
        for item in find_result:
            assert versions[item._msg.version]
            msg_other = versions[item._msg.version]._msg
            item._msg.time_updated = msg_other.time_updated = 0
            labels1 = set(item._msg.labels)
            item._msg.labels[:] = []
            labels2 = set(msg_other.labels)
            msg_other.labels[:] = []
            msg_other.model.CopyFrom(item._msg.model)
            assert labels1 == labels2
            assert item._msg == msg_other

    def test_find_stage(self, registered_model):
        find = registered_model.versions.find

        assert len(find("stage == unassigned")) == 0

        model_ver = registered_model.create_version()
        assert len(find("stage == unassigned")) == 1

        model_ver.change_stage(
            stage_change.Development("Working on it."),
        )
        assert len(find("stage == unassigned")) == 0
        assert len(find("stage == development")) == 1

        model_ver.change_stage(
            stage_change.Staging("Undergoing final testing."),
        )
        assert len(find("stage == development")) == 0
        assert len(find("stage == staging")) == 1

        model_ver.change_stage(
            stage_change.Production("Rolling out to prod."),
        )
        assert len(find("stage == staging")) == 0
        assert len(find("stage == production")) == 1

        model_ver.change_stage(
            stage_change.Archived("Deprioritized; keeping for posterity."),
        )
        assert len(find("stage == production")) == 0
        assert len(find("stage == archived")) == 1
