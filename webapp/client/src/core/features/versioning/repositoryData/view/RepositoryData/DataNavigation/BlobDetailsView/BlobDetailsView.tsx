import * as React from 'react';

import { IBlob } from 'core/shared/models/Versioning/Blob/Blob';
import { IHydratedCommit } from 'core/shared/models/Versioning/RepositoryData';
import * as DataLocation from 'core/shared/models/Versioning/DataLocation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import { DataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';

import BlobView from './BlobView/BlobView';
import styles from './BlobDetailsView.module.css';
import ExperimentRuns from './ExperimentRuns/ExperimentRuns';

interface ILocalProps {
  repository: IRepository;
  commit: IHydratedCommit;
  blobData: IBlob['data'];
  location: DataLocation.DataLocation;
}

const BlobDetailsView = (props: ILocalProps) => {
  return (
    <DataBox withPadding={true}>
      <div className={styles.blob}>
        <BlobView blobData={props.blobData} />
      </div>
      <div className={styles.experimentRuns}>
        <ExperimentRuns
          repositoryId={props.repository.id}
          commitSha={props.commit.sha}
          location={props.location}
        />
      </div>
    </DataBox>
  );
};

export default BlobDetailsView;
