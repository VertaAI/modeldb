import * as React from 'react';

import {
  IDockerEnvironmentBlob,
  makeDockerImage,
} from 'shared/models/Versioning/Blob/EnvironmentBlob';
import { BlobDataBox } from 'shared/view/domain/Versioning/Blob/BlobBox/BlobBox';
import CopyButton from 'shared/view/elements/CopyButton/CopyButton';

import PropertiesTable from '../../shared/PropertiesTable/PropertiesTable';
import styles from './DockerEnvironmentBlobView.module.css';

interface ILocalProps {
  blob: IDockerEnvironmentBlob;
}

const DockerEnvironmentBlobView = ({ blob: { data } }: ILocalProps) => {
  return (
    <BlobDataBox title="Docker details">
      <PropertiesTable
        data={data}
        propDefinitions={[
          {
            title: 'Docker container',
            render: d => {
              const dockerImage = makeDockerImage(d);
              return (
                <div className={styles.dockerContainerValue}>
                  {dockerImage}&nbsp;
                  <CopyButton value={dockerImage} />
                </div>
              );
            },
          },
        ]}
      />
    </BlobDataBox>
  );
};

export default DockerEnvironmentBlobView;
