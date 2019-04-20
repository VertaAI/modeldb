import { IFilterData } from 'models/Filters';
import { IFilterContextData } from 'store/filter';

export interface ISearchAndFilterService {
  searchFilterSuggestions(
    searchString: string,
    ctx?: IFilterContextData
  ): Promise<IFilterData[]>;
}
