import * as React from 'react';

import {
  IDockerEnvironmentBlob,
  makeDockerImage,
} from 'core/shared/models/Versioning/Blob/EnvironmentBlob';
import CopyButton from 'core/shared/view/elements/CopyButton/CopyButton';
import {
  PageHeader,
  RecordInfo,
} from 'core/shared/view/elements/PageComponents';

import styles from './DockerEnvironmentBlobView.module.css';

interface ILocalProps {
  blob: IDockerEnvironmentBlob;
}

const DockerEnvironmentBlobView = ({ blob: { data } }: ILocalProps) => {
  const dockerImage = makeDockerImage(data);
  return (
    <div className={styles.root}>
      <PageHeader title="Docker details" size="small" withoutSeparator={true} />
      <div className={styles.content}>
        <RecordInfo label="Docker container">
          <div className={styles.dockerContainerValue}>
            {dockerImage} <CopyButton value={dockerImage} />
          </div>
        </RecordInfo>
      </div>
    </div>
  );
};

export default DockerEnvironmentBlobView;
