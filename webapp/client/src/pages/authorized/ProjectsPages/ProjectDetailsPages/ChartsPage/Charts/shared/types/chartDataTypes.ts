import ModelRecord from 'models/ModelRecord';

export type IChartData = ModelRecord[] | null | undefined;

// TODO: move a reduced data fetch/parse on querying complete list of expRuns to reduce data prep time -----
export type ChartDataField = ModelRecord;

export type ValidChartData = ModelRecord[];
// -------------

export interface IGenericChartData {
  [key: string]: any;
}

export interface IKeyValPair {
  key: string;
  value: number | string;
}
export type Category = 'experimentId';

export const Category: Record<Category, Category> = {
  experimentId: 'experimentId',
};
