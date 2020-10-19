import { bind } from 'decko';
import React from 'react';

import ChartConfigDropdown, {
  IOption,
} from 'shared/view/elements/ChartConfigDropdown/ChartConfigDropdown';
import {
  listAverage,
  listCount,
  listMedian,
  listStdev,
  listSum,
  listVariance,
} from 'shared/utils/statMethods/AggregationTypes';
import { isNumeric } from 'shared/utils/typeChecker/numFormatChecker';

import { IAggregationChartSelection } from '../shared/types/chartConfiguration';
import { IGenericChartData } from '../shared/types/chartDataTypes';
import styles from './AggregationChartManager.module.css';
import BarChart from './charts/BarChart';
import BoxPlot from './charts/BoxPlot';

interface ILocalProps {
  aggregationChartConfig: IAggregationChartSelection;
  metricKeys: Set<string>;
  hyperparameterKeys: Set<string>;
  genericChartData: IGenericChartData[];
  updateAggregationChartConfig(
    aggregationChartSelection: IAggregationChartSelection
  ): void;
}

interface ILocalState {
  userSelectedMetricVal: string;
  userSelectedHyperparameterVal: string;
  userSelectedAggregationType: string;
  userSelectedChartType: string;
  aggregationTypes: string[];
  chartTypes: string[];
}

class AggregationChartManager extends React.Component<
  ILocalProps,
  ILocalState
