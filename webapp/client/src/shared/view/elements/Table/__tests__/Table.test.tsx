import { mount } from 'enzyme';
import React from 'react';

import { findByDataTestAttribute } from 'shared/utils/tests/react/helpers';

import GrouppedTable from '../GroupedTable/GroupedTable';
import Table from '../Table';
import {
  dataGroups,
  columnDefinitions,
  dataRows,
  columnDefinitionsWithSort,
  getRowKey,
  getGroupKey,
} from './fixtures';
import {
  findDisplayedColumnsNames,
  findDisplayedRows,
  findDisplayedCellsContents,
  findDisplayedGroups,
  findDisplayedColumnCellsContents,
  sortTable,
} from './helpers';

describe('(GrouppedTable component)', () => {
  describe('render tests', () => {
    const component = mount(
      <GrouppedTable
        dataGroups={dataGroups}
        columnDefinitions={columnDefinitions}
        getRowKey={getRowKey}
        getGroupKey={getGroupKey}
      />
    );

    it('should render groups', () => {
      expect(findDisplayedGroups(component).length).toEqual(dataGroups.length);
    });

    it('should render rows in groups', () => {
      expect(
        findDisplayedRows(findDisplayedGroups(component).first()).length
      ).toEqual(dataGroups[0].length);

      expect(
        findDisplayedRows(findDisplayedGroups(component).at(1)).length
      ).toEqual(dataGroups[1].length);
    });
  });

  describe('update data tests', () => {
    it('should update groups', () => {
      const component = mount(
        <GrouppedTable
          dataGroups={dataGroups}
          columnDefinitions={columnDefinitions}
          getRowKey={getRowKey}
          getGroupKey={getGroupKey}
        />
      );

      expect(findDisplayedGroups(component).length).toEqual(dataGroups.length);

      const newDataGroups = [...dataGroups, dataGroups[1]];

      component.setProps({ dataGroups: newDataGroups });

      expect(findDisplayedGroups(component).length).toEqual(
        newDataGroups.length
      );
    });

    it('should update rows in groups', () => {
      const component = mount(
        <GrouppedTable
          dataGroups={dataGroups}
          columnDefinitions={columnDefinitions}
          getRowKey={getRowKey}
          getGroupKey={getGroupKey}
        />
      );

      expect(
        findDisplayedRows(findDisplayedGroups(component).first()).length
      ).toEqual(dataGroups[0].length);

      const newDataGroups = [[...dataRows, ...dataRows], dataGroups[1]];

      component.setProps({ dataGroups: newDataGroups });

      expect(
        findDisplayedRows(findDisplayedGroups(component).first()).length
      ).toEqual(newDataGroups[0].length);
    });
  });

  describe('test no data cases', () => {
    it('should display "no data" if no data', () => {
      const component = mount(
        <GrouppedTable
          dataGroups={[]}
          columnDefinitions={columnDefinitionsWithSort}
          getRowKey={getRowKey}
          getGroupKey={getGroupKey}
        />
      );

      expect(findByDataTestAttribute('no-data', component).length).toEqual(1);
    });

    it('should render "no data" from props if no data', () => {
      const component = mount(
        <GrouppedTable
          dataGroups={[]}
          columnDefinitions={columnDefinitionsWithSort}
          noData={() => <span data-test="no-data-cell" />}
          getRowKey={getRowKey}
          getGroupKey={getGroupKey}
        />
      );

      expect(findByDataTestAttribute('no-data-cell', component).length).toEqual(
        1
      );
    });
  });
});

