import { ReactWrapper } from 'enzyme';
import React from 'react';

import { CommitComponentLocation } from 'core/shared/models/Versioning/CommitComponentLocation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import { IFullCommitComponentLocationComponents } from 'core/shared/models/Versioning/RepositoryData';
import { repositories } from 'core/shared/utils/tests/mocks/Versioning/repositoriesMocks';
import routes from 'core/shared/routes';
import makeMountComponentWithPredefinedData from 'utils/tests/integrations/makeMountComponentWithPredefinedData';
import { userWorkspacesWithCurrentUser } from 'utils/tests/mocks/models/workspace';

import RepositoryBreadcrumbs from '../RepositoryBreadcrumbs';
import { findByDataTestAttribute } from 'core/shared/utils/tests/react/helpers';

const currentWorkspace = userWorkspacesWithCurrentUser.user;
const repository: IRepository = repositories[0];

const makeComponent = async ({
  fullCommitComponentLocationComponents,
}: {
  fullCommitComponentLocationComponents: IFullCommitComponentLocationComponents;
}) => {
  delete (global as any).window.location;
  (global as any).window = Object.create(window);
  (global as any).window.location = {
    pathname: routes.repositoryData.getRedirectPath({
      repositoryName: repository.name,
      workspaceName: userWorkspacesWithCurrentUser.user.name,
    }),
  };

  const data = await makeMountComponentWithPredefinedData({
    settings: {},
    predefinedData: {
      currentWorkspace,
    },
    Component: () => (
      <RepositoryBreadcrumbs
        repositoryName={repository.name}
        fullCommitComponentLocationComponents={
          fullCommitComponentLocationComponents
        }
      />
    ),
  });

  return {
    ...data,
  };
};

const getDisplayedBreadcrumbs = (component: ReactWrapper) => {
  const breadcrumbs = findByDataTestAttribute('breadcrumb', component).map(
    wr => {
      const isActive = Boolean(!wr.find('a').length);
      return {
        isActive,
        link: !isActive
          ? wr
              .find('a')
              .at(0)
              .props().href
          : null,
        name: wr.text(),
      };
    }
  );

  return breadcrumbs;
};

describe('(feature repositoryData) RepositoryBreadcrumbs', () => {
  it('should render correct breadcrumbs', async () => {
    const fullCommitComponentLocationComponents: IFullCommitComponentLocationComponents = {
      commitPointer: {
        type: 'branch',
        value: 'master',
      },
      location: ['blobs', 'config', 'hyperparams'] as CommitComponentLocation,
      type: 'blob',
    };

    const { component } = await makeComponent({
      fullCommitComponentLocationComponents,
    });

    const expectedBreadcrumbs = [
      {
        isActive: false,
        link: `/${currentWorkspace.name}/repositories/${repository.name}/data`,
        name: 'repository-name',
      },
      {
        isActive: false,
        link: `/${currentWorkspace.name}/repositories/${
          repository.name
        }/data/folder/master/blobs`,
        name: 'blobs',
      },
      {
        isActive: false,
        link: `/${currentWorkspace.name}/repositories/${
          repository.name
        }/data/folder/master/blobs/config`,
        name: 'config',
      },
      {
        isActive: true,
        link: null,
        name: 'hyperparams',
      },
    ];

    expect(getDisplayedBreadcrumbs(component)).toEqual(expectedBreadcrumbs);
  });
});
