import _ from 'lodash';
import React from 'react';
import ModelRecord from '../../../models/ModelRecord';

interface ILocalProps {
  expRuns: ModelRecord[];
}

interface ILocalState {
  aggType: string[];
  flatMetric: any;
  computeXAxisFields: any;
}

export default class ModelExploration extends React.Component<ILocalProps, ILocalState> {
  public xAxisParams: Set<string> = new Set(); // computed fields from ModelRecord object
  public yAxisParams: Set<string> = new Set(); // metric fields
  public constructor(props: ILocalProps) {
    super(props);

    this.state = {
      aggType: ['average', 'sum', 'median', 'variance', 'stdev', 'count'],
      computeXAxisFields: this.computeXAxisFields(props.expRuns),
      flatMetric: this.computeFlatMetric(props.expRuns)
    };
  }

  public render() {
    // const project = this.props.project;
    console.log(this.state);
    return <div>damal</div>;
  }

  // Utilities
  public computeFlatMetric = (arr: ModelRecord[]) => {
    return _.map(arr, obj => {
      const metricField = _.pick(obj, 'startTime', 'metrics');
      const flatMetric: any = { date: metricField.startTime };
      metricField.metrics.forEach((kvPair: any) => {
        this.yAxisParams.add(kvPair.key);
        flatMetric[kvPair.key] = kvPair.value;
      });
      return flatMetric;
    });
  };

  public computeXAxisFields = (expRuns: ModelRecord[]) => {
    this.xAxisParams.add('experimentId');
    this.xAxisParams.add('projectId');
    this.xAxisParams.add('id');
    this.xAxisParams.add('startTime');
    return _.map(expRuns, modeRecord => {
      const fields: any = {};
      if (modeRecord.metrics) {
        modeRecord.metrics.forEach((kvPair: any) => {
          this.xAxisParams.add(kvPair.key);
          fields[`metrics_${kvPair.key}`] = kvPair.value;
        });
      }
      if (modeRecord.hyperparameters) {
        modeRecord.hyperparameters.forEach((kvPair: any) => {
          this.xAxisParams.add(kvPair.key);
          fields[`hyperparameters_${kvPair.key}`] = kvPair.value;
        });
      }
      if (modeRecord.datasets) {
        modeRecord.datasets.forEach((kvPair: any) => {
          this.xAxisParams.add(kvPair.key);
          fields[`dataset_${kvPair.key}`] = kvPair.path;
        });
      }
      if (modeRecord.artifacts) {
        modeRecord.artifacts.forEach((kvPair: any) => {
          this.xAxisParams.add(kvPair.key);
          fields[`artifact_${kvPair.key}`] = kvPair.path;
        });
      }
      fields.experiment_id = modeRecord.experimentId;
      fields.project_id = modeRecord.projectId;
      fields.exp_run_id = modeRecord.id;
      fields.start_time = modeRecord.startTime;

      if (modeRecord.codeVersion) {
        fields.code_version = modeRecord.codeVersion;
        this.xAxisParams.add('codeVersion');
      }
      if (modeRecord.owner) {
        fields.owner = modeRecord.owner;
        this.xAxisParams.add('owner');
      }
      if (modeRecord.tags) {
        modeRecord.tags.forEach((tag: string) => {
          fields[`tag_${tag}`] = tag;
        });
      }
      return fields;
    });
  };
}
