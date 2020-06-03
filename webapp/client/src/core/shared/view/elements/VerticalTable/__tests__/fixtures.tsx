import React from 'react';
import { IColumnData, IPropDefinition } from '../VerticalTable';

interface IData {
  property: string;
  number: number;
  timestamp: number;
}

export const columnsData: Array<IColumnData<IData>> = [
  {
    columnTitle: 'First column',
    data: {
      number: 1,
      property: 'test prop',
      timestamp: new Date().valueOf(),
    },
  },
  {
    columnTitle: 'Second column',
    data: {
      number: 5,
      property: 'second prop',
      timestamp: new Date().valueOf(),
    },
  },
];

export const propDefinitions: Array<IPropDefinition<IData>> = [
  {
    title: 'Property',
    render: ({ property }) => <span>{property}</span>,
    titleDataTest: 'prop-title',
    type: 'property',
  },
  {
    title: 'Number',
    render: ({ number }) => <span>{number}</span>,
    titleDataTest: 'prop-title',
    type: 'number',
  },
  {
    title: 'Date',
    render: ({ timestamp }) => (
      <span>{new Date(timestamp).toLocaleDateString()}</span>
    ),
    titleDataTest: 'prop-title',
    type: 'date',
  },
];
