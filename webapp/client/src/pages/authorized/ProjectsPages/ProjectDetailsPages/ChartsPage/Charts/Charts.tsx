import { ICommunication } from 'core/shared/utils/redux/communication';
import { bind } from 'decko';
import _ from 'lodash';
import React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps, withRouter } from 'react-router';
import { Dispatch, bindActionCreators } from 'redux';

import { IFilterData } from 'core/features/filter/Model';
import PageCommunicationError from 'core/shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import ModelRecord from 'models/ModelRecord';
import {
  resetExperimentRunsSettings,
  selectSequentialChartData,
  selectLoadingSequentialChartData,
  selectLazyChartData,
  selectLoadingLazyChartData,
} from 'store/experimentRuns';
import { selectCurrentContextAppliedFilters } from 'core/features/filter';
import { IApplicationState } from 'store/store';

import AggregationChartManager from './AggregationChart/AggregationChartManager';
import ChartRangeFilter from './ChartRangeFilter/ChartRangeFilterManager';
import styles from './Charts.module.css';
import GroupedChartManager from './GroupedChart/GroupedChartManager';
import ParallelChartManager from './ParallelChart/ParallelChartManager';
import ChartWrapperForNoData from './shared/components/ChartWrapperForNoData/ChartWrapperForNoData';
import {
  IChartConfigurations,
  ISummaryChartSelection,
  IAggregationChartSelection,
  initializeChartConfig,
  IParallelChartSelection,
  IGroupedChartSelection,
  getDefaultChartConfigurations,
  saveChartConfigurations,
  mergeChartsConfigurations,
  resetChartConfigurations,
} from './shared/types/chartConfiguration';
import {
  IChartData,
  IGenericChartData,
  IKeyValPair,
} from './shared/types/chartDataTypes';
import SummaryChartManager from './SummaryChart/SummaryChartManager';

interface ILocalProps {
  projectId: string;
  onResetConfigurations(): void;
}

interface IPropsFromState {
  lazyChartData: ModelRecord[] | null;
  loadingLazyChartData: ICommunication;
  sequentialChartData: ModelRecord[] | null;
  loadingSequentialChartData: ICommunication;
  filters: IFilterData[];
}

interface IActionProps {
  resetExperimentRunsSettings: typeof resetExperimentRunsSettings;
}

type AllProps = IPropsFromState &
  RouteComponentProps &
  ILocalProps &
  IActionProps;

interface ILocalState {
  chartConfigurations: IChartConfigurations;
  initialChartConfig: IChartConfigurations;
  chartModelRecords: IGenericChartData[];
  chartMetricHypFields: IGenericChartData[];
  chartMetricHypNonNumericFields: IGenericChartData[];
  metricKeys: Set<string>;
  hyperparameterKeys: Set<string>;
  isRangeFilterApplied: boolean;
}

class Charts extends React.Component<AllProps, ILocalState> {
  public state: ILocalState = {
    chartConfigurations: initializeChartConfig(),
    initialChartConfig: initializeChartConfig(),
    chartModelRecords: [],
    chartMetricHypFields: [],
    chartMetricHypNonNumericFields: [],
    metricKeys: new Set(),
    hyperparameterKeys: new Set(),
    isRangeFilterApplied: false,
  };

  public UNSAFE_componentWillMount() {
    if (
      this.props.sequentialChartData &&
      this.props.sequentialChartData.length > 0
    ) {
      this.setState({
        chartConfigurations: initializeChartConfig(),
        initialChartConfig: initializeChartConfig(),
        chartModelRecords: [],
        chartMetricHypFields: [],
        chartMetricHypNonNumericFields: [],
        metricKeys: new Set(),
        hyperparameterKeys: new Set(),
        isRangeFilterApplied: false,
      });
      this.computeChartsData(
        this.props.sequentialChartData,
        this.props.projectId
      );
    }
    if (this.props.lazyChartData) {
      this.computeChartsData(this.props.lazyChartData, this.props.projectId);
    }
  }

