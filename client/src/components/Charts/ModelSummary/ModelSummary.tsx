import _ from 'lodash';
import React from 'react';
import ModelRecord from '../../../models/ModelRecord';
import ScatterChart from './ScatterChart';
import styles from './ModelSummary.module.css';

interface ILocalProps {
  experimentRuns: ModelRecord[];
  initialYSelection: string;
}

interface IChartData {
  [key: string]: any;
}

interface ILocalState {
  selectedMetric: string;
  chartData: IChartData;
}
export default class ModelExploration extends React.Component<
  ILocalProps,
  ILocalState
> {
  public yAxisParams: Set<string> = new Set(); // metric fields
  public constructor(props: ILocalProps) {
    super(props);

    this.state = {
      chartData: this.computeFlatMetric(props.experimentRuns),
      selectedMetric: props.initialYSelection,
    };
  }

  public render() {
    return (
      <div>
        <div className={styles.chart_selector}>
          <span className={styles.chart_selector_label}>Metric :</span>
          <select
            name="selected-metric"
            onChange={this.handleMetricChange}
            className={styles.dropdown}
          >
            {[...this.yAxisParams].map((param: string, i: number) => {
              return (
                <option key={i} value={param}>
                  {param}
                </option>
              );
            })}
          </select>
        </div>
        <ScatterChart
          flatdata={this.state.chartData}
          selectedMetric={this.state.selectedMetric}
        />
      </div>
    );
  }

  public handleMetricChange = (event: React.FormEvent<HTMLSelectElement>) => {
    const element = event.target as HTMLSelectElement;
    this.setState({ selectedMetric: element.value });
  };

  // Utilities
  public computeFlatMetric = (arr: ModelRecord[]) => {
    return _.map(arr, obj => {
      const metricField = _.pick(obj, 'startTime', 'metrics', 'id');
      const flatMetric: any = {
        date: metricField.startTime,
        id: metricField.id,
      };
      metricField.metrics.forEach((kvPair: any) => {
        this.yAxisParams.add(kvPair.key);
        flatMetric[kvPair.key] = kvPair.value;
      });
      return flatMetric;
    });
  };
}
