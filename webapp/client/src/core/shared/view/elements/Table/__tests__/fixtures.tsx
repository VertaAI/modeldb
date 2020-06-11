import React from 'react';

import { ColumnDefinition } from '../types';

export const dataRows = [
  {
    name: 'Test name',
    word: 'bbb',
    timestamp: 1589385458,
    number: 1,
  },
  {
    name: 'Second',
    word: 'ccc',
    timestamp: 1589385458,
    number: 2,
  },
  {
    name: 'Third',
    word: 'aaa',
    timestamp: 1589385458,
    number: 3,
  },
];

export const dataGroups = [dataRows, [dataRows[0], dataRows[2]]];

export const columnDefinitions: Array<
  ColumnDefinition<typeof dataRows[number]>
> = [
  {
    title: 'Name',
    type: 'name',
    width: '25%',
    render: row => <span>{row.name}</span>,
  },
  {
    title: 'Word',
    type: 'word',
    width: '25%',
    render: row => <span>{row.word}</span>,
  },
  {
    title: 'timestamp',
    type: 'timestamp',
    width: '25%',
    render: row => <span>{new Date(row.timestamp).toLocaleDateString()}</span>,
  },
  {
    title: 'Number',
    type: 'number',
    width: '25%',
    render: row => <span>{row.number}</span>,
  },
];

export const columnDefinitionsWithSort: Array<
  ColumnDefinition<typeof dataRows[number]>
> = [
  {
    title: 'Name',
    type: 'name',
    width: '25%',
    render: row => <span>{row.name}</span>,
    withSort: true,
    getValue: row => row.name,
  },
  {
    title: 'Word',
    type: 'word',
    width: '25%',
    render: row => <span>{row.word}</span>,
    withSort: true,
    getValue: row => row.word,
  },
  {
    title: 'timestamp',
    type: 'timestamp',
    width: '25%',
    render: row => <span>{new Date(row.timestamp).toLocaleDateString()}</span>,
    withSort: true,
    getValue: row => row.timestamp,
  },
  {
    title: 'Number',
    type: 'number',
    width: '25%',
    render: row => <span>{row.number}</span>,
    withSort: true,
    getValue: row => row.number,
  },
];

export const getRowKey = (row: typeof dataRows[number]) => row.name;

export const getGroupKey = (group: typeof dataGroups[number], index: number) =>
  index;