  public componentDidUpdate(prevProps: AllProps, prevState: ILocalState) {
    if (prevProps.lazyChartData !== this.props.lazyChartData) {
      if (this.props.lazyChartData) {
        this.computeChartsData(this.props.lazyChartData, this.props.projectId);
      }
    }

    if (prevProps.sequentialChartData !== this.props.sequentialChartData) {
      if (this.props.sequentialChartData) {
        this.computeChartsData(
          this.props.sequentialChartData,
          this.props.projectId
        );
      }
    }
    if (prevState.chartConfigurations !== this.state.chartConfigurations) {
      saveChartConfigurations(
        this.state.chartConfigurations,
        this.props.history,
        this.props.projectId
      );
    }
  }

  public render() {
    const {
      projectId,
      lazyChartData,
      loadingLazyChartData,
      loadingSequentialChartData,
    } = this.props;

    return (
      <div className={styles.root} data-test="charts-root">
        {(() => {
          if (loadingLazyChartData.isRequesting) {
            return (
              <div className={styles.preloader}>
                <Preloader variant="dots" />
              </div>
            );
          }
          if (loadingLazyChartData.error || !lazyChartData) {
            return (
              <PageCommunicationError
                error={loadingLazyChartData.error}
                isNillEntity={!lazyChartData}
              />
            );
          }
          return (
            <>
              <ChartRangeFilter
                loadingSequentialChartData={loadingSequentialChartData}
                projectId={projectId}
                paginatedDataLength={this.state.chartModelRecords.length}
                updateIsRangeAppliedAtParent={this.updateIsRangeFilterApplied}
                resetChartConfigAtParent={this.resetConfigurations}
              />

              <SummaryChartManager
                summaryChartConfig={
                  this.state.chartConfigurations.summaryChartSelection
                }
                genericChartData={this.state.chartModelRecords}
                metricKeys={this.state.metricKeys}
                updateSummaryChartConfig={this.updateSummaryChartUserConfig}
              />

              <AggregationChartManager
                aggregationChartConfig={
                  this.state.chartConfigurations.aggregationChartSelection
                }
                genericChartData={this.state.chartModelRecords}
                metricKeys={this.state.metricKeys}
                hyperparameterKeys={this.state.hyperparameterKeys}
                updateAggregationChartConfig={
                  this.updateAggregationChartUserConfig
                }
              />

              {this.state.chartMetricHypNonNumericFields &&
              this.state.metricKeys &&
              this.state.metricKeys.size > 0 ? (
                <div>
                  <ParallelChartManager
                    initialConfiguration={
                      getDefaultChartConfigurations(projectId)
                        .parallelChartSelection
                    }
                    data={this.state.chartMetricHypNonNumericFields}
                    metricKeysSet={this.state.metricKeys}
                    updateParallelChartConfig={
                      this.updateParallelChartUserConfig
                    }
                  />
                </div>
              ) : (
                <ChartWrapperForNoData
                  chartHeading={
                    'Parallel Coordinates of Hyperparameters and Metrics'
                  }
                  message={'No parallel Coordinates for available data'}
                  canvasClassName={'parallelEmptyChart'}
                />
              )}

              {this.state.chartMetricHypFields &&
              this.state.metricKeys &&
              this.state.metricKeys.size > 0 &&
              this.state.hyperparameterKeys &&
              this.state.hyperparameterKeys.size > 0 ? (
                <div>
                  <GroupedChartManager
                    initialConfiguration={
                      getDefaultChartConfigurations(projectId)
                        .groupedChartSelection
                    }
                    data={this.state.chartMetricHypFields}
                    metricKeysSet={this.state.metricKeys}
                    hyperparamKeysSet={this.state.hyperparameterKeys}
                    updateGroupedChartConfig={this.updateGroupedChartUserConfig}
                  />
                </div>
              ) : (
                <ChartWrapperForNoData
                  chartHeading={
                    'Grouped Metric Chart, Aggregated by Hyperparameters'
                  }
                  message={'No grouping results for available data'}
                  canvasClassName={'groupedEmptyChart'}
                />
              )}
            </>
          );
        })()}
      </div>
    );
  }

