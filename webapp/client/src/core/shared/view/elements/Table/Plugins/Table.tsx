import { Table } from '@devexpress/dx-react-grid-material-ui';

import withProps from 'core/shared/utils/react/withProps';

import TableCell from '../Templates/Cell/Cell';
import Row from '../Templates/Row/Row';

export default withProps(Table)({
  cellComponent: TableCell,
  rowComponent: Row,
});
