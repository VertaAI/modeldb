import * as React from 'react';

import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  IHydratedCommit,
  IFullCommitComponentLocationComponents,
  ICommitComponent,
} from 'core/shared/models/Versioning/RepositoryData';
import matchBy from 'core/shared/utils/matchBy';

import BlobDetailsView from './BlobDetailsView/BlobDetailsView';
import styles from './DataNavigation.module.css';
import FolderView from './FolderView/FolderView';

interface ILocalProps {
  repository: IRepository;
  fullCommitComponentLocationComponents: IFullCommitComponentLocationComponents;
  commit: IHydratedCommit;
  component: ICommitComponent;
}

const DataNavigation = (props: ILocalProps) => {
  const {
    repository,
    commit,
    fullCommitComponentLocationComponents,
    component,
  } = props;

  return (
    <div className={styles.root}>
      {matchBy(component, 'type')({
        blob: blob => (
          <BlobDetailsView
            repository={repository}
            commit={commit}
            location={fullCommitComponentLocationComponents.location}
            blobData={blob.data}
          />
        ),
        folder: folder => (
          <FolderView
            data={folder}
            fullCommitComponentLocationComponents={
              fullCommitComponentLocationComponents
            }
            commit={commit}
            repositoryName={repository.name}
          />
        ),
      })}
    </div>
  );
};

export default DataNavigation;
