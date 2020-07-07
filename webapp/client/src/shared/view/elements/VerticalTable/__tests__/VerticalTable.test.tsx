import { mount } from 'enzyme';
import React from 'react';

import { findByDataTestAttribute } from 'shared/utils/tests/react/helpers';

import VerticalTable from '../VerticalTable';
import { columnsData, propDefinitions } from './fixtures';
import {
  findDisplayedRows,
  findDisplayedRowByType,
  findDisplayedColumnsNames,
  findDisplayedRowTitles,
  findDisplayedCellsContents,
} from './helpers';

describe('(VerticalTable component)', () => {
  describe('render tests', () => {
    const component = mount(
      <VerticalTable
        columnsData={columnsData}
        propDefinitions={propDefinitions}
      />
    );

    it('should render columns and rows', () => {
      expect(findDisplayedRows(component).length).toEqual(
        propDefinitions.length
      );

      expect(findDisplayedColumnsNames(component).length).toEqual(
        columnsData.length
      );
    });

    it('should correct render column titles', () => {
      expect(findDisplayedColumnsNames(component)).toEqual(
        columnsData.map(c => c.columnTitle)
      );
    });

    it('should correct render prop titles', () => {
      expect(findDisplayedRowTitles('prop-title', component)).toEqual(
        propDefinitions.map(p => p.title)
      );
    });

    it('should correct render columns data', () => {
      expect(
        findDisplayedCellsContents(propDefinitions[0].type!, component)
      ).toEqual([columnsData[0].data.property, columnsData[1].data.property]);

      expect(
        findDisplayedCellsContents(propDefinitions[1].type!, component)
      ).toEqual([
        columnsData[0].data.number.toString(),
        columnsData[1].data.number.toString(),
      ]);

      expect(
        findDisplayedCellsContents(propDefinitions[2].type!, component)
      ).toEqual([
        new Date(columnsData[0].data.timestamp).toLocaleDateString(),
        new Date(columnsData[1].data.timestamp).toLocaleDateString(),
      ]);
    });
  });

  describe('data update tests', () => {
    it('should update columns in table', () => {
      const component = mount(
        <VerticalTable
          columnsData={columnsData}
          propDefinitions={propDefinitions}
        />
      );

      expect(findDisplayedColumnsNames(component).length).toEqual(
        columnsData.length
      );

      const newColumnsData = [
        ...columnsData,
        { ...columnsData[0], columnTitle: 'Third column' },
      ];

      component.setProps({ columnsData: newColumnsData });

      expect(
        expect(findDisplayedColumnsNames(component).length).toEqual(
          newColumnsData.length
        )
      );
    });

    it('should update rows in table', () => {
      const component = mount(
        <VerticalTable
          columnsData={columnsData}
          propDefinitions={propDefinitions}
        />
      );

      expect(findDisplayedRows(component).length).toEqual(
        propDefinitions.length
      );

      const newPropDefinitions: Array<typeof propDefinitions[number]> = [
        ...propDefinitions,
        {
          title: 'Timestamp',
          render: data => <span>{data.timestamp}</span>,
        },
      ];

      component.setProps({ propDefinitions: newPropDefinitions });

      expect(findDisplayedRows(component).length).toEqual(
        newPropDefinitions.length
      );
    });

    it('should update rows contents', () => {
      const component = mount(
        <VerticalTable
          columnsData={columnsData}
          propDefinitions={propDefinitions}
        />
      );

      expect(
        findDisplayedCellsContents(propDefinitions[0].type!, component)
      ).toEqual([columnsData[0].data.property, columnsData[1].data.property]);

      const newColumnsData = [columnsData[1], columnsData[0]];

      component.setProps({ columnsData: newColumnsData });

      expect(
        findDisplayedCellsContents(propDefinitions[0].type!, component)
      ).toEqual([
        newColumnsData[0].data.property,
        newColumnsData[1].data.property,
      ]);
    });
  });
});
