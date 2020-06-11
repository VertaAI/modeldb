import Experiment from 'models/Experiment';
import { DataWithPagination } from 'core/shared/models/Pagination';

export type ILoadExperimentsResponse = DataWithPagination<Experiment>;
