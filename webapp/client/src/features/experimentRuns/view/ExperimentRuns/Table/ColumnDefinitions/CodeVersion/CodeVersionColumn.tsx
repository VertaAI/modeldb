import * as React from 'react';

import { IRow } from '../types';
import CodeVersions from 'shared/view/domain/ModelRecord/ModelRecordProps/CodeVersions/CodeVersions';

interface ILocalProps {
  row: IRow;
}

type AllProps = ILocalProps;

class CodeVersionColumn extends React.PureComponent<AllProps> {
  public render() {
    const {
      experimentRun: { id, codeVersion, codeVersionsFromBlob, versionedInputs },
    } = this.props.row;
    return codeVersion || codeVersionsFromBlob ? (
      <CodeVersions
        versionedInputs={versionedInputs}
        experimentRunId={id}
        codeVersion={codeVersion}
        codeVersionsFromBlob={codeVersionsFromBlob}
      />
    ) : null;
  }
}

export default CodeVersionColumn;
