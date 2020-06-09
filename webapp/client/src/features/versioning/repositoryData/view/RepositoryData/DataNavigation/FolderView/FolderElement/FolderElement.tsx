import * as React from 'react';
import { useSelector } from 'react-redux';

import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  IFolderElement,
  IFullCommitComponentLocationComponents,
} from 'core/shared/models/Versioning/RepositoryData';
import matchType from 'core/shared/utils/matchType';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';

import * as RouteHeplers from '../../../routeHelpers';
import NavigationItem from '../NavigationItem/NavigationItem';

interface ILocalProps {
  repositoryName: IRepository['name'];
  fullCommitComponentLocationComponents: IFullCommitComponentLocationComponents;
  data: IFolderElement;
}

const FolderElement = ({
  data,
  repositoryName,
  fullCommitComponentLocationComponents,
}: ILocalProps) => {
  const currentWorkspaceName = useSelector(selectCurrentWorkspaceName);
  return (
    <NavigationItem
      name={data.name}
      iconType={matchType(
        { folder: () => 'folder', blob: () => 'file' },
        data.type
      )}
      to={RouteHeplers.addName(data.name, data.type, {
        ...fullCommitComponentLocationComponents,
        workspaceName: currentWorkspaceName,
        repositoryName,
      })}
    />
  );
};

export default FolderElement;
