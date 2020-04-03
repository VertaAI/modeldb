import * as R from 'ramda';
import React from 'react';

import * as RouteHelpers from 'core/features/versioning/repositoryData/view/RepositoryData/routeHelpers';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  IFolder,
  IFolderElement,
  IHydratedCommit,
  IFullDataLocationComponents,
  emptyFolder,
} from 'core/shared/models/Versioning/RepositoryData';
import Placeholder from 'core/shared/view/elements/Placeholder/Placeholder';
import { DataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';

import CurrentCommitInfo from './CurrentCommitInfo/CurrentCommitInfo';
import FolderElement from './FolderElement/FolderElement';
import NavigationItem from './NavigationItem/NavigationItem';
import styles from './FolderView.module.css';

interface ILocalProps {
  fullDataLocationComponents: IFullDataLocationComponents;
  repositoryName: IRepository['name'];
  commit: IHydratedCommit;
  data: IFolder;
}

const FolderView: React.FC<ILocalProps> = ({
  commit,
  fullDataLocationComponents,
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
        <CurrentCommitInfo repositoryName={repositoryName} data={commit} />
        {R.equals(data, emptyFolder) ? (
          <div className={styles.placeholder}>
            <Placeholder>Empty folder</Placeholder>
          </div>
        ) : (
          orderedFolderElements.map(folderElement => (
            <FolderElement
              repositoryName={repositoryName}
              fullDataLocationComponents={fullDataLocationComponents}
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
    fullDataLocationComponents,
    repositoryName,
  }: {
    fullDataLocationComponents: IFullDataLocationComponents;
    repositoryName: IRepository['name'];
  }) => (
    <NavigationItem
      to={RouteHelpers.goBack({
        ...fullDataLocationComponents,
        repositoryName,
      })}
      name=".."
    />
  )
);

export default FolderView;
