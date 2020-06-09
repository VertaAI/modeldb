import * as React from 'react';

import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  IHydratedCommit,
  IFullCommitComponentLocationComponents,
} from 'core/shared/models/Versioning/RepositoryData';
import matchBy from 'core/shared/utils/matchBy';

import BlobDetailsView from './BlobDetailsView/BlobDetailsView';
import styles from './DataNavigation.module.css';
import FolderView from './FolderView/FolderView';
import { ICommitComponentView } from '../../../store/types';

interface ILocalProps {
  repository: IRepository;
  fullCommitComponentLocationComponents: IFullCommitComponentLocationComponents;
  commit: IHydratedCommit;
  component: ICommitComponentView;
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
            blob={blob}
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
