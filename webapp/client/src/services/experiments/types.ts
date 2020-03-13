import Experiment from 'models/Experiment';

export interface ILoadExperimentsResponse {
  experiments: Experiment[];
  totalCount: number;
}
