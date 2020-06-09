import { DataWithPagination } from 'core/shared/models/Pagination';
import ModelRecord from 'core/shared/models/ModelRecord';

import * as Comments from 'core/features/comments';

export type ILoadExperimentRunsResult = DataWithPagination<
  ILoadModelRecordResult
>;

export interface ILoadModelRecordResult {
  experimentRun: ModelRecord;
  comments: Comments.Model.IComment[];
}

export interface ILazyLoadChartData {
  lazyChartData: ModelRecord[];
  totalCount: number;
}
