import * as React from 'react';

import { IEnvironmentBlob } from 'shared/models/Versioning/Blob/EnvironmentBlob';
import matchBy from 'shared/utils/matchBy';
import AnsiView from 'shared/view/elements/AnsiView/AnsiView';
import CopyButton from 'shared/view/elements/CopyButton/CopyButton';
import KeyValuePairs from 'shared/view/elements/KeyValuePairs/KeyValuePairs';

import {
  MultipleBlobDataBox,
  BlobDataBox,
} from 'shared/view/domain/Versioning/Blob/BlobBox/BlobBox';
import PropertiesTable from '../shared/PropertiesTable/PropertiesTable';
import DockerEnvironmentBlobView from './DockerEnvironmentBlobView/DockerEnvironmentBlobView';
import styles from './EnvironmentBlobView.module.css';
import PythonEnvironmentBlobView from './PythonEnvironmentBlobView/PythonEnvironmentBlobView';

interface ILocalProps {
  blob: IEnvironmentBlob;
}

const EnvironmentBlobView = ({ blob }: ILocalProps) => {
  return (
    <MultipleBlobDataBox title="Environment">
      <EnvironmentDetails
        commandLine={blob.data.commandLine}
        variables={blob.data.variables}
      />
      {matchBy(blob.data.data, 'type')({
        python: pythonBlob => <PythonEnvironmentBlobView blob={pythonBlob} />,
        docker: dockerBlob => <DockerEnvironmentBlobView blob={dockerBlob} />,
      })}
    </MultipleBlobDataBox>
  );
};

const EnvironmentDetails = (
  data: Pick<IEnvironmentBlob['data'], 'commandLine' | 'variables'>
) => {
  return data.commandLine || data.variables ? (
    <BlobDataBox title="Common details">
      <PropertiesTable
        data={data}
        propDefinitions={[
          {
            title: 'Environment variables',
            render: ({ variables }) =>
              variables ? (
                <KeyValuePairs
                  data={variables.map(({ name, value }) => ({
                    key: name,
                    value,
                  }))}
                />
              ) : null,
          },
          {
            title: 'Command line',
            render: ({ commandLine }) =>
              commandLine ? (
                <div className={styles.commandLineContent}>
                  <div className={styles.commandLineView}>
                    <AnsiView data={commandLine.join('\n')} />
                  </div>
                  <div className={styles.coppyCommandLine}>
                    <CopyButton value={commandLine.join('\n')} />
                  </div>
                </div>
              ) : null,
          },
        ]}
      />
    </BlobDataBox>
  ) : null;
};

export default EnvironmentBlobView;
