import { IMetaData, MetaData } from 'models/IMetaData';
import Project from 'models/Project';
import { IFilterData } from '../components/FilterSelect/FilterSelect';
import ISearchAndFilterService from './ISearchAndFilterService';

export default class MockSearchAndFiltersService implements ISearchAndFilterService<Project> {
  private filters: string[] = [];

  private metaData: IMetaData[] = [];

  public searchFilters(searchString: string): Promise<IFilterData[]> {
    return new Promise<IFilterData[]>((resolve, reject) => {
      resolve(this.tryFindFilters(searchString));
    });
  }

  public search<T extends MetaData>(searchString: string): Promise<T[]> {
    return new Promise<T[]>((resolve, reject) => {
      resolve([]);
    });
  }
  public setMetaData(meta: IMetaData[]): void {
    this.metaData = meta;
    this.filters = [];
    meta.forEach(data => (data.propertyName ? this.filters.push(data.propertyName) : false));
  }
  private tryFindFilters(searchString: string): IFilterData[] {
    const searchParts: string[] = searchString
      .trim()
      .toUpperCase()
      .split(' ');

    if (searchParts.length > 1) {
      let foundFilters: string[] = [];
      let endIdx: number = 0;
      let potentialFilterName: string = '';

      searchParts.forEach(searchPart => {
        potentialFilterName = `${potentialFilterName} ${searchPart}`.trim();

        let potentialFilters = foundFilters.length === 0 ? this.filters : foundFilters;
        potentialFilters = potentialFilters.filter(filterName => filterName.toUpperCase().indexOf(potentialFilterName) > -1);

        if (potentialFilters.length > 0) {
          foundFilters = potentialFilters;
          endIdx = searchString.toUpperCase().indexOf(potentialFilterName) + potentialFilterName.length;
        }
      });

      const result: IFilterData[] = [];
      const value: string = searchString.substr(endIdx).trim();

      if (value.length > 0) {
        foundFilters.forEach(foundFilter => {
          result.push({ propertyName: foundFilter, propertyValue: value });
        });

        return result;
      }
    }
    return [];
  }
}
