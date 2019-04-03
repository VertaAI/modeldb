import { IMetaData } from 'models/IMetaData';

import { ComparisonType, IFilterData, PropertyType } from 'models/Filters';
import { IFilterContextData } from 'store/filter';

import { ISearchAndFilterService } from './ISearchAndFilterService';

export default class MockSFService implements ISearchAndFilterService {
  public searchFilterSuggestions(
    searchString: string,
    data?: IFilterContextData
  ): Promise<IFilterData[]> {
    return new Promise<IFilterData[]>((resolve, reject) => {
      if (data === undefined) {
        reject();
      } else {
        const metadata: IMetaData[] = data.ctx.getMetadata();
        if (metadata !== undefined) {
          resolve(this.tryFindFilters(searchString, metadata));
        } else {
          reject();
        }
      }
    });
  }

  private tryFindFilters(
    searchString: string,
    metadata: IMetaData[]
  ): IFilterData[] {
    const filters: IFilterData[] = metadata.map(data =>
      this.createDefaultFilterDataByType(data.type, data.propertyName)
    );

    const searchParts: string[] = searchString
      .trim()
      .toUpperCase()
      .split(' ');

    if (searchParts.length > 1) {
      let foundFilters: IFilterData[] = [];
      let endIdx: number = 0;
      let potentialFilterName: string = '';

      searchParts.forEach(searchPart => {
        potentialFilterName = `${potentialFilterName} ${searchPart}`.trim();

        let potentialFilters =
          foundFilters.length === 0 ? filters : foundFilters;
        potentialFilters = potentialFilters.filter(
          filterData =>
            filterData.name.toUpperCase().indexOf(potentialFilterName) > -1
        );

        if (potentialFilters.length > 0) {
          foundFilters = potentialFilters;
          endIdx =
            searchString.toUpperCase().indexOf(potentialFilterName) +
            potentialFilterName.length;
        }
      });

      const result: IFilterData[] = [];
      const value: string = searchString.substr(endIdx).trim();

      if (value.length > 0) {
        foundFilters.forEach(foundFilter => {
          result.push(
            this.createDefaultFilterDataByType(
              foundFilter.type,
              foundFilter.name,
              value
            )
          );
        });

        return result;
      }
    }
    return [];
  }

  private createDefaultFilterDataByType(
    type: PropertyType,
    name: string,
    value?: string
  ): IFilterData {
    if (type === PropertyType.STRING) {
      return { name, type, invert: false, value: (value as unknown) as '' };
    }

    if (type === PropertyType.NUMBER) {
      return { name, type, invert: false, value: (value as unknown) as number };
    }

    if (type === PropertyType.METRIC) {
      return {
        name,
        type,
        value: (value as unknown) as number,
        comparisonType: ComparisonType.MORE,
      };
    }

    throw Error('Unknown type');
  }
}
