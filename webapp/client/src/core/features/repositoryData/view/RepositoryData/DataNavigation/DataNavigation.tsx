import * as React from 'react';

import { IRepository } from 'core/shared/models/Repository/Repository';
import {
  IHydratedCommit,
  IFullDataLocationComponents,
  IRepositoryData,
} from 'core/shared/models/Repository/RepositoryData';
import matchBy from 'core/shared/utils/matchBy';

import BlobView from './BlobView/BlobView';
import styles from './DataNavigation.module.css';
import FolderView from './FolderView/FolderView';

interface ILocalProps {
  repositoryName: IRepository['name'];
  fullDataLocationComponents: IFullDataLocationComponents;
  commit: IHydratedCommit;
  data: IRepositoryData;
}

const DataNavigation = (props: ILocalProps) => {
  const { repositoryName, commit, fullDataLocationComponents, data } = props;

  return (
    <div className={styles.root}>
      {matchBy(data, 'type')({
        blob: blob => <BlobView blobData={blob.data} />,
        folder: folder => (
          <FolderView
            data={folder}
            fullDataLocationComponents={fullDataLocationComponents}
            commit={commit}
            repositoryName={repositoryName}
          />
        ),
      })}
    </div>
  );
};

export default DataNavigation;