  @bind
  public computeChartsData(data: ModelRecord[] | null, projectId: string) {
    if (data) {
      this.computeUniqueKeySets(data);
      this.setState({
        chartConfigurations: mergeChartsConfigurations(
          getDefaultChartConfigurations(projectId),
          updateChartConfig(data)
        ),
        initialChartConfig: mergeChartsConfigurations(
          getDefaultChartConfigurations(projectId),
          updateChartConfig(data)
        ),
        chartModelRecords: computeChartModelRecord(data), // data for scatter and agg
        chartMetricHypFields: computeMetricHypFields(
          data,
          false // pass only numeric hyperparameters
        ), // data for grouped chart
        chartMetricHypNonNumericFields: computeMetricHypFields(
          data, // data for parallelChart
          true, // pass only numeric hyperparameters
          true // include a modelRecord object inside flat data
        ),
      });
    }
  }

  @bind
  public updateIsRangeFilterApplied(isApplied: boolean) {
    this.setState({
      isRangeFilterApplied: isApplied,
    });
  }

  @bind
  public resetConfigurations() {
    const data = this.state.chartModelRecords;
    if (data) {
      resetChartConfigurations(this.props.history, this.props.projectId);
      this.setState({
        chartConfigurations: this.state.initialChartConfig,
      });
      this.props.onResetConfigurations();
    }
  }

  @bind
  public updateSummaryChartUserConfig(
    summaryChartUserSelection: ISummaryChartSelection
  ) {
    this.setState(prev => ({
      chartConfigurations: {
        ...prev.chartConfigurations,
        summaryChartSelection: summaryChartUserSelection,
      },
    }));
  }

  @bind
  public updateAggregationChartUserConfig(
    aggregationChartUserSelection: IAggregationChartSelection
  ) {
    this.setState(prev => ({
      chartConfigurations: {
        ...prev.chartConfigurations,
        aggregationChartSelection: aggregationChartUserSelection,
      },
    }));
  }

  @bind
  public updateParallelChartUserConfig(
    parallelChartConfig: IParallelChartSelection
  ) {
    this.setState(prev => ({
      chartConfigurations: {
        ...prev.chartConfigurations,
        parallelChartSelection: parallelChartConfig,
      },
    }));
  }

  @bind public updateGroupedChartUserConfig(
    groupedChartConfig: IGroupedChartSelection
  ) {
    this.setState(prev => ({
      chartConfigurations: {
        ...prev.chartConfigurations,
        groupedChartSelection: groupedChartConfig,
      },
    }));
  }

  @bind
  public computeUniqueKeySets(expRunArr: ModelRecord[]) {
    _.forEach(expRunArr, (modelRecord: ModelRecord) => {
      if (modelRecord.metrics.length > 0) {
        modelRecord.metrics.forEach((kvPair: IKeyValPair) => {
          if (!this.state.metricKeys.has(kvPair.key)) {
            this.setState({
              metricKeys: new Set(this.state.metricKeys.add(kvPair.key)),
            });
          }
        });
      }
      if (modelRecord.hyperparameters.length > 0) {
        modelRecord.hyperparameters.forEach((kvPair: IKeyValPair) => {
          if (!this.state.hyperparameterKeys.has(kvPair.key)) {
            this.setState({
              hyperparameterKeys: new Set(
                this.state.hyperparameterKeys.add(kvPair.key)
              ),
            });
          }
        });
      }
    });
  }
}

