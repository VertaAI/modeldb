import { ReactWrapper } from 'enzyme';

import { findByDataTestAttribute } from 'shared/utils/tests/react/helpers';

import { Row, Cell } from '../components';
import { Group } from '../GroupedTable/GroupedTable';
import { HeaderCell } from '../TableHeader';

export const findDisplayedRows = (component: ReactWrapper<any, any>) => {
  return component.find(Row);
};

export const findDisplayedColumnCells = (
  dataType: string,
  component: ReactWrapper<any, any>
) => {
  return component.find(`[data-type="${dataType}"]`);
};

export const findDisplayedColumnCellsContents = (
  dataType: string,
  component: ReactWrapper<any, any>
) => {
  return findDisplayedColumnCells(dataType, component).map(c => c.text());
};

export const findDisplayedHeaderCells = (component: ReactWrapper<any, any>) => {
  return component.find(HeaderCell);
};

export const findDisplayedCells = (component: ReactWrapper<any, any>) => {
  return component.find(Cell);
};

export const findDisplayedCellsContents = (
  component: ReactWrapper<any, any>
) => {
  return findDisplayedCells(component).map(c => c.text());
};

export const findDisplayedColumnsNames = (
  component: ReactWrapper<any, any>
) => {
  return findDisplayedHeaderCells(component).map(c => c.text());
};

export const findDisplayedGroups = (component: ReactWrapper<any, any>) => {
  return component.find(Group);
};

export const sortTable = (
  columnType: string,
  component: ReactWrapper<any, any>
) => {
  const columnHeaderCell = findByDataTestAttribute(
    `header-${columnType}`,
    component
  );
  const columnSortLabelIcon = findByDataTestAttribute(
    'sort-label-icon',
    columnHeaderCell
  );

  columnSortLabelIcon.simulate('click');
  component.update();
};
