import * as React from 'react';

import { Table, TableProps } from '@devexpress/dx-react-grid-material-ui';

import TableCell from '../Templates/Cell/Cell';
import Row from '../Templates/Row/Row';

const Table_ = ({
  cellComponent = TableCell,
  rowComponent = Row,
  ...restProps
}: TableProps) => {
  return (
    <Table
      {...restProps}
      cellComponent={cellComponent}
      rowComponent={rowComponent}
    />
  );
};

export default Table_;