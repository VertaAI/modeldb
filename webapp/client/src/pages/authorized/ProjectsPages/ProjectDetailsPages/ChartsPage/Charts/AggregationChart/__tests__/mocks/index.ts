export const mockNoMetricAggChartConfig = {
  selectedMetric: 'data not available',
  selectedHyperparameter: 'C',
  selectedAggregationType: 'average',
  selectedChartType: 'bar-chart',
};
export const mockNoHypAggChartConfig = {
  selectedMetric: 'val_acc',
  selectedHyperparameter: 'data not available',
  selectedAggregationType: 'average',
  selectedChartType: 'bar-chart',
};
export const mockAggChartConfig = {
  selectedMetric: 'val_acc',
  selectedHyperparameter: 'C',
  selectedAggregationType: 'average',
  selectedChartType: 'bar-chart',
};
export const mockNoMetricKeys: Set<string> = new Set();
export const mockNoHyperparamKeys: Set<string> = new Set();
export const mockMetricKeys: Set<string> = new Set(['val_acc']);
export const mockHyperparamKeys: Set<string> = new Set([
  'C',
  'max_iter',
  'solver',
]);
