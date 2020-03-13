import { TableHeaderRow as MaterialTableHeaderRow } from '@devexpress/dx-react-grid-material-ui';
import * as React from 'react';

import withProps from 'core/shared/utils/react/withProps';

import HeaderCell from '../Templates/HeaderCell/HeaderCell';
import Row from '../Templates/Row/Row';

const Content = (props: any) => <div {...props} />;
const Title = (props: any) => <span {...props} />;

export default withProps(MaterialTableHeaderRow)({
  cellComponent: HeaderCell,
  rowComponent: Row,
  contentComponent: Content,
  titleComponent: Title,
});
