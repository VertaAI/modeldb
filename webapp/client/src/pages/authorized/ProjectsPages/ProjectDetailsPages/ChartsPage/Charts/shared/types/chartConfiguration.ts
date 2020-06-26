import { History } from 'history';

export interface IRangeFilterSelection {
  filterSet: boolean;
  filteredExpRunsData: any;
}

export interface ISummaryChartSelection {
  selectedMetric: string;
}

export interface IAggregationChartSelection {
  selectedMetric: string;
  selectedHyperparameter: string;
  selectedAggregationType: string;
  selectedChartType: string;
}

export interface IParallelChartSelection {
  selectedPanelElements: string[];
}

export interface IGroupedChartSelection {
  selectedYAxisType: string;
  selectedChartType: string;
  selectedPanelElements: string[];
}

export interface IChartConfigurations {
  summaryChartSelection: ISummaryChartSelection;
  aggregationChartSelection: IAggregationChartSelection;
  parallelChartSelection: IParallelChartSelection;
  groupedChartSelection: IGroupedChartSelection;
}

export function initializeChartConfig(): IChartConfigurations {
  return {
    summaryChartSelection: {
      selectedMetric: 'data not available',
    },
    aggregationChartSelection: {
      selectedMetric: 'data not available',
      selectedHyperparameter: 'data not available',
      selectedAggregationType: 'average',
      selectedChartType: 'bar-chart',
    },
    parallelChartSelection: { selectedPanelElements: [] },
    groupedChartSelection: {
      selectedPanelElements: [],
      selectedChartType: 'multi-line-chart',
      selectedYAxisType: 'linear',
    },
  };
}

export type IDefaultChartConfigurations = Partial<IChartConfigurations>;

export function getDefaultChartConfigurations(
  projectId: string
): IDefaultChartConfigurations {
  const configurationsFromUrl = getDefaultChartConfigurationsFromUrl();
  const configurationsFromLocalStorage = getDefaultChartConfigurationsFromLocalStorage(
    projectId
  );
  return Object.values(configurationsFromUrl).every((v) => !v)
    ? configurationsFromLocalStorage
    : configurationsFromUrl;
}

export function saveChartConfigurations(
  chartConfigurations: IChartConfigurations,
  history: History,
  projectId: string
) {
  saveChartConfigurationsInUrl(chartConfigurations, history);
  saveChartConfigurationsInLocalStorage(chartConfigurations, projectId);
}

export function resetChartConfigurations(history: History, projectId: string) {
  const urlSearchParams = new URLSearchParams(history.location.search);

  urlSearchParams.delete('summaryChart');
  urlSearchParams.delete('aggregationChart');
  urlSearchParams.delete('groupedChart');
  urlSearchParams.delete('parallelChart');

  localStorage.removeItem(`chartConfigurations_${projectId}`);

  history.push({
    search: String(urlSearchParams),
  });
}

function saveChartConfigurationsInUrl(
  chartConfigurations: IChartConfigurations,
  history: History
) {
  const urlSearchParams = new URLSearchParams(history.location.search);

  urlSearchParams.set(
    'summaryChart',
    encodeURIComponent(
      JSON.stringify(chartConfigurations.summaryChartSelection)
    )
  );
  urlSearchParams.set(
    'aggregationChart',
    encodeURIComponent(
      JSON.stringify(chartConfigurations.aggregationChartSelection)
    )
  );
  urlSearchParams.set(
    'groupedChart',
    encodeURIComponent(
      JSON.stringify(chartConfigurations.groupedChartSelection)
    )
  );
  urlSearchParams.set(
    'parallelChart',
    encodeURIComponent(
      JSON.stringify(chartConfigurations.parallelChartSelection)
    )
  );

  history.push({
    search: String(urlSearchParams),
  });
}
function getDefaultChartConfigurationsFromUrl(): IDefaultChartConfigurations {
  const urlSearchParams = new URLSearchParams(window.location.search);

  const summaryChartConfigurationFromUrl = urlSearchParams.get('summaryChart');
  const aggregationChartConfigurationFromUrl = urlSearchParams.get(
    'aggregationChart'
  );
  const groupedChartConfigurationFromUrl = urlSearchParams.get('groupedChart');
  const parallelChartConfigurationFromUrl = urlSearchParams.get(
    'parallelChart'
  );

  return {
    summaryChartSelection: summaryChartConfigurationFromUrl
      ? JSON.parse(decodeURIComponent(summaryChartConfigurationFromUrl))
      : undefined,
    aggregationChartSelection: aggregationChartConfigurationFromUrl
      ? JSON.parse(decodeURIComponent(aggregationChartConfigurationFromUrl))
      : undefined,
    groupedChartSelection: groupedChartConfigurationFromUrl
      ? JSON.parse(decodeURIComponent(groupedChartConfigurationFromUrl))
      : undefined,
    parallelChartSelection: parallelChartConfigurationFromUrl
      ? JSON.parse(decodeURIComponent(parallelChartConfigurationFromUrl))
      : undefined,
  };
}

function saveChartConfigurationsInLocalStorage(
  chartConfigurations: IChartConfigurations,
  projectId: string
): void {
  localStorage.setItem(
    `chartConfigurations_${projectId}`,
    JSON.stringify(chartConfigurations)
  );
}
function getDefaultChartConfigurationsFromLocalStorage(
  projectId: string
): IDefaultChartConfigurations {
  const chartConfigurationsFromLocalStorage = localStorage.getItem(
    `chartConfigurations_${projectId}`
  );
  return chartConfigurationsFromLocalStorage
    ? JSON.parse(chartConfigurationsFromLocalStorage)
    : {
        aggregationChartSelection: undefined,
        groupedChartSelection: undefined,
        parallelChartSelection: undefined,
        summaryChartSelection: undefined,
      };
}

export const mergeChartsConfigurations = (
  chartConfiguration1: Partial<IChartConfigurations>,
  chartConfiguration2: IChartConfigurations
): IChartConfigurations => {
  return {
    aggregationChartSelection:
      chartConfiguration1.aggregationChartSelection ||
      chartConfiguration2.aggregationChartSelection,
    groupedChartSelection:
      chartConfiguration1.groupedChartSelection ||
      chartConfiguration2.groupedChartSelection,
    parallelChartSelection:
      chartConfiguration1.parallelChartSelection ||
      chartConfiguration2.parallelChartSelection,
    summaryChartSelection:
      chartConfiguration1.summaryChartSelection ||
      chartConfiguration2.summaryChartSelection,
  };
};
