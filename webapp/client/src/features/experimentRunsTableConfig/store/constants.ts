import { IColumnConfig } from './types';

export const defaultColumnConfig: IColumnConfig = {
  summary: {
    isShown: true,
    name: 'summary',
    label: 'Run Summary',
    order: 0,
  },
  metrics: { isShown: true, name: 'metrics', label: 'Metrics', order: 1 },
  hyperparameters: {
    isShown: true,
    name: 'hyperparameters',
    label: 'Hyperparameters',
    order: 2,
  },
  artifacts: { isShown: true, name: 'artifacts', label: 'Artifacts', order: 3 },
  observations: {
    isShown: true,
    name: 'observations',
    label: 'Observations',
    order: 4,
  },
  attributes: {
    isShown: false,
    name: 'attributes',
    label: 'Attributes',
    order: 5,
  },
  codeVersion: {
    isShown: false,
    name: 'codeVersion',
    label: 'Code Version',
    order: 6,
  },
  datasets: {
    isShown: false,
    name: 'datasets',
    label: 'Datasets',
    order: 7,
  },
};
