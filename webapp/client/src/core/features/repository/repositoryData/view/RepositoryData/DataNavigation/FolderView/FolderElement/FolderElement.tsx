import * as React from 'react';

import { IRepository } from 'core/shared/models/Repository/Repository';
import {
  IFolderElement,
  IFullDataLocationComponents,
} from 'core/shared/models/Repository/RepositoryData';
import matchType from 'core/shared/utils/matchType';

import * as RouteHeplers from '../../../routeHelpers';

import NavigationItem from '../NavigationItem/NavigationItem';

interface ILocalProps {
  repositoryName: IRepository['name'];
  fullDataLocationComponents: IFullDataLocationComponents;
  data: IFolderElement;
}

const FolderElement = ({
  data,
  repositoryName,
  fullDataLocationComponents,
}: ILocalProps) => {
  return (
    <NavigationItem
      name={data.name}
      iconType={matchType(
        { folder: () => 'folder', blob: () => 'file' },
        data.type
      )}
      to={RouteHeplers.addName(data.name, data.type, {
        ...fullDataLocationComponents,
        repositoryName,
      })}
    />
  );
};

export default FolderElement;
