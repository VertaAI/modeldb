import Experiment from 'shared/models/Experiment';
import { DataWithPagination } from 'shared/models/Pagination';

export type ILoadExperimentsResponse = DataWithPagination<Experiment>;
