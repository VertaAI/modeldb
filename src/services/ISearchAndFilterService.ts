import { IMetaData, MetaData } from 'models/IMetaData';
import { IFilterData } from '../components/FilterSelect/FilterSelect';

export default interface ISearchAndFilterService<T extends MetaData> {
  searchFilters(searchString: string): Promise<IFilterData[]>;
  search(searchString: string): Promise<T[]>;
  setMetaData(meta: IMetaData[]): void;
}
