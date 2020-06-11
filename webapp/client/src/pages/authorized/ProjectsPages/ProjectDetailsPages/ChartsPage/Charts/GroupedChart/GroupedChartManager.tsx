import * as d3 from 'd3';
import React from 'react';

import ChartConfigDropdown, {
  IOption,
} from 'core/shared/view/elements/ChartConfigDropdown/ChartConfigDropdown';
import Collapsable from 'core/shared/view/elements/Collapsable/Collapsable';
import { Icon } from 'core/shared/view/elements/Icon/Icon';
import { cssTheme } from 'core/shared/styles/theme';

import { IGroupedChartSelection } from '../shared/types/chartConfiguration';
import GroupedChart from './chart/GroupedChart';
import MultiLineChart from './chart/MultiLineChart';
import styles from './GroupedChartManager.module.css';
import getGroupedChartData from './utils/prepareGroupedData';
import getGroupedLineChartData from './utils/prepareGroupedLineData';

interface IChartData {
  [key: string]: any;
}

interface ILocalProps {
  initialConfiguration?: Partial<IGroupedChartSelection>;
  data: IChartData;
  metricKeysSet: Set<string>;
  hyperparamKeysSet: Set<string>;
  updateGroupedChartConfig(groupedChartSelection: IGroupedChartSelection): void;
}

interface ILocalState {
  panelConfig: IPanelObject;
  aggregatedData: IAggregatedData[];
  aggregatedLineData: IAggregatedData[];
  selectedMetrics: string[];
  selectedHyperparam: string[];
  selectedChartType: string;
  selectedYAxisType: string;
  selectedPanelElements: string[];
}

interface IPanelObject {
  [key: string]: any;
}
interface IPanelField {
  key: string;
  selected: boolean;
  metricKey: boolean;
}

interface IAggregatedData {
  [key: string]: any;
}

function getUniqueKeys(data: IChartData) {
  return [
    ...new Set(
      Array.prototype.concat.apply(
        [],
        data.map((item: object) => Object.keys(item))
      )
    ),
  ].filter(key => key !== 'modelRecord');
}

function getReducedData(
  data: IChartData,
  metricSet: Set<string>,
  initialSelectedPanelElements?: string[]
): IPanelObject {
  // hadrcoded min no of hyperparam to be selected for grouping
  let hypCounter: number = 1;
  return getUniqueKeys(data).reduce(function(result, item, index, array) {
    const hypSelected: boolean = !metricSet.has(item);
    result[item] = {
      key: item,
      selected: initialSelectedPanelElements
        ? initialSelectedPanelElements.includes(item)
        : hypSelected
        ? hypCounter > 0
          ? true
          : false
        : getUniqueKeys(data)
            .slice(0, 9)
            .includes(item),
      metricKey: false,
    };
    hypCounter--;
    return result;
  }, {});
}

interface IKeyFilterOptions {
  panelObject: IPanelObject;
  metricKeysSet: Set<string>;
  returnMetricKeys: boolean;
}

function filterSelectedKeys(options: IKeyFilterOptions) {
  const { panelObject, metricKeysSet, returnMetricKeys } = options;
  return Object.values(panelObject)
    .filter(config => {
      if (returnMetricKeys) {
        return metricKeysSet.has(config.key);
      }
      return !metricKeysSet.has(config.key);
    })
    .filter(config => config.selected)
    .map(config => config.key);
}

function fetchAllSelectedKeys(panelObject: IPanelObject) {
  return Object.values(panelObject)
    .filter(obj => obj.selected)
    .map(obj => obj.key);
}

class GroupedChartManager extends React.Component<ILocalProps, ILocalState> {
  public chartTypes: string[] = ['multi-line-chart', 'grouped-bar-chart'];
  public yAxisTypes: string[] = ['linear', 'log'];
  public colorScale = d3
    .scaleOrdinal()
    .range([
      cssTheme.bgColor2,
      '#CBE11E',
      '#1ECBE1',
      '#E11ECB',
      '#e6ab02',
      '#a6761d',
      '#666666',
    ]);

