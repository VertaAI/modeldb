import { bind } from 'decko';
import _ from 'lodash';
import React from 'react';

import ChartConfigDropdown, {
  IOption,
} from 'core/shared/view/elements/ChartConfigDropdown/ChartConfigDropdown';

import { ISummaryChartSelection } from '../shared/types/chartConfiguration';
import { IGenericChartData, Category } from '../shared/types/chartDataTypes';
import ScatterChart from './charts/ScatterChart';
import styles from './SummaryChartManager.module.css';

interface ILocalProps {
  summaryChartConfig: any; // config used as initial render and with updated expRuns
  metricKeys: Set<string>;
  genericChartData: IGenericChartData;
  updateSummaryChartConfig(summaryChartSelection: ISummaryChartSelection): void; // config used to update global object and local state of selection
}

interface ILocalState {
  userSelectedMetricVal: string;
  selectedCategory: Category;
}

class SummaryChartManager extends React.Component<ILocalProps, ILocalState> {
  public state: ILocalState = {
    userSelectedMetricVal: this.props.summaryChartConfig.selectedMetric,
    selectedCategory: 'experimentId',
  };

  public componentDidUpdate(prevProps: ILocalProps) {
    if (prevProps !== this.props) {
      this.setState({
        userSelectedMetricVal: this.props.summaryChartConfig.selectedMetric,
      });
    }
  }

  public render() {
    const { genericChartData, metricKeys } = this.props;
    return (
      <div className={styles.chart_section_wrapper} data-test="summary-chart">
        <div className={styles.chart_header_container}>
          <div className={styles.chart_header}> Metric Summary Chart </div>
        </div>
        <div className={styles.chart_config_selectors}>
          <ChartConfigDropdown
            value={this.state.userSelectedMetricVal}
            label="Metric :"
            options={metricKeys}
            onChange={this.handleMetricChange}
          />
          <ChartConfigDropdown
            label="Category :"
            value={this.state.selectedCategory}
            options={Object.values(Category)}
            onChange={this.handleCategoryChange}
          />
        </div>

        <ScatterChart
          flatdata={genericChartData}
          selectedMetric={this.state.userSelectedMetricVal}
          selectedCategory={this.state.selectedCategory}
        />
      </div>
    );
  }

  @bind
  public handleCategoryChange(option: IOption) {
    const selectedCategory = option.value as Category;
    this.setState({ selectedCategory });
  }

  @bind
  private handleMetricChange(option: IOption) {
    this.setState({ userSelectedMetricVal: option.value });
    this.props.updateSummaryChartConfig({
      selectedMetric: option.value,
    });
  }
}

export type ISummaryChartProps = ILocalProps;
export { SummaryChartManager };
export default SummaryChartManager;
