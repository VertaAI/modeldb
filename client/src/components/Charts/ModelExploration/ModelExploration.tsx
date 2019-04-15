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

import Icon from 'components/shared/Icon/Icon';

import ModelRecord from '../../../models/ModelRecord';
import BarChart from './BarChart';
import styles from './ModelExploration.module.css';
import ParallelCoordinates from './ParamParallelCoordinates';

interface IParallelData {
  [key: string]: any;
}

interface IKeyValPair {
  key: string;
  value: number | string;
}

interface IChartData {
  [key: string]: any;
}

interface IInitialSelection {
  initialHyperparam: string;
  initialMetric: string;
}

interface ILocalProps {
  expRuns: ModelRecord[];
  initialSelection: IInitialSelection;
}

interface ILocalState {
  aggType: string[];
  computeXAxisFields: object[];
  selectedXAxis: string;
  selectedYAxis: string;
  selectedAggregate: string;
  parallelData: IParallelData;
}

export default class ModelExploration extends React.Component<
  ILocalProps,
  ILocalState
> {
  public xAxisParams: Set<string> = new Set();
  public yAxisParams: Set<string> = new Set();
  public summaryParams: Set<string> = new Set();
  public hyperParams: Set<string> = new Set();
  public mapOptGroup = { metric: false, hyper: false };
  public constructor(props: ILocalProps) {
    super(props);
    this.computeYParamsMetric(props.expRuns);
    this.state = {
      aggType: ['average', 'sum', 'median', 'variance', 'stdev', 'count'],
      computeXAxisFields: this.computeXAxisFields(props.expRuns),
      selectedAggregate: 'average',
      selectedXAxis: props.initialSelection.initialHyperparam,
      selectedYAxis: props.initialSelection.initialMetric,
      parallelData: this.computeParallelData(props.expRuns),
    };
  }

  public render() {
    const { expRuns } = this.props;
    return expRuns ? (
      <div className={styles.summary_wrapper}>
        <div className={styles.chartHeader}>Explore Visualizations</div>
        <div className={styles.chartDescription}>
          Generate charts to visualize trends in data by selecting fields to
          plot as x and y values. Optionally pick fields to group by and specify
          what type of aggregation to use.
        </div>

        <div style={{ display: 'flex' }}>
          <div className={styles.chart_selector}>
            <span className={styles.chart_selector_label}>X Axis: </span>
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
            <Icon type="caret-down" className={styles.chart_selector_arrow} />
          </div>

          <div className={styles.chart_selector}>
            <span className={styles.chart_selector_label}>Y Axis:</span>
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
            <Icon type="caret-down" className={styles.chart_selector_arrow} />
          </div>

          <div className={styles.chart_selector}>
            <span className={styles.chart_selector_label}>Aggregate:</span>
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
            <Icon type="caret-down" className={styles.chart_selector_arrow} />
          </div>
        </div>
        <div>
          <BarChart
            xLabel={this.state.selectedXAxis}
            yLabel={this.state.selectedYAxis}
            data={this.returnAggResults(
              this.state.selectedAggregate,
              this.groupBy(
                this.state.computeXAxisFields,
                (field: IChartData) => {
                  if (field[this.state.selectedXAxis]) {
                    return field[this.state.selectedXAxis];
                  }
                }
              )
            )}
          />
        </div>
        <br />
        <div>
          {' '}
          <div className={styles.chartHeader}>
            {' '}
            Parallel Coordinates of Hyperparameters and Metrics{' '}
          </div>
          <ParallelCoordinates data={this.state.parallelData} />
          <div className={styles.parallelMeta}>
            *click and drag on the y-axis to apply filter chart based on axis
          </div>
        </div>
      </div>
    ) : (
      ''
    );
  }

  // set initial x and y axis
  public getInitialYAxis(experimentRuns: ModelRecord[]) {
    return experimentRuns[0].hyperparameters[0].key;
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
  public groupBy(list: IChartData[], keyGetter: any) {
    const map = new Map();
    list.forEach((item: IChartData) => {
      const key = keyGetter(item);
      const collection = map.get(key);
      if (!collection) {
        if (item[this.state.selectedYAxis]) {
          map.set(key, [item[this.state.selectedYAxis]]);
        }
      } else {
        if (item[this.state.selectedYAxis]) {
          collection.push(item[this.state.selectedYAxis]);
        }
      }
    });
    return map;
  }

  @bind
  public computeYParamsMetric(arr: ModelRecord[]) {
    return _.map(arr, obj => {
      const metricField = _.pick(obj, 'metrics');
      metricField.metrics.forEach((kvPair: any) => {
        this.yAxisParams.add(kvPair.key);
      });
    });
  }

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
    return expRuns
      .map(modeRecord => {
        const fields: any = {};
        if (
          modeRecord.hyperparameters &&
          modeRecord.hyperparameters.length !== 0
        ) {
          modeRecord.hyperparameters.forEach((kvPair: any) => {
            this.xAxisParams.add(`${kvPair.key}`);
            this.hyperParams.add(kvPair.key);
            fields[kvPair.key] =
              kvPair.value.length > 8
                ? kvPair.value.substring(0, 8)
                : kvPair.value;
          });
        }

        if (modeRecord.metrics && modeRecord.metrics.length !== 0) {
          modeRecord.metrics.forEach((kvPair: any) => {
            // this.xAxisParams.add(`${kvPair.key}`);
            fields[kvPair.key] = kvPair.value;
          });
        }
        if (Object.getOwnPropertyNames(fields).length === 0) {
          return;
        }
        fields.experiment_id = modeRecord.experimentId.substring(0, 8);
        fields.project_id = modeRecord.projectId.substring(0, 8);
        fields.exp_run_id = modeRecord.id.substring(0, 8);
        fields.start_time = modeRecord.dateCreated;

        if (modeRecord.codeVersion) {
          fields.code_version = modeRecord.codeVersion;
          this.xAxisParams.add('code_version');
        }
        if (modeRecord.owner) {
          fields.owner = modeRecord.owner;
          this.xAxisParams.add('owner');
          this.summaryParams.add('owner');
        }
        return fields;
      })
      .filter(obj => obj !== undefined);
  }

  // compute Parallel chart's Data
  @bind
  public computeParallelData(expRuns: ModelRecord[]) {
    return expRuns
      .map(modeRecord => {
        const fields: IChartData = {};
        if (modeRecord.hyperparameters) {
          modeRecord.hyperparameters.forEach((kvPair: IKeyValPair) => {
            if (typeof kvPair.value !== 'string') {
              fields[kvPair.key] = kvPair.value;
            }
          });
        }

        if (modeRecord.metrics) {
          modeRecord.metrics.forEach((kvPair: IKeyValPair) => {
            fields[kvPair.key] = kvPair.value;
          });
        }
        if (Object.getOwnPropertyNames(fields).length !== 0) {
          return fields;
        }
      })
      .filter(obj => obj !== undefined);
  }
}
