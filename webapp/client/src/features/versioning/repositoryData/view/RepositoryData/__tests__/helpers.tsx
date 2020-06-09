import { ReactWrapper } from 'enzyme';

import { findByText } from 'core/shared/utils/tests/react/helpers';
import { IFolder } from 'core/shared/models/Versioning/RepositoryData';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import { IWorkspace } from 'core/shared/models/Workspace';

export const findDataLinkHref = (name: string, component: ReactWrapper) =>
  findDataLink(name, component).prop('href');
export const findDataLink = (name: string, component: ReactWrapper) => {
  return findByText(name, component).closest('a');
};

export const viewCommitComponent = (name: string, component: ReactWrapper) => {
  findDataLink(name, component).simulate('click', {
    button: 0,
  });
};

export const checkFolderContent = (
  folder: IFolder,
  component: ReactWrapper
) => {
  folder.subFolders.forEach(subFolder => {
    expect(findByText(subFolder.name, component).length).toEqual(1);
  });

  folder.blobs.forEach(blob => {
    expect(findByText(blob.name, component).length).toEqual(1);
  });
};
export const checkFolderContentWithLinks = ({
  folder,
  component,
  currentWorkspace,
  repository,
}: {
  folder: IFolder;
  component: ReactWrapper;
  repository: IRepository;
  currentWorkspace: IWorkspace;
}) => {
  const subFolder = folder.subFolders[0];
  const subFolderExpectedLink = `/${currentWorkspace.name}/repositories/${
    repository.name
  }/data/folder/master/${subFolder.name}`;
  expect(findByText(subFolder.name, component).length).toEqual(1);
  expect(findDataLinkHref(subFolder.name, component)).toEqual(
    subFolderExpectedLink
  );

  const blob = folder.blobs[0];
  expect(findByText(blob.name, component).length).toEqual(1);
  expect(findDataLinkHref(blob.name, component)).toEqual(
    `/${currentWorkspace.name}/repositories/${
      repository.name
    }/data/blob/master/${blob.name}`
  );
};
