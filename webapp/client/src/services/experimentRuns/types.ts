import { IComment } from 'core/shared/models/Comment';
import ModelRecord from 'core/shared/models/ModelRecord';
import { DataWithPagination } from 'core/shared/models/Pagination';

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
