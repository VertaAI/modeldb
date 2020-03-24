import * as React from 'react';

import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  IHydratedCommit,
  IFullDataLocationComponents,
  IRepositoryData,
} from 'core/shared/models/Versioning/RepositoryData';
import matchBy from 'core/shared/utils/matchBy';

import styles from './DataNavigation.module.css';
import FolderView from './FolderView/FolderView';
import BlobDetailsView from './BlobDetailsView/BlobDetailsView';

interface ILocalProps {
  repository: IRepository;
  fullDataLocationComponents: IFullDataLocationComponents;
  commit: IHydratedCommit;
  data: IRepositoryData;
}

const DataNavigation = (props: ILocalProps) => {
  const { repository, commit, fullDataLocationComponents, data } = props;

  return (
    <div className={styles.root}>
      {matchBy(data, 'type')({
        blob: blob => (
          <BlobDetailsView
            repository={repository}
            commit={commit}
            location={fullDataLocationComponents.location}
            blobData={blob.data}
          />
        ),
        folder: folder => (
          <FolderView
            data={folder}
            fullDataLocationComponents={fullDataLocationComponents}
            commit={commit}
            repositoryName={repository.name}
          />
        ),
      })}
    </div>
  );
};

export default DataNavigation;
