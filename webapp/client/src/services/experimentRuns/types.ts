import { DataWithPagination } from 'core/shared/models/Pagination';
import ModelRecord from 'models/ModelRecord';

import * as Comments from 'features/comments';

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
