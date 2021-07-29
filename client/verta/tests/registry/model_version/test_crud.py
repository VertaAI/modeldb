# -*- coding: utf-8 -*-

import pytest
import requests

import verta
import verta.dataset
from verta import visibility
from verta import data_types
from verta.registry import lock
from verta._internal_utils import importer

pytestmark = pytest.mark.not_oss  # skip if run in oss setup. Applied to entire module


class TestCRUD:
    def test_create(self, registered_model):
        name = verta._internal_utils._utils.generate_default_name()
        assert registered_model.create_version(name)
        with pytest.raises(requests.HTTPError) as excinfo:
            assert registered_model.create_version(name)
        excinfo_value = str(excinfo.value).strip()
        assert "409" in excinfo_value
        assert "already exists" in excinfo_value

    def test_set(self, registered_model):
        name = verta._internal_utils._utils.generate_default_name()
        version = registered_model.set_version(name=name)

        assert registered_model.set_version(name=version.name).id == version.id

    def test_get_by_name(self, registered_model):
        model_version = registered_model.get_or_create_version(name="my version")
        retrieved_model_version = registered_model.get_version(name=model_version.name)
        assert retrieved_model_version.id == model_version.id

    def test_get_by_id(self, registered_model):
        model_version = registered_model.get_or_create_version()
        retrieved_model_version = registered_model.get_version(id=model_version.id)
        assert model_version.id == retrieved_model_version.id
        with pytest.warns(UserWarning, match=".*already exists.*"):
            registered_model.get_or_create_version(
                id=model_version.id, desc="new description"
            )

    def test_repr(self, model_version):
        model_version.add_labels(["tag1", "tag2"])
        repr = str(model_version)

        assert model_version.name in repr
        assert str(model_version.id) in repr
        assert str(model_version.registered_model_id) in repr
        assert str(model_version.get_labels()) in repr

        np = pytest.importorskip("numpy")
        sklearn = pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression

        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())

        model_version.log_model(classifier)
        model_version.log_artifact("coef", classifier.coef_)
        repr = str(model_version)
        assert "model" in repr
        assert "coef" in repr

    def test_get_by_client(self, client, created_entities):
        registered_model = client.set_registered_model()
        created_entities.append(registered_model)
        model_version = registered_model.get_or_create_version(name="my version")

        retrieved_model_version_by_id = client.get_registered_model_version(
            model_version.id
        )

        assert retrieved_model_version_by_id.id == model_version.id

    def test_labels(self, client, created_entities):
        registered_model = client.set_registered_model()
        created_entities.append(registered_model)
        model_version = registered_model.get_or_create_version(name="my version")

        model_version.add_label("tag1")
        model_version.add_label("tag2")
        model_version.add_label("tag3")
        assert model_version.get_labels() == ["tag1", "tag2", "tag3"]
        model_version.del_label("tag2")
        assert model_version.get_labels() == ["tag1", "tag3"]
        model_version.del_label("tag4")
        assert model_version.get_labels() == ["tag1", "tag3"]
        model_version.add_label("tag2")
        assert model_version.get_labels() == ["tag1", "tag2", "tag3"]

        model_version.add_labels(["tag2", "tag4", "tag1", "tag5"])
        assert model_version.get_labels() == ["tag1", "tag2", "tag3", "tag4", "tag5"]

    def test_description(self, client, registered_model):
        desc = "description"
        model_version = registered_model.get_or_create_version(name="my version")
        model_version.set_description(desc)
        assert desc == model_version.get_description()

    @pytest.mark.skip(reason="functionality postponed in Client")
    def test_archive(self, model_version):
        assert not model_version.is_archived

        model_version.archive()
        assert model_version.is_archived

        with pytest.raises(RuntimeError) as excinfo:
            model_version.archive()

        assert "the version has already been archived" in str(excinfo.value)

    def test_clear_cache(self, registered_model):
        # Multiple log_artifacts calls, which would potentially fail without clear_cache
        model_version = registered_model.get_or_create_version(name="my version")
        model_version_2 = registered_model.get_version(
            id=model_version.id
        )  # same version object

        np = pytest.importorskip("numpy")
        artifact = np.random.random((36, 12))

        for i in range(2):
            model_version.log_artifact("artifact_{}".format(2 * i), artifact)
            model_version_2.log_artifact("artifact_{}".format(2 * i + 1), artifact)

        model_version = registered_model.get_version(
            id=model_version.id
        )  # re-retrieve the version
        assert len(model_version._msg.artifacts) == 4

    def test_attributes(self, client, registered_model):
        model_version = registered_model.get_or_create_version(name="my version")

        model_version.add_attribute("float-attr", 0.4)
        model_version.add_attribute("float-attr", 0.8)
        assert model_version.get_attribute("float-attr") == 0.4

        model_version.add_attribute("float-attr", 0.8, overwrite=True)
        assert model_version.get_attribute("float-attr") == 0.8
        # Test overwriting
        model_version.add_attribute("int-attr", 15)
        assert model_version.get_attribute("int-attr") == 15

        model_version.add_attributes(
            {"int-attr": 16, "float-attr": 123.0}, overwrite=True
        )
        assert model_version.get_attributes() == {"int-attr": 16, "float-attr": 123.0}
        # Test deleting:
        model_version.del_attribute("int-attr")
        assert model_version.get_attributes() == {"float-attr": 123.0}

        # Deleting non-existing key:
        model_version.del_attribute("non-existing")

    def test_attributes_overwrite(self, client, registered_model):
        old_attr = ("a", 1)
        new_attr = ("a", 2)
        second_attr = ("b", 2)

        model_version = registered_model.create_version()
        model_version.add_attribute(*old_attr)

        # without `overwrite`
        with pytest.warns(
            UserWarning,
            match="^skipping.*{}.*already exists".format(old_attr[0]),
        ):
            model_version.add_attributes(dict([new_attr, second_attr]))
        assert model_version.get_attributes() == dict([old_attr, second_attr])

        # with `overwrite`
        with pytest.warns(None) as record:
            model_version.add_attributes(dict([new_attr, second_attr]), overwrite=True)
        assert not record  # no warning
        assert model_version.get_attributes() == dict([new_attr, second_attr])

    def test_patch(self, registered_model):
        NAME = "name"
        DESCRIPTION = "description"
        LABELS = ["label"]
        ATTRIBUTES = {"attribute": 3}

        version = registered_model.create_version(NAME)

        version.set_description(DESCRIPTION)
        assert version.name == NAME

        version.add_labels(LABELS)
        assert version.get_description() == DESCRIPTION

        version.add_attributes(ATTRIBUTES)
        assert version.get_labels() == LABELS

        assert version.get_attributes() == ATTRIBUTES