// updated dropdowns for all charts with metrics and hyp keys
function updateChartConfig(
  experimentRuns: ModelRecord[] | IGenericChartData[] | undefined | null
) {
  const chartConfig: IChartConfigurations = initializeChartConfig();
  if (experimentRuns && experimentRuns.length > 0) {
    const validMetrics = experimentRuns
        .filter(d => d.metrics.length > 0)
        .map(d => d.metrics),
      validHyperparameters = experimentRuns
        .filter(d => d.hyperparameters.length > 0)
        .map(d => d.hyperparameters);

    // summary chart config
    validMetrics.length > 0
      ? (chartConfig.summaryChartSelection.selectedMetric =
          validMetrics[0][0].key)
      : (chartConfig.summaryChartSelection.selectedMetric =
          'data not available');

    // aggregation chart config
    validMetrics.length > 0
      ? (chartConfig.aggregationChartSelection.selectedMetric =
          validMetrics[0][0].key)
      : (chartConfig.aggregationChartSelection.selectedMetric =
          'data not available');
    validHyperparameters.length > 0
      ? (chartConfig.aggregationChartSelection.selectedHyperparameter =
          validHyperparameters[0][0].key)
      : (chartConfig.aggregationChartSelection.selectedHyperparameter =
          'data not available');

    if (validMetrics.length === 0 && validHyperparameters.length === 0) {
      chartConfig.summaryChartSelection = {
        selectedMetric: 'data not available',
      };
      chartConfig.aggregationChartSelection = {
        selectedMetric: 'data not available',
        selectedHyperparameter: 'data not available',
        selectedAggregationType: 'average',
        selectedChartType: 'bar-chart',
      };
    }
  }
  return chartConfig;
}

// pass striingified for bar charts
function computeMetricHypFields(
  expRunArr: IChartData,
  numericHyperparameters?: boolean,
  includeModelRecord?: boolean
): IGenericChartData[] {
  return _.map(expRunArr, (modelRecord: ModelRecord) => {
    const field: IGenericChartData = {};
    if (
      modelRecord &&
      modelRecord.hyperparameters &&
      modelRecord.hyperparameters.length > 0
    ) {
      _.forEach(modelRecord.hyperparameters, (kvPair: IKeyValPair) => {
        if (numericHyperparameters) {
          if (typeof kvPair.value !== 'string') {
            field[kvPair.key] = kvPair.value; // rounding was done here
          }
        } else {
          field[kvPair.key] = kvPair.value;
        }
      });
    }
    if (modelRecord && modelRecord.metrics && modelRecord.metrics.length > 0) {
      _.forEach(modelRecord.metrics, (kvPair: IKeyValPair) => {
        field[kvPair.key] = kvPair.value;
      });
    }
    if (includeModelRecord) {
      return { ...field, modelRecord };
    }
    return field;
  }).filter(obj => obj !== undefined);
}

function computeChartModelRecord(expRunArr: IChartData): IGenericChartData[] {
  return _.map(expRunArr, (modelRecord: ModelRecord) => {
    const {
      artifacts,
      attributes,
      datasets,
      codeVersion,
      observations,
      dateUpdated,
      startTime,
      endTime,
      description,
      ...reducedModelRecord
    } = modelRecord;
    const chartModelRecord: IGenericChartData = reducedModelRecord;
    if (
      reducedModelRecord &&
      reducedModelRecord.hyperparameters &&
      reducedModelRecord.hyperparameters.length !== 0
    ) {
      _.forEach(reducedModelRecord.hyperparameters, (kvPair: IKeyValPair) => {
        chartModelRecord[kvPair.key] = kvPair.value;
      });
    }
    if (
      reducedModelRecord &&
      reducedModelRecord.metrics &&
      reducedModelRecord.metrics.length !== 0
    ) {
      _.forEach(reducedModelRecord.metrics, (kvPair: IKeyValPair) => {
        chartModelRecord[kvPair.key] = kvPair.value;
      });
    }
    if (Object.getOwnPropertyNames(chartModelRecord).length === 0) {
      return {};
    }
    return chartModelRecord;
  }).filter(obj => obj !== undefined);
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => ({
  lazyChartData: selectLazyChartData(state),
  loadingLazyChartData: selectLoadingLazyChartData(state),
  sequentialChartData: selectSequentialChartData(state),
  loadingSequentialChartData: selectLoadingSequentialChartData(state),
  filters: selectCurrentContextAppliedFilters(state),
});

const mapDispatchToProps = (dispatch: Dispatch): IActionProps => {
  return bindActionCreators({ resetExperimentRunsSettings }, dispatch);
};

export type IChartPropsFromState = IPropsFromState;
export type IChartProps = AllProps;
export { Charts };

export default withRouter(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(Charts)
);
