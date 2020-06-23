import * as R from 'ramda';
import React from 'react';

import * as RouteHelpers from 'features/versioning/repositoryData/view/RepositoryData/routeHelpers';
import { IRepository } from 'shared/models/Versioning/Repository';
import {
  IFolder,
  IFolderElement,
  IHydratedCommit,
  IFullCommitComponentLocationComponents,
  emptyFolder,
} from 'shared/models/Versioning/RepositoryData';
import { DataBox } from 'shared/view/domain/Versioning/Blob/BlobBox/BlobBox';
import Placeholder from 'shared/view/elements/Placeholder/Placeholder';

import CurrentCommitInfo from './CurrentCommitInfo/CurrentCommitInfo';
import FolderElement from './FolderElement/FolderElement';
import styles from './FolderView.module.css';
import NavigationItem from './NavigationItem/NavigationItem';
import { IWorkspace } from 'shared/models/Workspace';

interface ILocalProps {
  fullCommitComponentLocationComponents: IFullCommitComponentLocationComponents;
  repositoryName: IRepository['name'];
  commit: IHydratedCommit;
  data: IFolder;
}

const FolderView: React.FC<ILocalProps> = ({
  commit,
  fullCommitComponentLocationComponents,
  repositoryName,
  data,
}: ILocalProps) => {
  const { blobs, subFolders } = data;
  const orderedFolderElements: IFolderElement[] = [
    ...R.sortBy(({ name }) => name, subFolders),
    ...R.sortBy(({ name }) => name, blobs),
  ];

  return (
    <DataBox withPadding={false}>
      <div className={styles.root}>
        <div className={styles.titles}>
          <div className={styles.title}>Content</div>
          <div className={styles.title}>Change description</div>
          <div className={styles.title}>Time of change</div>
        </div>
        <CurrentCommitInfo repositoryName={repositoryName} data={commit} />
        {R.equals(data, emptyFolder) ? (
          <div className={styles.placeholder}>
            <Placeholder>Empty folder</Placeholder>
          </div>
        ) : (
          orderedFolderElements.map(folderElement => (
            <FolderElement
              repositoryName={repositoryName}
              fullCommitComponentLocationComponents={
                fullCommitComponentLocationComponents
              }
              data={folderElement}
              key={folderElement.name}
            />
          ))
        )}
      </div>
    </DataBox>
  );
};

const UpTree = React.memo(
  ({
    fullCommitComponentLocationComponents,
    repositoryName,
    currentWorkspaceName,
  }: {
    fullCommitComponentLocationComponents: IFullCommitComponentLocationComponents;
    repositoryName: IRepository['name'];
    currentWorkspaceName: IWorkspace['name'];
  }) => (
    <NavigationItem
      to={RouteHelpers.goBack({
        ...fullCommitComponentLocationComponents,
        repositoryName,
        workspaceName: currentWorkspaceName,
      })}
      name=".."
    />
  )
);

export default FolderView;
