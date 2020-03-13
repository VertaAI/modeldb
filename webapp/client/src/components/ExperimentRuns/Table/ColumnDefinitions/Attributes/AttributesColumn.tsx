import {
  DataTypeProvider,
  DataTypeProviderProps,
} from '@devexpress/dx-react-grid';
import * as React from 'react';

import Attributes from 'components/ModelRecordProps/Attributes/Attributes/Attributes';

import { IRow } from '../types';

interface ILocalProps {
  row: IRow;
}

type AllProps = ILocalProps;

class AttributesColumn extends React.PureComponent<AllProps> {
  public render() {
    const {
      row: {
        experimentRun: { attributes },
      },
    } = this.props;
    return <Attributes attributes={attributes} />;
  }
}

const AttributesTypeProvider = (props: DataTypeProviderProps) => (
  <DataTypeProvider formatterComponent={AttributesColumn as any} {...props} />
);

export default AttributesTypeProvider;