class TestLockLevels:
    @pytest.mark.parametrize("lock_level", (lock.Open(), lock.Redact(), lock.Closed()))
    def test_creation(self, registered_model, lock_level):
        model_ver = registered_model.create_version(lock_level=lock_level)
        assert model_ver._msg.lock_level == lock_level._as_proto()
        assert isinstance(model_ver.get_lock_level(), lock_level.__class__)

    @pytest.mark.parametrize("lock_level", (lock.Open(), lock.Redact(), lock.Closed()))
    def test_creation_from_run(self, registered_model, experiment_run, lock_level):
        model_ver = registered_model.create_version_from_run(
            experiment_run.id, lock_level=lock_level
        )
        assert model_ver._msg.lock_level == lock_level._as_proto()
        assert isinstance(model_ver.get_lock_level(), lock_level.__class__)

    def test_transition_levels(self, client, client_2, organization, created_entities):
        organization.add_member(client_2._conn.email)
        reg_model = client.create_registered_model(
            workspace=organization.name,
            visibility=visibility.OrgCustom(write=True),
        )
        created_entities.append(reg_model)
        # org owner
        admin_model_ver = reg_model.create_version()
        # same model version as R/W user
        user_model_ver = client_2.get_registered_model_version(admin_model_ver.id)

        # R/W user can upgrade lock level
        user_model_ver.set_lock_level(lock.redact)
        user_model_ver.set_lock_level(lock.closed)

        # R/W user cannot downgrade; admin can
        with pytest.raises(requests.HTTPError, match="^403"):
            user_model_ver.set_lock_level(lock.redact)
        admin_model_ver.set_lock_level(lock.redact)
        with pytest.raises(requests.HTTPError, match="^403"):
            user_model_ver.set_lock_level(lock.open)
        admin_model_ver.set_lock_level(lock.open)

        # admin can upgrade lock level
        admin_model_ver.set_lock_level(lock.redact)
        admin_model_ver.set_lock_level(lock.closed)

    def test_closed(self, client, client_2, organization, created_entities):
        description = "My model version"
        label = "mine"

        organization.add_member(client_2._conn.email)
        reg_model = client.create_registered_model(
            workspace=organization.name,
            visibility=visibility.OrgCustom(write=True),
        )
        created_entities.append(reg_model)

        # org owner
        admin_model_ver = reg_model.create_version(lock_level=lock.closed)
        # same model version as R/W user
        user_model_ver = client_2.get_registered_model_version(admin_model_ver.id)

        for model_ver in [admin_model_ver, user_model_ver]:
            model_ver.set_description(description)
            assert model_ver.get_description() == description
            model_ver.add_label(label)
            assert model_ver.get_labels() == [label]
            model_ver.del_label(label)
            with pytest.raises(requests.HTTPError, match="locked for changes"):
                model_ver.add_attribute("a", {"a": 1})
            with pytest.raises(requests.HTTPError, match="locked for changes"):
                model_ver.delete()

    def test_redact(self, client, client_2, organization, created_entities):
        description = "My model version"
        label = "mine"

        organization.add_member(client_2._conn.email)
        reg_model = client.create_registered_model(
            workspace=organization.name,
            visibility=visibility.OrgCustom(write=True),
        )
        created_entities.append(reg_model)

        # org owner
        admin_model_ver = reg_model.create_version(lock_level=lock.redact)
        # same model version as R/W user
        user_model_ver = client_2.get_registered_model_version(admin_model_ver.id)

        for model_ver in [admin_model_ver, user_model_ver]:
            model_ver.set_description(description)
            assert model_ver.get_description() == description
            model_ver.add_label(label)
            assert model_ver.get_labels() == [label]
            model_ver.del_label(label)

        admin_model_ver.add_attribute("a", {"a": 1})
        with pytest.raises(requests.HTTPError, match="^403"):
            user_model_ver.del_attribute("a")

        admin_model_ver.log_artifact("b", {"b": 2})
        with pytest.raises(requests.HTTPError, match="^403"):
            user_model_ver.del_artifact("b")

        with pytest.raises(requests.HTTPError, match="^403"):
            user_model_ver.delete()
        admin_model_ver.delete()