> {
  public state: ILocalState = {
    userSelectedMetricVal: this.props.aggregationChartConfig.selectedMetric,
    userSelectedHyperparameterVal: this.props.aggregationChartConfig
      .selectedHyperparameter,
    userSelectedAggregationType: this.props.aggregationChartConfig
      .selectedAggregationType,
    userSelectedChartType: this.props.aggregationChartConfig.selectedChartType,
    aggregationTypes: [
      'average',
      'sum',
      'median',
      'variance',
      'stdev',
      'count',
    ],
    chartTypes: ['bar-chart', 'box-plot'],
  };

  public missingCount: number = 0;

  public componentDidUpdate(prevProps: ILocalProps) {
    if (prevProps !== this.props) {
      this.setState({
        userSelectedMetricVal: this.props.aggregationChartConfig.selectedMetric,
        userSelectedHyperparameterVal: this.props.aggregationChartConfig
          .selectedHyperparameter,
      });
    }
  }

  public render() {
    const { genericChartData, metricKeys, hyperparameterKeys } = this.props;
    return (
      <div
        className={styles.chart_section_wrapper}
        data-test="aggregation-chart"
      >
        <div className={styles.chart_header}>
          Aggregated Metrics/Hyperparameters Chart
        </div>
        <div className={styles.chart_config_selectors}>
          <ChartConfigDropdown
            label="Metric :"
            value={this.state.userSelectedMetricVal}
            options={metricKeys}
            onChange={this.handleMetricChange}
          />
          <ChartConfigDropdown
            value={this.state.userSelectedHyperparameterVal}
            label="Hyperparameter :"
            options={hyperparameterKeys}
            onChange={this.handleHyperparameterChange}
          />

          <ChartConfigDropdown
            isDisabled={
              this.state.userSelectedChartType === 'box-plot' ? true : false
            }
            label="Aggregation Types :"
            value={this.state.userSelectedAggregationType}
            options={this.state.aggregationTypes}
            onChange={this.handleAggregationTypeChange}
          />

          <ChartConfigDropdown
            label="Chart Type :"
            value={this.state.userSelectedChartType}
            options={this.state.chartTypes}
            onChange={this.handleChartTypeChange}
          />
        </div>
        <div>
          {this.state.userSelectedChartType === 'bar-chart' ? (
            <BarChart
              xLabel={this.state.userSelectedHyperparameterVal}
              yLabel={this.state.userSelectedMetricVal}
              data={this.returnAggResults(
                this.state.userSelectedAggregationType,
                this.groupBy(genericChartData, (field: IGenericChartData) => {
                  if (field[this.state.userSelectedHyperparameterVal]) {
                    return field[this.state.userSelectedHyperparameterVal];
                  }
                })
              )}
            />
          ) : (
            ''
          )}
          {this.state.userSelectedChartType === 'box-plot' ? (
            <BoxPlot
              xLabel={this.state.userSelectedHyperparameterVal}
              yLabel={this.state.userSelectedMetricVal}
              data={this.groupBy(
                genericChartData,
                (field: IGenericChartData) => {
                  if (field[this.state.userSelectedHyperparameterVal]) {
                    return field[this.state.userSelectedHyperparameterVal];
                  }
                }
              )}
            />
          ) : (
            ''
          )}
          {this.missingCount > 0 && (
            <div className={styles.missing_count_block}>
              missing data count {this.missingCount} - [
              {this.state.userSelectedHyperparameterVal}]
            </div>
          )}
        </div>
      </div>
    );
  }

  // Utilities
  @bind
  public groupBy(list: IGenericChartData[], keyGetter: any) {
    const map = new Map();
    let localMissingCount = 0;
    list.forEach((item: IGenericChartData) => {
      const key = keyGetter(item);
      if (key) {
        const collection = map.get(key);
        if (!collection) {
          if (item[this.state.userSelectedMetricVal]) {
            map.set(key, [item[this.state.userSelectedMetricVal]]);
          }
        } else {
          if (item[this.state.userSelectedMetricVal]) {
            collection.push(item[this.state.userSelectedMetricVal]);
          }
        }
      } else {
        localMissingCount++;
      }
    });

    const sortStringKeys = (a: string[], b: string[]) => {
      if (isNumeric(a[0]) && isNumeric(b[0])) {
        const a0 = parseFloat(a[0]);
        const b0 = parseFloat(b[0]);
        return a0 > b0 ? 1 : -1;
      }
      return a[0] > b[0] ? 1 : -1;
    };
    this.missingCount = localMissingCount;
    return new Map([...map].sort(sortStringKeys));
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
    return [...mapObj].map((obj) => {
      return { key: obj[0], value: listAverage(obj[1]) };
    });
  }

  @bind
  public sumReduceMetrics(mapObj: any) {
    return [...mapObj].map((obj) => {
      return { key: obj[0], value: listSum(obj[1]) };
    });
  }

  @bind
  public medianReduceMetrics(mapObj: any) {
    return [...mapObj].map((obj) => {
      return { key: obj[0], value: listMedian(obj[1]) };
    });
  }

  @bind
  public varianceReduceMetrics(mapObj: any) {
    return [...mapObj].map((obj) => {
      return { key: obj[0], value: listVariance(obj[1]) };
    });
  }

  @bind
  public stdevReduceMetrics(mapObj: any) {
    return [...mapObj].map((obj) => {
      return { key: obj[0], value: listStdev(obj[1]) };
    });
  }

  @bind
  public countReduceMetrics(mapObj: any) {
    return [...mapObj].map((obj) => {
      return { key: obj[0], value: listCount(obj[1]) };
    });
  }

  @bind
  private handleMetricChange(option: IOption) {
    this.setState({ userSelectedMetricVal: option.value });
    this.props.updateAggregationChartConfig({
      selectedHyperparameter: this.state.userSelectedHyperparameterVal,
      selectedAggregationType: this.state.userSelectedAggregationType,
      selectedChartType: this.state.userSelectedChartType,
      selectedMetric: option.value,
    });
  }

  @bind
  private handleHyperparameterChange(option: IOption) {
    this.setState({ userSelectedHyperparameterVal: option.value });
    this.props.updateAggregationChartConfig({
      selectedMetric: this.state.userSelectedMetricVal,
      selectedAggregationType: this.state.userSelectedAggregationType,
      selectedChartType: this.state.userSelectedChartType,
      selectedHyperparameter: option.value,
    });
  }

  @bind
  private handleAggregationTypeChange(option: IOption) {
    this.setState({ userSelectedAggregationType: option.value });
    this.props.updateAggregationChartConfig({
      selectedMetric: this.state.userSelectedMetricVal,
      selectedHyperparameter: this.state.userSelectedHyperparameterVal,
      selectedChartType: this.state.userSelectedChartType,
      selectedAggregationType: option.value,
    });
  }

  @bind
  private handleChartTypeChange(option: IOption) {
    this.setState({ userSelectedChartType: option.value });
    this.props.updateAggregationChartConfig({
      selectedMetric: this.state.userSelectedMetricVal,
      selectedHyperparameter: this.state.userSelectedHyperparameterVal,
      selectedAggregationType: this.state.userSelectedAggregationType,
      selectedChartType: option.value,
    });
  }
}

export type IAggregationChartProps = ILocalProps;
export { AggregationChartManager };
export default AggregationChartManager;