  constructor(props: ILocalProps) {
    super(props);

    const { initialConfiguration } = props;

    const panelObject = getReducedData(
        this.props.data,
        this.props.metricKeysSet,
        initialConfiguration && initialConfiguration.selectedPanelElements
      ),
      selectedMetricKeys = filterSelectedKeys({
        panelObject,
        metricKeysSet: this.props.metricKeysSet,
        returnMetricKeys: true,
      }),
      selectedHypKeys = filterSelectedKeys({
        panelObject,
        metricKeysSet: this.props.metricKeysSet,
        returnMetricKeys: false,
      });

    this.state = {
      panelConfig: panelObject,
      aggregatedData: [],
      aggregatedLineData: [],
      selectedChartType:
        initialConfiguration && initialConfiguration.selectedChartType
          ? initialConfiguration.selectedChartType
          : 'multi-line-chart',
      selectedYAxisType:
        initialConfiguration && initialConfiguration.selectedYAxisType
          ? initialConfiguration.selectedYAxisType
          : 'linear',
      selectedMetrics: selectedMetricKeys,
      selectedHyperparam: selectedHypKeys,
      selectedPanelElements: fetchAllSelectedKeys(panelObject),
    };

    getGroupedChartData(props.data, selectedHypKeys, selectedMetricKeys).then(
      aggData => {
        this.setState({ aggregatedData: aggData });
      }
    );
    getGroupedLineChartData(
      this.props.data,
      selectedHypKeys,
      selectedMetricKeys
    ).then(aggData => {
      this.setState({ aggregatedLineData: aggData });
    });
  }

  public componentDidMount() {
    this.props.updateGroupedChartConfig({
      selectedPanelElements: this.state.selectedPanelElements,
      selectedChartType: this.state.selectedChartType,
      selectedYAxisType: this.state.selectedYAxisType,
    });
  }

  public componentDidUpdate(prevProps: ILocalProps) {
    if (prevProps !== this.props) {
      this.setState({
        selectedPanelElements: Object.values(this.state.panelConfig)
          .filter(obj => obj.selected)
          .map(obj => obj.key),
      });
    }
  }

  public render() {
    const reducedArrayOfFeatures = this.state.panelConfig;
    if (this.props.metricKeysSet && [...this.props.metricKeysSet].length > 0) {
      Object.values(this.state.panelConfig).map(obj => {
        if (this.props.metricKeysSet.has(obj.key)) {
          obj.metricKey = true;
        }
        return '';
      });
    }
    return (
      <div className={styles.chart_section_wrapper} data-test="grouped-chart">
        <div className={styles.chart_header_container}>
          <div className={styles.chart_header}>
            Grouped Metric Chart, Aggregated by Hyperparameters
          </div>
        </div>
        <div>
          <div className={styles.parallelControlPanel}>
            <div>
              <div className={styles.chart_config_selectors}>
                <ChartConfigDropdown
                  label="Chart Type :"
                  value={this.state.selectedChartType}
                  options={this.chartTypes}
                  onChange={this.handleChartTypeChange}
                />
                <ChartConfigDropdown
                  label="Y Axis Type :"
                  value={this.state.selectedYAxisType}
                  options={this.yAxisTypes}
                  onChange={this.handleYAxisTypeChange}
                />
              </div>
              <Collapsable
                collapseLabel={' Control Panel'}
                collapsibleContainerID={'groupedChartCollapsible'}
                keepOpen={true}
                children={
                  <div>
                    <div className={styles.metric_key_block}>
                      Select Hyperparameters (x-axis):
                      <div className={styles.panel_content}>
                        {Object.keys(reducedArrayOfFeatures)
                          .filter(
                            item => !reducedArrayOfFeatures[item].metricKey
                          )
                          .map((item, i) => {
                            return (
                              <button
                                key={i}
                                className={
                                  reducedArrayOfFeatures[item].selected
                                    ? styles.panel_buttons
                                    : styles.panel_button_deselected
                                }
                                onClick={this.handleHypButtonClick(item)}
                              >
                                <span>{item}</span>
                              </button>
                            );
                          })}
                      </div>
                    </div>
                    <div className={styles.metric_key_block}>
                      Select Metrics (y-axis):
                      <div className={styles.panel_content}>
                        {Object.keys(reducedArrayOfFeatures)
                          .filter(
                            item => reducedArrayOfFeatures[item].metricKey
                          )
                          .map((item, i) => {
                            return (
                              <button
                                key={i}
                                className={
                                  reducedArrayOfFeatures[item].selected
                                    ? styles.panel_buttons
                                    : styles.panel_button_deselected
                                }
                                onClick={this.handleMetricButtonClick(item)}
                              >
                                <span>{item}</span>
                              </button>
                            );
                          })}
                      </div>
                    </div>
                  </div>
                }
              />
            </div>
            <div>
              {this.state.selectedPanelElements.length >= 11 && (
                <div className={styles.parallelCountMeta}>
                  <div className={styles.pcSuggestionLogo}>
                    <Icon
                      className={styles.desc_action_icon}
                      type={'exclamation-triangle-lite'}
                    />
                  </div>
                  max no. of fields selected, consider deselecting some for
                  better visibility
                </div>
              )}
            </div>
          </div>
          <div>
            {this.state.selectedChartType === 'grouped-bar-chart' && (
              <GroupedChart
                data={this.state.aggregatedData}
                selectedHyperparams={this.state.selectedHyperparam}
                selectedYAxisType={this.state.selectedYAxisType}
                colorScale={this.colorScale}
              />
            )}
            {this.state.selectedChartType === 'multi-line-chart' && (
              <MultiLineChart
                data={this.state.aggregatedLineData}
                selectedHyperparams={this.state.selectedHyperparam}
                selectedYAxisType={this.state.selectedYAxisType}
                colorScale={this.colorScale}
              />
            )}
          </div>
        </div>
      </div>
    );
  }

