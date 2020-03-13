import { Plugin, Getter, Getters, Action } from '@devexpress/dx-react-core';
import { bind } from 'decko';
import * as R from 'ramda';
import * as React from 'react';

import { ISorting } from 'core/shared/models/Sorting';

interface ILocalProps<T, B extends string> {
  columnNames: B[];
  sorting: ISorting | null;
  getFieldNames(row: T, columnName: B): string[];
  onSortingChange(sorting: ISorting | null): void;
}

type AllProps<T, B extends string> = ILocalProps<T, B>;

export interface IColumnFieldSortingPluginGetters {
  columnFieldSorting: ISorting | null;
  columnFieldSortingNames: string[];
  isAvailable: boolean;
  sortableFieldsByColumnName: Record<string, string[] | undefined>;
}

export interface IColumnFieldSortingPluginActions {
  changeColumnFieldSorting(sorting: ISorting | null): void;
}

export default class ColumnFieldSorting<
  T,
  B extends string
> extends React.Component<AllProps<T, B>> {
  public render() {
    const { sorting, columnNames } = this.props;
    return (
      <Plugin name="ColumnFieldSorting">
        <Getter name="isAvailable" value={true} />
        <Getter name="columnFieldSorting" value={sorting} />
        <Getter name="columnFieldSortingNames" value={columnNames} />
        <Getter
          name="sortableFieldsByColumnName"
          computed={this.getSortableFieldsByColumnName}
        />
        <Action
          name="changeColumnFieldSorting"
          action={this.changeColumnFieldSorting}
        />
      </Plugin>
    );
  }

  @bind
  private getSortableFieldsByColumnName({
    rows,
  }: Getters): IColumnFieldSortingPluginGetters['sortableFieldsByColumnName'] {
    const { columnNames, getFieldNames, sorting } = this.props;
    if (rows) {
      const sortableFieldsByColumnName = columnNames.reduce(
        (acc, columnName) => {
          const fields = R.uniq(
            R.unnest(rows.map((row: T) => getFieldNames(row, columnName)))
          );
          return {
            ...acc,
            [columnName]:
              fields.length === 0 && sorting ? [sorting.fieldName] : fields,
          };
        },
        {}
      );
      return sortableFieldsByColumnName;
    }
    return {};
  }

  @bind
  private changeColumnFieldSorting(sorting: ISorting | null) {
    this.props.onSortingChange(sorting);
  }
}
