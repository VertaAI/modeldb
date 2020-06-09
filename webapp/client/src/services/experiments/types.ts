import Experiment from 'core/shared/models/Experiment';
import { DataWithPagination } from 'core/shared/models/Pagination';

export type ILoadExperimentsResponse = DataWithPagination<Experiment>;
