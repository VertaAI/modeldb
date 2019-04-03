import { bind } from 'decko';
import _ from 'lodash';
import React from 'react';
import {
  listAverage,
  listCount,
  listMedian,
  listStdev,
  listSum,
  listVariance,
} from 'utils/StatMethods/AggregationTypes';
import ModelRecord from '../../../models/ModelRecord';
import BarChart from './BarChart';

import styles from './ModelExploration.module.css';

interface ILocalProps {
  expRuns: ModelRecord[];
  initialSelection: any;
}

interface ILocalState {
  aggType: string[];
  flatMetric: object[];
  computeXAxisFields: object[];
  selectedXAxis: string;
  selectedYAxis: string;
  selectedAggregate: string;
}

export default class ModelExploration extends React.Component<
  ILocalProps,
  ILocalState
> {
  public xAxisParams: Set<string> = new Set(); // computed fields from ModelRecord object
  public yAxisParams: Set<string> = new Set(); // metric fields only for Y axis
  public summaryParams: Set<string> = new Set();
  public hyperParams: Set<string> = new Set();
  public mapOptGroup = { metric: false, hyper: false };
  public constructor(props: ILocalProps) {
    super(props);
    this.state = {
      aggType: ['average', 'sum', 'median', 'variance', 'stdev', 'count'],
      computeXAxisFields: this.computeXAxisFields(props.expRuns),
      flatMetric: this.computeFlatMetric(props.expRuns),
      selectedAggregate: 'average',
      selectedXAxis: 'hidden_size', // initial val for testing //
      selectedYAxis: 'test_loss', // initial val for testing - first metric
    };
  }

  public render() {
    const { expRuns } = this.props;
    return expRuns ? (
      <div className={styles.summary_wrapper}>
        <h3>Explore Visualizations</h3>
        <p>
          Generate charts to visualize trends in data by selecting fields to
          plot as x and y values. Optionally pick fields to group by and specify
          what type of aggregation to use.
        </p>

        <div style={{ display: 'flex' }}>
          <div className={styles.chart_selector}>
            X Axis: {'  '}
            <select
              name="selected-xaxis"
              value={this.state.selectedXAxis}
              onChange={this.setLocalXState}
              className={styles.dropdown}
            >
              {Array.from(this.summaryParams).map(
                (param: string, i: number) => {
                  return (
                    <option key={i} value={param}>
                      {param}
                    </option>
                  );
                }
              )}
              <optgroup key={'hyper-param'} label={'Hyperparameters'}>
                {Array.from(this.hyperParams).map(
                  (param: string, i: number) => {
                    return (
                      <option key={i} value={param}>
                        {param}
                      </option>
                    );
                  }
                )}
              </optgroup>
            </select>
          </div>

          <div className={styles.chart_selector}>
            Y Axis: {'  '}
            <select
              name="selected-yaxis"
              value={this.state.selectedYAxis}
              onChange={this.setLocalYState}
              className={styles.dropdown}
            >
              <optgroup key={'metric'} label={'Metrics'}>
                {Array.from(this.yAxisParams).map(
                  (param: string, i: number) => {
                    return (
                      <option key={i} value={param}>
                        {param}
                      </option>
                    );
                  }
                )}
              </optgroup>
            </select>
          </div>

          <div className={styles.chart_selector}>
            Aggregate: {'  '}
            <select
              name="selected-aggregate"
              value={this.state.selectedAggregate}
              onChange={this.setLocalAggState}
              className={styles.dropdown}
            >
              {this.state.aggType.map((param: string, i: number) => {
                return (
                  <option key={i} value={param}>
                    {param}
                  </option>
                );
              })}
            </select>
          </div>
          {/* <div className={styles.compute_button}>
            <button>Compute Charts</button>
          </div> */}
        </div>
        <div>
          <BarChart
            xLabel={this.state.selectedXAxis}
            yLabel={this.state.selectedYAxis}
            // data={this.reduceMetricForAgg(
            //   this.groupBy(this.state.computeXAxisFields, (field: any) => field[this.state.selectedXAxis]),
            //   this.state.selectedAggregate
            // )}
            data={this.returnAggResults(
              this.state.selectedAggregate,
              this.groupBy(
                this.state.computeXAxisFields,
                (field: any) => field[this.state.selectedXAxis]
              )
            )}
          />
        </div>
      </div>
    ) : (
      ''
    );
  }
  // event handler to set user selection fields for bar chart

  @bind
  public setLocalYState(event: React.FormEvent<HTMLSelectElement>): void {
    const element = event.target as HTMLSelectElement;
    this.setState({ selectedYAxis: element.value });
  }

  @bind
  public setLocalXState(event: React.FormEvent<HTMLSelectElement>): void {
    const element = event.target as HTMLSelectElement;
    this.setState({ selectedXAxis: element.value });
  }

  @bind
  public setLocalAggState(event: React.FormEvent<HTMLSelectElement>): void {
    const element = event.target as HTMLSelectElement;
    this.setState({ selectedAggregate: element.value });
  }

  // Utility Functions
  @bind
  public groupBy(list: object[], keyGetter: any) {
    const map = new Map();
    list.forEach((item: any) => {
      const key = keyGetter(item);
      const collection = map.get(key);
      if (!collection) {
        map.set(key, [item[this.state.selectedYAxis]]);
      } else {
        collection.push(item[this.state.selectedYAxis]);
      }
    });
    return map;
  }

  @bind
  public computeFlatMetric(arr: ModelRecord[]) {
    return arr.map(obj => {
      const metricField = _.pick(obj, 'startTime', 'metrics');
      const flatMetric: any = { date: metricField.startTime };
      metricField.metrics.forEach((kvPair: any) => {
        this.yAxisParams.add(kvPair.key);
        flatMetric[kvPair.key] = kvPair.value;
      });
      return flatMetric;
    });
  }

  // reduce data based on aggigation type
  // public reduceMetricForAgg = (groupByResult: any, selectedAggType: string) => {
  //   let aggFun: any;
  //   switch (selectedAggType) {
  //     case 'sum':
  //       aggFun = this.sum;
  //     case 'median':
  //       aggFun = this.median;
  //     case 'variance':
  //       aggFun = this.variance;
  //     case 'stdev':
  //       aggFun = this.stdev;
  //     case 'count':
  //       aggFun = this.count;
  //     default:
  //       aggFun = this.average;
  //   }
  //   return [...groupByResult].map(obj => {
  //     return { key: obj[0], value: aggFun(obj[1]) };
  //   });
  // };

  @bind
  public returnAggResults(selected: string, arrayGpBy: any) {
    switch (selected) {
      case 'average':
        return this.averageReduceMetrics(arrayGpBy);
      case 'sum':
        return this.sumReduceMetrics(arrayGpBy);
      case 'median':
        return this.medianReduceMetrics(arrayGpBy);
      case 'variance':
        return this.varianceReduceMetrics(arrayGpBy);
      case 'stdev':
        return this.stdevReduceMetrics(arrayGpBy);
      case 'count':
        return this.countReduceMetrics(arrayGpBy);
    }
  }

  @bind
  public averageReduceMetrics(mapObj: any) {
    return [...mapObj].map(obj => {
      return { key: obj[0], value: listAverage(obj[1]) };
    });
  }

  @bind
  public sumReduceMetrics(mapObj: any) {
    return [...mapObj].map(obj => {
      return { key: obj[0], value: listSum(obj[1]) };
    });
  }

  @bind
  public medianReduceMetrics(mapObj: any) {
    return [...mapObj].map(obj => {
      return { key: obj[0], value: listMedian(obj[1]) };
    });
  }

  @bind
  public varianceReduceMetrics(mapObj: any) {
    return [...mapObj].map(obj => {
      return { key: obj[0], value: listVariance(obj[1]) };
    });
  }

  @bind
  public stdevReduceMetrics(mapObj: any) {
    return [...mapObj].map(obj => {
      return { key: obj[0], value: listStdev(obj[1]) };
    });
  }

  @bind
  public countReduceMetrics(mapObj: any) {
    return [...mapObj].map(obj => {
      return { key: obj[0], value: listCount(obj[1]) };
    });
  }

  // compute flat data and set unique xAxisParams to render dropdown
  @bind
  public computeXAxisFields(expRuns: ModelRecord[]) {
    // hard coded with an assumption that these ids will always be present with the data
    this.xAxisParams.add('experiment_id');
    this.summaryParams.add('experiment_id');
    this.xAxisParams.add('project_id');
    this.summaryParams.add('project_id');
    // this.xAxisParams.add('id');
    // this.xAxisParams.add('start_time');
    return expRuns.map(modeRecord => {
      const fields: any = {};
      if (modeRecord.hyperparameters) {
        modeRecord.hyperparameters.forEach((kvPair: any) => {
          this.xAxisParams.add(`${kvPair.key}`);
          this.hyperParams.add(kvPair.key);
          fields[`${kvPair.key}`] = kvPair.value;
        });
      }

      if (modeRecord.metrics) {
        modeRecord.metrics.forEach((kvPair: any) => {
          // this.xAxisParams.add(`${kvPair.key}`);
          fields[`${kvPair.key}`] = kvPair.value;
        });
      }
      // if (modeRecord.datasets) {
      //   modeRecord.datasets.forEach((kvPair: any) => {
      //     this.xAxisParams.add(kvPair.key);
      //     fields[`dataset_${kvPair.key}`] = kvPair.path;
      //   });
      // }
      // if (modeRecord.artifacts) {
      //   modeRecord.artifacts.forEach((kvPair: any) => {
      //     this.xAxisParams.add(kvPair.key);
      //     fields[`artifact_${kvPair.key}`] = kvPair.path;
      //   });
      // }
      fields.experiment_id = modeRecord.experimentId;
      fields.project_id = modeRecord.projectId;
      fields.exp_run_id = modeRecord.id;
      fields.start_time = modeRecord.startTime;

      if (modeRecord.codeVersion) {
        fields.code_version = modeRecord.codeVersion;
        this.xAxisParams.add('code_version');
      }
      if (modeRecord.owner) {
        fields.owner = modeRecord.owner;
        this.xAxisParams.add('owner');
        this.summaryParams.add('owner');
      }
      if (modeRecord.tags) {
        modeRecord.tags.forEach((tag: string) => {
          fields[`tag_${tag}`] = tag;
        });
      }
      return fields;
    });
  }
}
