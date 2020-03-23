import * as React from 'react';

import { IEnvironmentBlob } from 'core/shared/models/Versioning/Blob/EnvironmentBlob';
import matchBy from 'core/shared/utils/matchBy';
import AnsiView from 'core/shared/view/elements/AnsiView/AnsiView';
import CopyButton from 'core/shared/view/elements/CopyButton/CopyButton';
import {
  RecordInfo,
  PageHeader,
  PageContent,
} from 'core/shared/view/elements/PageComponents';

import DockerEnvironmentBlobView from './DockerEnvironmentBlobView/DockerEnvironmentBlobView';
import styles from './EnvironmentBlobView.module.css';
import KeyValuePairs from '../../../../../../../../shared/view/elements/KeyValuePairs/KeyValuePairs';
import PythonEnvironmentBlobView from './PythonEnvironmentBlobView/PythonEnvironmentBlobView';

interface ILocalProps {
  blob: IEnvironmentBlob;
}

const EnvironmentBlobView = ({ blob }: ILocalProps) => {
  return (
    <div className={styles.root}>
      <PageHeader title="Environment" size="medium" />
      <PageContent>
        <EnvironmentDetails
          commandLine={blob.data.commandLine}
          variables={blob.data.variables}
        />
        <div className={styles.detail}>
          {matchBy(blob.data.data, 'type')({
            python: pythonBlob => (
              <PythonEnvironmentBlobView blob={pythonBlob} />
            ),
            docker: dockerBlob => (
              <DockerEnvironmentBlobView blob={dockerBlob} />
            ),
          })}
        </div>
      </PageContent>
    </div>
  );
};

const EnvironmentDetails = ({
  commandLine,
  variables,
}: {
  commandLine: IEnvironmentBlob['data']['commandLine'];
  variables: IEnvironmentBlob['data']['variables'];
}) => {
  return commandLine || variables ? (
    <div className={styles.section}>
      <div className={styles.section__title}>
        <PageHeader
          title="Common details"
          size="small"
          withoutSeparator={true}
        />
      </div>
      <div className={styles.section__content}>
        {variables && (
          <RecordInfo label="Environment variables">
            <KeyValuePairs
              data={variables.map(({ name, value }) => ({ key: name, value }))}
            />
          </RecordInfo>
        )}
        {commandLine && (
          <RecordInfo label="Command line">
            <div className={styles.commandLineContent}>
              <div className={styles.commandLineView}>
                <AnsiView data={commandLine.join('\n')} />
              </div>
              <div className={styles.coppyCommandLine}>
                <CopyButton value={commandLine.join('\n')} />
              </div>
            </div>
          </RecordInfo>
        )}
      </div>
    </div>
  ) : null;
};

export default EnvironmentBlobView;
