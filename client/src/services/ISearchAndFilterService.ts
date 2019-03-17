import { IFilterContextData } from 'store/filter';
import { IFilterData } from '../models/Filters';

export default interface ISearchAndFilterService {
  searchFilterSuggestions(searchString: string, ctx?: IFilterContextData): Promise<IFilterData[]>;
}