describe('(Table component)', () => {
  describe('render tests', () => {
    const component = mount(
      <Table
        dataRows={dataRows}
        getRowKey={getRowKey}
        columnDefinitions={columnDefinitions}
      />
    );

    it('should render rows and columns', () => {
      expect(findDisplayedColumnsNames(component).length).toEqual(
        columnDefinitions.length
      );

      expect(findDisplayedRows(component).length).toEqual(dataRows.length);
    });

    it('should correct render columns names', () => {
      expect(findDisplayedColumnsNames(component)).toEqual([
        columnDefinitions[0].title,
        columnDefinitions[1].title,
        columnDefinitions[2].title,
        columnDefinitions[3].title,
      ]);
    });

    it('should correct render rows content', () => {
      expect(
        findDisplayedCellsContents(findDisplayedRows(component).first())
      ).toEqual([
        dataRows[0].name,
        dataRows[0].word,
        new Date(dataRows[0].timestamp).toLocaleDateString(),
        dataRows[0].number.toString(),
      ]);
    });
  });

  describe('update data tests', () => {
    it('should update rows', () => {
      const component = mount(
        <Table
          dataRows={dataRows}
          getRowKey={getRowKey}
          columnDefinitions={columnDefinitions}
        />
      );

      expect(findDisplayedRows(component).length).toEqual(dataRows.length);

      const newDataRows = [
        dataRows[0],
        dataRows[1],
        dataRows[0],
        dataRows[1],
        dataRows[2],
      ];

      component.setProps({ dataRows: newDataRows });

      expect(findDisplayedRows(component).length).toEqual(newDataRows.length);
    });

    it('should update rows content', () => {
      const component = mount(
        <Table
          dataRows={dataRows}
          getRowKey={getRowKey}
          columnDefinitions={columnDefinitions}
        />
      );

      expect(
        findDisplayedCellsContents(findDisplayedRows(component).first())
      ).toEqual([
        dataRows[0].name,
        dataRows[0].word,
        new Date(dataRows[0].timestamp).toLocaleDateString(),
        dataRows[0].number.toString(),
      ]);

      const newDataRows = [dataRows[2], dataRows[1], dataRows[0]];

      component.setProps({ dataRows: newDataRows });

      expect(
        findDisplayedCellsContents(findDisplayedRows(component).first())
      ).toEqual([
        newDataRows[0].name,
        newDataRows[0].word,
        new Date(newDataRows[0].timestamp).toLocaleDateString(),
        newDataRows[0].number.toString(),
      ]);
    });

    it('should update columns', () => {
      const component = mount(
        <Table
          dataRows={dataRows}
          getRowKey={getRowKey}
          columnDefinitions={columnDefinitions}
        />
      );

      expect(
        findDisplayedCellsContents(findDisplayedRows(component).first())
      ).toEqual([
        dataRows[0].name,
        dataRows[0].word,
        new Date(dataRows[0].timestamp).toLocaleDateString(),
        dataRows[0].number.toString(),
      ]);

      expect(findDisplayedColumnsNames(component)).toEqual(
        columnDefinitions.map(column => column.title)
      );

      const newColumnDefinitions = [columnDefinitions[2], columnDefinitions[1]];

      component.setProps({ columnDefinitions: newColumnDefinitions });

      expect(
        findDisplayedCellsContents(findDisplayedRows(component).first())
      ).toEqual([
        new Date(dataRows[0].timestamp).toLocaleDateString(),
        dataRows[0].word,
      ]);

      expect(findDisplayedColumnsNames(component)).toEqual([
        newColumnDefinitions[0].title,
        newColumnDefinitions[1].title,
      ]);
    });
  });

  describe('selection tests', () => {
    it('should render selection components from props', () => {
      const component = mount(
        <Table
          dataRows={dataRows}
          columnDefinitions={columnDefinitions}
          getRowKey={getRowKey}
          selection={{
            cellComponent: () => <div data-test="selection-cell" />,
            headerCellComponent: () => (
              <div data-test="selection-header-cell" />
            ),
            showSelectAll: true,
            showSelectionColumn: true,
          }}
        />
      );

      expect(
        findByDataTestAttribute('selection-cell', component).length
      ).toEqual(dataRows.length);

      expect(
        findByDataTestAttribute('selection-header-cell', component).length
      ).toEqual(1);
    });

    it('should not render selection components if props flags is false', () => {
      const component = mount(
        <Table
          dataRows={dataRows}
          columnDefinitions={columnDefinitions}
          getRowKey={getRowKey}
          selection={{
            cellComponent: () => <div data-test="selection-cell" />,
            headerCellComponent: () => (
              <div data-test="selection-header-cell" />
            ),
            showSelectAll: false,
            showSelectionColumn: false,
          }}
        />
      );

      expect(
        findByDataTestAttribute('selection-cell', component).length
      ).toEqual(0);

      expect(
        findByDataTestAttribute('selection-header-cell', component).length
      ).toEqual(0);
    });

    it('should give a correct row to cellComponent arguments', () => {
      const component = mount(
        <Table
          dataRows={dataRows}
          columnDefinitions={columnDefinitions}
          getRowKey={getRowKey}
          selection={{
            cellComponent: row => (
              <div data-test="selection-cell">{row.name}</div>
            ),
            headerCellComponent: () => (
              <div data-test="selection-header-cell" />
            ),
            showSelectAll: true,
            showSelectionColumn: true,
          }}
        />
      );

      const displayedNamesInCells = findByDataTestAttribute(
        'selection-cell',
        component
      ).map(c => c.text());
      const rowsNames = dataRows.map(row => row.name);

      expect(displayedNamesInCells).toEqual(rowsNames);
    });
  });

  describe('sorting tests', () => {
    it('should not render sort labels if columnDefinitions without selection', () => {
      const component = mount(
        <Table
          dataRows={dataRows}
          columnDefinitions={columnDefinitions}
          getRowKey={getRowKey}
        />
      );

      expect(findByDataTestAttribute('sort-label', component).length).toEqual(
        0
      );
    });

    it('should render sort labels', () => {
      const component = mount(
        <Table
          dataRows={dataRows}
          columnDefinitions={columnDefinitionsWithSort}
          getRowKey={getRowKey}
        />
      );

      expect(findByDataTestAttribute('sort-label', component).length).toEqual(
        columnDefinitionsWithSort.length
      );
    });

    it('should sort column on sort label click (numbers)', () => {
      const component = mount(
        <Table
          dataRows={dataRows}
          columnDefinitions={columnDefinitionsWithSort}
          getRowKey={getRowKey}
        />
      );

      const column = columnDefinitionsWithSort[3];

      sortTable(column.type, component);

      const sortedNumbers = dataRows
        .map(row => row.number)
        .sort()
        .map(content => content.toString())
        .reverse();

      expect(findDisplayedColumnCellsContents(column.type, component)).toEqual(
        sortedNumbers
      );
    });

    it('should sort column on sort label click (strings)', () => {
      const component = mount(
        <Table
          dataRows={dataRows}
          columnDefinitions={columnDefinitionsWithSort}
          getRowKey={getRowKey}
        />
      );

      const column = columnDefinitionsWithSort[1];

      sortTable(column.type, component);

      const sortedWords = dataRows
        .map(row => row.word)
        .sort()
        .map(content => content.toString())
        .reverse();

      expect(findDisplayedColumnCellsContents(column.type, component)).toEqual(
        sortedWords
      );
    });

    it('should reverse sort column on sort label double click', () => {
      const component = mount(
        <Table
          dataRows={dataRows}
          columnDefinitions={columnDefinitionsWithSort}
          getRowKey={getRowKey}
        />
      );

      const column = columnDefinitionsWithSort[3];

      sortTable(column.type, component);
      sortTable(column.type, component);

      const sortedNumbers = dataRows
        .map(row => row.number)
        .sort()
        .map(content => content.toString());

      expect(findDisplayedColumnCellsContents(column.type, component)).toEqual(
        sortedNumbers
      );
    });

    it('should change sorted column on another sort label click', () => {
      const component = mount(
        <Table
          dataRows={dataRows}
          columnDefinitions={columnDefinitionsWithSort}
          getRowKey={getRowKey}
        />
      );

      const column = columnDefinitionsWithSort[3];

      sortTable(column.type, component);

      const sortedNumbers = dataRows
        .map(row => row.number)
        .sort()
        .map(content => content.toString())
        .reverse();

      expect(findDisplayedColumnCellsContents(column.type, component)).toEqual(
        sortedNumbers
      );

      const wordsColumn = columnDefinitionsWithSort[1];

      sortTable(wordsColumn.type, component);

      const sortedWords = dataRows
        .map(row => row.word)
        .sort()
        .map(content => content.toString())
        .reverse();

      expect(
        findDisplayedColumnCellsContents(wordsColumn.type, component)
      ).toEqual(sortedWords);
    });
  });

  describe('test no data cases', () => {
    it('should display "no data" if no data', () => {
      const component = mount(
        <Table
          dataRows={[]}
          columnDefinitions={columnDefinitionsWithSort}
          getRowKey={getRowKey}
        />
      );

      expect(findByDataTestAttribute('no-data', component).length).toEqual(1);
    });

    it('should render "no data" from props if no data', () => {
      const component = mount(
        <Table
          dataRows={[]}
          columnDefinitions={columnDefinitionsWithSort}
          getRowKey={getRowKey}
          noData={() => <span data-test="no-data-cell" />}
        />
      );

      expect(findByDataTestAttribute('no-data-cell', component).length).toEqual(
        1
      );
    });

    it('should not render sort labels if no data', () => {
      const component = mount(
        <Table
          dataRows={[]}
          columnDefinitions={columnDefinitionsWithSort}
          getRowKey={getRowKey}
          noData={() => <span data-test="no-data-cell" />}
        />
      );

      expect(findByDataTestAttribute('sort-label', component).length).toEqual(
        0
      );
    });
  });
});
