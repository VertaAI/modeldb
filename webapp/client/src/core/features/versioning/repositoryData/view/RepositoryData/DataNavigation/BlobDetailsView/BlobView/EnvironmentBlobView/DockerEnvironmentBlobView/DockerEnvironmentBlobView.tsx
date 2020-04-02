import * as React from 'react';

import {
  IDockerEnvironmentBlob,
  makeDockerImage,
} from 'core/shared/models/Versioning/Blob/EnvironmentBlob';
import CopyButton from 'core/shared/view/elements/CopyButton/CopyButton';
import makePropertiesTableComponents from 'core/shared/view/domain/Versioning/Blob/PropertiesTable/PropertiesTable';
import { BlobDataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';

import styles from './DockerEnvironmentBlobView.module.css';

interface ILocalProps {
  blob: IDockerEnvironmentBlob;
}

const tableComponents = makePropertiesTableComponents<
  IDockerEnvironmentBlob['data']
>();

const DockerEnvironmentBlobView = ({ blob: { data } }: ILocalProps) => {
  return (
    <BlobDataBox title="Docker details">
      <tableComponents.Table data={data}>
        <tableComponents.PropDefinition
          title="Docker container"
          render={({ data }) => {
            const dockerImage = makeDockerImage(data);
            return (
              <div className={styles.dockerContainerValue}>
                {dockerImage}&nbsp;
                <CopyButton value={dockerImage} />
              </div>
            );
          }}
        />
      </tableComponents.Table>
    </BlobDataBox>
  );
};

export default DockerEnvironmentBlobView;
