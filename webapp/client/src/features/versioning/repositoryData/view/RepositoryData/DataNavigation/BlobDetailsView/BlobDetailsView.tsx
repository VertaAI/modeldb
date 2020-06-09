import * as React from 'react';
import { connect } from 'react-redux';

import { IHydratedCommit } from 'shared/models/Versioning/RepositoryData';
import * as CommitComponentLocation from 'shared/models/Versioning/CommitComponentLocation';
import { IRepository } from 'shared/models/Versioning/Repository';
import { DataBox } from 'shared/view/domain/Versioning/Blob/BlobBox/BlobBox';
import { IBlobView } from 'features/versioning/repositoryData/store/types';
import AssociatedExperimentRuns from 'shared/view/domain/Versioning/AssociatedExperimentRuns/AssociatedExperimentRuns';
import { IApplicationState } from 'store/store';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';

import BlobView from './BlobView/BlobView';
import styles from './BlobDetailsView.module.css';

interface ILocalProps {
  repository: IRepository;
  commit: IHydratedCommit;
  blob: IBlobView;
  location: CommitComponentLocation.CommitComponentLocation;
}

const mapStateToProps = (state: IApplicationState) => {
  return {
    workspaceName: selectCurrentWorkspaceName(state),
  };
};

type AllProps = ILocalProps & ReturnType<typeof mapStateToProps>;

const BlobDetailsView = (props: AllProps) => {
  return (
    <DataBox withPadding={true}>
      <div className={styles.blob}>
        <BlobView blobData={props.blob.data} />
      </div>
      <div className={styles.experimentRuns}>
        <AssociatedExperimentRuns
          data={props.blob.experimentRuns}
          workspaceName={props.workspaceName}
        />
      </div>
    </DataBox>
  );
};

export default connect(mapStateToProps)(BlobDetailsView);
