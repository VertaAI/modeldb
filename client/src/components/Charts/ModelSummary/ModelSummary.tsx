import _ from 'lodash';
import React from 'react';

import ModelRecord from 'models/ModelRecord';
import ScatterChart from './ScatterChart';
import styles from './ModelSummary.module.css';

interface ILocalProps {
  experimentRuns: ModelRecord[];
  initialYSelection: string;
}

interface IKeyValPair {
  key: string;
  value: number | string;
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
  public yAxisParams: Set<string> = new Set();
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
        <div className={styles.scatterMeta}>
          *click on the marks to view corresponding ModelRecord
        </div>
      </div>
    );
  }

  public handleMetricChange = (event: React.FormEvent<HTMLSelectElement>) => {
    const element = event.target as HTMLSelectElement;
    this.setState({ selectedMetric: element.value });
  };

  // Utilities
  public computeFlatMetric = (arr: ModelRecord[]) => {
    return _.map(arr, metricField => {
      const flatMetric: IChartData = metricField;
      if (metricField.metrics.length !== 0) {
        metricField.metrics.forEach((kvPair: IKeyValPair) => {
          this.yAxisParams.add(kvPair.key);
          flatMetric[kvPair.key] = kvPair.value;
        });
        return flatMetric;
      }
    }).filter(obj => obj !== undefined);
  };
}
