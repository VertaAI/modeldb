import { ReactWrapper } from 'enzyme';

import { findByDataTestAttribute } from 'shared/utils/tests/react/helpers';

import { Prop } from '../VerticalTable';

export const findDisplayedRows = (component: ReactWrapper) => {
  return component.find(Prop);
};

export const findDisplayedColumnsNames = (component: ReactWrapper) => {
  return findByDataTestAttribute('header-cell', component).map(c => c.text());
};

export const findDisplayedRowByType = (
  type: string,
  component: ReactWrapper
) => {
  return findByDataTestAttribute(`prop-${type}`, component);
};

export const findDisplayedRowTitles = (
  titleDataTest: string,
  component: ReactWrapper
) => {
  return findByDataTestAttribute(titleDataTest, component).map(c => c.text());
};

export const findDisplayedCells = (
  propType: string,
  component: ReactWrapper
) => {
  return component.find(`[data-type="${propType}"]`);
};

export const findDisplayedCellsContents = (
  propType: string,
  component: ReactWrapper
) => {
  return findDisplayedCells(propType, component).map(c => c.text());
};
