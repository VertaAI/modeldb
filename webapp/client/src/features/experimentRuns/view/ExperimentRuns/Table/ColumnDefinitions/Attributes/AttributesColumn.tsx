import * as React from 'react';

import Attributes from 'core/shared/view/domain/ModelRecord/ModelRecordProps/Attributes/Attributes/Attributes';

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

export default AttributesColumn;
