import {
  DataTypeProvider,
  DataTypeProviderProps,
} from '@devexpress/dx-react-grid';
import * as React from 'react';

import CodeVersion from 'components/CodeVersion/CodeVersion';

import { IRow } from '../types';

interface ILocalProps {
  row: IRow;
}

type AllProps = ILocalProps;

class CodeVersionColumn extends React.PureComponent<AllProps> {
  public render() {
    const {
      experimentRun: { id, codeVersion },
    } = this.props.row;
    return codeVersion ? (
      <CodeVersion
        entityType="experimentRun"
        codeVersion={codeVersion}
        entityId={id}
      />
    ) : null;
  }
}

const CodeVersionTypeProvider = (props: DataTypeProviderProps) => (
  <DataTypeProvider formatterComponent={CodeVersionColumn as any} {...props} />
);

export default CodeVersionTypeProvider;
