import six

import pytest

import verta


OPERATORS = six.viewkeys(verta._tracking.ExperimentRuns._OP_MAP)


class TestFind:
    def test_reject_unsupported_keys(self, client, floats):
        # known unsupported keys
        all_keys = {
            attr
            for attr
            in verta.client._ExperimentRunService.ExperimentRun.__dict__.keys()
            if not attr[0].isupper()
            and not attr.startswith('_')
        }
        unsupported_keys = all_keys - verta._tracking.ExperimentRuns._VALID_QUERY_KEYS
        proj = client.set_project()
        expt = client.set_experiment()

        for _ in range(3):
            client.set_experiment_run()

        for expt_runs in (proj.expt_runs, expt.expt_runs):
            for key in unsupported_keys:
                for op, val in zip(OPERATORS, floats):
                    with pytest.raises(ValueError):
                        expt_runs.find("{} {} {}".format(key, op, val))

    def test_reject_random_keys(self, client, strs, floats):
        proj = client.set_project()
        expt = client.set_experiment()

        for _ in range(3):
            client.set_experiment_run()

        for expt_runs in (proj.expt_runs, expt.expt_runs):
            for key in strs:
                for op, val in zip(OPERATORS, floats):
                    with pytest.raises(ValueError):
                        expt_runs.find("{} {} {}".format(key, op, val))

    def test_id(self, client):
        proj = client.set_project()
        client.set_experiment()
        runs = [client.set_experiment_run() for _ in range(3)]

        for run_id in (run.id for run in runs):
            result = proj.expt_runs.find("id == '{}'".format(run_id))
            assert len(result) == 1
            assert result[0].id == run_id

    def test_project_id(self, client):
        proj = client.set_project()
        client.set_experiment()
        runs = [client.set_experiment_run() for _ in range(3)]
        client.set_experiment()
        runs.extend([client.set_experiment_run() for _ in range(3)])

        result = proj.expt_runs.find("project_id == '{}'".format(proj.id))
        assert set(run.id for run in result) == set(run.id for run in runs)

    def test_experiment_id(self, client):
        proj = client.set_project()
        client.set_experiment()
        [client.set_experiment_run() for _ in range(3)]
        expt = client.set_experiment()
        runs = [client.set_experiment_run() for _ in range(3)]

        result = proj.expt_runs.find("experiment_id == '{}'".format(expt.id))
        assert set(run.id for run in result) == set(run.id for run in runs)

    def test_name(self, client):
        proj = client.set_project()
        run = client.set_experiment_run()

        # no quotes around value!
        result = proj.expt_runs.find("name == {}".format(run.name))
        assert len(result) == 1
        assert result[0].id == run.id

    @pytest.mark.skip(reason="not implemented")
    def test_date_created(self, client):
        key = "date_created"

    @pytest.mark.skip(reason="not implemented")
    def test_date_updated(self, client):
        key = "date_updated"

    @pytest.mark.skip(reason="not implemented")
    def test_start_time(self, client):
        key = "start_time"

    @pytest.mark.skip(reason="not implemented")
    def test_end_time(self, client):
        key = "end_time"

    @pytest.mark.skip(reason="not implemented")
    def test_tags(self, client, strs):
        tags = strs[:5]
        proj = client.set_project()
        client.set_experiment()

        for i in range(1, len(tags)+1):
            client.set_experiment_run(tags=tags[:i])
        expt_runs = proj.expt_runs

        for tag in tags:
            # contains tag
            result = expt_runs.find("tags == '{}'".format(tag))
            runs = [run for run in expt_runs if tag in run.get_tags()]
            assert set(run.id for run in result) == set(run.id for run in runs)

            # does not contain tag
            result = expt_runs.find("tags != '{}'".format(tag))
            runs = [run for run in expt_runs if tag not in run.get_tags()]
            assert set(run.id for run in result) == set(run.id for run in runs)


    @pytest.mark.skip(reason="not implemented")
    def test_attributes(self, client):
        key = "attributes"

    def test_metrics_and_hyperparameters(self, client, strs, bools, floats):
        proj = client.set_project()
        client.set_experiment()

        metric_vals = [floats.pop() for _ in range(5)]
        hyperparam_vals = list(reversed([floats.pop() for _ in range(5)]))
        for metric_val, hyperparam_val in zip(metric_vals, hyperparam_vals):
            run = client.set_experiment_run()
            run.log_metric('val', metric_val)
            run.log_hyperparameter('val', hyperparam_val)
        expt_runs = proj.expt_runs

        threshold = int(metric_vals[len(metric_vals)//2])
        local_filtered_run_ids = set(run.id for run in expt_runs if run.get_metric('val') >= threshold)
        backend_filtered_run_ids = set(run.id for run in expt_runs.find("metrics.val >= {}".format(threshold)))
        assert local_filtered_run_ids == backend_filtered_run_ids

        threshold = int(hyperparam_vals[len(hyperparam_vals)//2])
        local_filtered_run_ids = set(run.id for run in expt_runs if run.get_hyperparameter('val') >= threshold)
        backend_filtered_run_ids = set(run.id for run in expt_runs.find("hyperparameters.val >= {}".format(threshold)))
        assert local_filtered_run_ids == backend_filtered_run_ids

    def test_negative_values(self, client):
        """There was a bug that rejected negative numbers as values."""
        proj = client.set_project()
        client.set_experiment()

        for val in range(-6, 0):
            client.set_experiment_run().log_metric('val', val)
        expt_runs = proj.expt_runs

        threshold = -3
        local_filtered_run_ids = set(run.id for run in expt_runs if run.get_metric('val') >= threshold)
        backend_filtered_run_ids = set(run.id for run in expt_runs.find("metrics.val >= {}".format(threshold)))
        assert local_filtered_run_ids == backend_filtered_run_ids


class TestSort:
    @pytest.mark.skip("back end sorts numbers lexicographically")
    def test_metrics_and_hyperparameters(self, client, floats):
        proj = client.set_project()
        client.set_experiment()

        metric_vals = [floats.pop() for _ in range(5)]
        hyperparam_vals = list(reversed([floats.pop() for _ in range(5)]))
        for metric_val, hyperparam_val in zip(metric_vals, hyperparam_vals):
            run = client.set_experiment_run()
            run.log_metric('val', metric_val)
            run.log_hyperparameter('val', hyperparam_val)
        expt_runs = proj.expt_runs

        # by metric
        sorted_run_ids = [
            run.id
            for run in sorted(expt_runs,
                              key=lambda run: run.get_metric('val'))
        ]
        for run_id, run in zip(sorted_run_ids, expt_runs.sort("metrics.val")):
            assert run_id == run.id

        # by hyperparameter, descending
        sorted_run_ids = [
            run.id
            for run in sorted(expt_runs,
                              key=lambda run: run.get_hyperparameter('val'),
                              reverse=True)
        ]
        for run_id, run in zip(sorted_run_ids, expt_runs.sort("hyperparameters.val", descending=True)):
            assert run_id == run.id


class TestChain:
    def test_chain(self, client):
        client.set_project()
        expt = client.set_experiment()

        for acc, loss in zip(range(6), reversed(range(6))):
            run = client.set_experiment_run()
            run.log_metric('acc', acc)
            run.log_metric('loss', loss)

        # chain *_k()
        runs = expt.expt_runs.bottom_k("metrics.acc", 4).top_k("metrics.acc", 2)
        assert [run.get_metric('acc') for run in runs] == [3, 2]

        # *_k() overrides prior sort()
        runs = expt.expt_runs.sort('metrics.loss').top_k("metrics.acc", 2)
        assert [run.get_metric('acc') for run in runs] == [5, 4]
        runs = expt.expt_runs.sort('metrics.loss', descending=True).top_k("metrics.acc", 2)
        assert [run.get_metric('acc') for run in runs] == [5, 4]

        # sort() overrides prior sort()
        runs = expt.expt_runs.sort('metrics.loss').sort("metrics.acc")
        assert [run.get_metric('acc') for run in runs] == [0, 1, 2, 3, 4, 5]
        runs = expt.expt_runs.sort('metrics.acc').sort("metrics.loss")
        assert [run.get_metric('loss') for run in runs] == [0, 1, 2, 3, 4, 5]


class TestTopK:
    @pytest.mark.skip("back end sorts numbers lexicographically")
    def test_metrics_and_hyperparameters(self, client, floats):
        k = 3
        proj = client.set_project()
        client.set_experiment()

        metric_vals = [floats.pop() for _ in range(5)]
        hyperparam_vals = list(reversed([floats.pop() for _ in range(5)]))
        for metric_val, hyperparam_val in zip(metric_vals, hyperparam_vals):
            run = client.set_experiment_run()
            run.log_metric('val', metric_val)
            run.log_hyperparameter('val', hyperparam_val)
        expt_runs = proj.expt_runs

        # by metric
        top_run_ids = [
            run.id
            for run in sorted(expt_runs,
                              key=lambda run: run.get_metric('val'),
                              reverse=True)
        ][:k]
        for run_id, run in zip(top_run_ids, expt_runs.top_k("metrics.val", k)):
            assert run_id == run.id

        # by hyperparameter
        top_run_ids = [
            run.id
            for run in sorted(expt_runs,
                              key=lambda run: run.get_hyperparameter('val'),
                              reverse=True)
        ][:k]
        for run_id, run in zip(top_run_ids, expt_runs.top_k("hyperparameters.val", k)):
            assert run_id == run.id


class TestBottomK:
    @pytest.mark.skip("back end sorts numbers lexicographically")
    def test_metrics_and_hyperparameters(self, client, floats):
        k = 3
        proj = client.set_project()
        client.set_experiment()

        metric_vals = [floats.pop() for _ in range(5)]
        hyperparam_vals = list(reversed([floats.pop() for _ in range(5)]))
        for metric_val, hyperparam_val in zip(metric_vals, hyperparam_vals):
            run = client.set_experiment_run()
            run.log_metric('val', metric_val)
            run.log_hyperparameter('val', hyperparam_val)
        expt_runs = proj.expt_runs

        # by metric
        bottom_run_ids = [
            run.id
            for run in sorted(expt_runs,
                              key=lambda run: run.get_metric('val'))
        ][:k]
        for run_id, run in zip(bottom_run_ids, expt_runs.bottom_k("metrics.val", k)):
            assert run_id == run.id

        # by hyperparameter
        bottom_run_ids = [
            run.id
            for run in sorted(expt_runs,
                              key=lambda run: run.get_hyperparameter('val'))
        ][:k]
        for run_id, run in zip(bottom_run_ids, expt_runs.bottom_k("hyperparameters.val", k)):
            assert run_id == run.id