# TODO: combine with test_experimentrun/test_attributes.py::TestComplexAttributes
@pytest.mark.skipif(
    importer.maybe_dependency("scipy") is None,
    reason="scipy is not installed",
)
class TestComplexAttributes:
    def test_creation(self, client, strs):
        key = strs[0]
        attr = data_types.DiscreteHistogram(
            buckets=["yes", "no"],
            data=[10, 20],
        )

        registered_model = client.create_registered_model()
        model_version = registered_model.create_version(
            attrs={key: attr},
        )
        assert model_version.get_attribute(key) == attr

    def test_single(self, model_version, strs):
        key = strs[0]
        attr = data_types.Line(
            x=[1, 2, 3],
            y=[1, 4, 9],
        )

        model_version.add_attribute(key, attr)
        assert model_version.get_attribute(key) == attr

    def test_batch(self, model_version, strs):
        key1, key2, key3 = strs[:3]
        attr1 = data_types.Table(
            data=[[1, "two", 3], [4, "five", 6]],
            columns=["header1", "header2", "header3"],
        )
        attr2 = data_types.ConfusionMatrix(
            value=[[1, 2, 3], [4, 5, 6], [7, 8, 9]],
            labels=["a", "b", "c"],
        )
        attr3 = {"a": 1}

        model_version.add_attribute(key1, attr1)
        assert model_version.get_attribute(key1) == attr1
        model_version.add_attribute(key2, attr2)
        assert model_version.get_attribute(key2) == attr2
        model_version.add_attribute(key3, attr3)

        assert model_version.get_attributes() == {
            key1: attr1,
            key2: attr2,
            key3: attr3,
        }