  public handleChartTypeChange = (option: IOption) => {
    this.setState({ selectedChartType: option.value });
    this.props.updateGroupedChartConfig({
      selectedPanelElements: this.state.selectedPanelElements,
      selectedYAxisType: this.state.selectedYAxisType,
      selectedChartType: option.value,
    });
  };
  public handleYAxisTypeChange = (option: IOption) => {
    this.setState({ selectedYAxisType: option.value });
    this.props.updateGroupedChartConfig({
      selectedPanelElements: this.state.selectedPanelElements,
      selectedChartType: this.state.selectedChartType,
      selectedYAxisType: option.value,
    });
  };
  public handleHypButtonClick = (item: string) => (
    event: React.MouseEvent<HTMLButtonElement, MouseEvent>
  ) => {
    const reducedArrayOfFeatures = this.state.panelConfig;
    const updatedPanelConfig = {
      ...reducedArrayOfFeatures,
      [item]: {
        key: item,
        selected: !reducedArrayOfFeatures[item].selected,
      },
    };

    const newHypSelection = filterSelectedKeys({
      panelObject: updatedPanelConfig,
      metricKeysSet: this.props.metricKeysSet,
      returnMetricKeys: false,
    });

    // recompute aggregations for line and bars based on new selection
    getGroupedChartData(
      this.props.data,
      newHypSelection,
      this.state.selectedMetrics
    ).then(aggData => {
      this.setState({ aggregatedData: aggData });
    });

    getGroupedLineChartData(
      this.props.data,
      newHypSelection,
      this.state.selectedMetrics
    ).then(aggData => {
      this.setState({
        aggregatedLineData: aggData,
      });
    });
    // -----------------------------------

    this.setState({
      panelConfig: updatedPanelConfig,
      selectedPanelElements: Object.values(updatedPanelConfig)
        .filter(config => config.selected === true)
        .map(config => config.key),
      selectedHyperparam: Object.values(updatedPanelConfig)
        .filter(config => !this.props.metricKeysSet.has(config.key))
        .filter(config => config.selected)
        .map(config => config.key),
    });

    this.props.updateGroupedChartConfig({
      selectedChartType: this.state.selectedChartType,
      selectedYAxisType: this.state.selectedYAxisType,
      selectedPanelElements: Object.values(updatedPanelConfig)
        .filter(config => config.selected)
        .map(config => config.key),
    });
  };

  public handleMetricButtonClick = (item: string) => (
    event: React.MouseEvent<HTMLButtonElement, MouseEvent>
  ) => {
    const reducedArrayOfFeatures = this.state.panelConfig;
    const updatedPanelConfig = {
      ...reducedArrayOfFeatures,
      [item]: {
        key: item,
        selected: !reducedArrayOfFeatures[item].selected,
      },
    };

    const newMetricSelection = filterSelectedKeys({
      panelObject: updatedPanelConfig,
      metricKeysSet: this.props.metricKeysSet,
      returnMetricKeys: false,
    });

    // recompute aggregations for line and bars based on new selection
    getGroupedChartData(
      this.props.data,
      this.state.selectedHyperparam,
      newMetricSelection
    ).then(aggData => {
      this.setState({ aggregatedData: aggData });
    });

    getGroupedLineChartData(
      this.props.data,
      this.state.selectedHyperparam,
      newMetricSelection
    ).then(aggData => {
      this.setState({
        aggregatedLineData: aggData,
      });
    });
    // -----------------------------------

    this.setState({
      selectedMetrics: Object.values(updatedPanelConfig)
        .filter(config => this.props.metricKeysSet.has(config.key))
        .filter(config => config.selected)
        .map(config => config.key),
      panelConfig: updatedPanelConfig,
      selectedPanelElements: Object.values(updatedPanelConfig)
        .filter(config => config.selected === true)
        .map(config => config.key),
    });
    this.props.updateGroupedChartConfig({
      selectedChartType: this.state.selectedChartType,
      selectedYAxisType: this.state.selectedYAxisType,
      selectedPanelElements: Object.values(updatedPanelConfig)
        .filter(config => config.selected)
        .map(config => config.key),
    });
    return;
  };
}

export type IGroupedChartProps = ILocalProps;
export { GroupedChartManager };
export default GroupedChartManager;
