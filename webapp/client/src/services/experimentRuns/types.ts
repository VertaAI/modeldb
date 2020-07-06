import { IComment } from 'shared/models/Comment';
import ModelRecord from 'shared/models/ModelRecord';
import { DataWithPagination } from 'shared/models/Pagination';

export type ILoadExperimentRunsResult = DataWithPagination<
  ILoadModelRecordResult
>;

export interface ILoadModelRecordResult {
  experimentRun: ModelRecord;
  comments: IComment[];
}

export interface ILazyLoadChartData {
  lazyChartData: ModelRecord[];
  totalCount: number;
}
