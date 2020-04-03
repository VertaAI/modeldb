import * as React from 'react';

import { IEnvironmentBlob } from 'core/shared/models/Versioning/Blob/EnvironmentBlob';
import matchBy from 'core/shared/utils/matchBy';
import AnsiView from 'core/shared/view/elements/AnsiView/AnsiView';
import CopyButton from 'core/shared/view/elements/CopyButton/CopyButton';
import KeyValuePairs from 'core/shared/view/elements/KeyValuePairs/KeyValuePairs';

import DockerEnvironmentBlobView from './DockerEnvironmentBlobView/DockerEnvironmentBlobView';
import styles from './EnvironmentBlobView.module.css';
import PythonEnvironmentBlobView from './PythonEnvironmentBlobView/PythonEnvironmentBlobView';
import makePropertiesTableComponents from 'core/shared/view/domain/Versioning/Blob/PropertiesTable/PropertiesTable';
import {
  MultipleBlobDataBox,
  BlobDataBox,
} from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';

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

const tableComponents = makePropertiesTableComponents<
  Pick<IEnvironmentBlob['data'], 'commandLine' | 'variables'>
>();

const EnvironmentDetails = (
  data: Pick<IEnvironmentBlob['data'], 'commandLine' | 'variables'>
) => {
  return data.commandLine || data.variables ? (
    <BlobDataBox title="Common details">
      <tableComponents.Table data={data}>
        <tableComponents.PropDefinition
          title="Environment variables"
          render={({ data: { variables } }) =>
            variables && (
              <KeyValuePairs
                data={variables.map(({ name, value }) => ({
                  key: name,
                  value,
                }))}
              />
            )
          }
        />
        <tableComponents.PropDefinition
          title="Command line"
          render={({ data: { commandLine } }) =>
            commandLine && (
              <div className={styles.commandLineContent}>
                <div className={styles.commandLineView}>
                  <AnsiView data={commandLine.join('\n')} />
                </div>
                <div className={styles.coppyCommandLine}>
                  <CopyButton value={commandLine.join('\n')} />
                </div>
              </div>
            )
          }
        />
      </tableComponents.Table>
    </BlobDataBox>
  ) : null;
};

export default EnvironmentBlobView;
