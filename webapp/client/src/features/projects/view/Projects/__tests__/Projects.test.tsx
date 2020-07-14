import { shallow } from 'enzyme';
import * as React from 'react';

import { makeDefaultStringFilter } from 'shared/models/Filters';
import { createCodeError } from 'shared/models/Error';
import { initialCommunication } from 'shared/utils/redux/communication';
import { makeRouterMockProps } from 'shared/utils/tests/react/routeComponentProps';
import PageCommunicationError from 'shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import NoResultsStub from 'shared/view/elements/NoResultsStub/NoResultsStub';
import Pagination from 'shared/view/elements/Pagination/Pagination';
import Preloader from 'shared/view/elements/Preloader/Preloader';
import routes, { GetRouteParams } from 'shared/routes';
import { currentUserProjects } from 'shared/utils/tests/mocks/models/projectsMocks';
import { userWorkspacesWithCurrentUser } from 'shared/utils/tests/mocks/models/workspace';

import { IProjectsAllProps, ProjectsView } from '../Projects';
import styles from '../Projects.module.css';
import ProjectWidget from '../ProjectWidget/ProjectWidget';

const makeShallowComponent = (props: Partial<IProjectsAllProps> = {}) => {
  const defaultProps: IProjectsAllProps = {
    ...makeRouterMockProps<GetRouteParams<typeof routes.projects>>({
      workspaceName: userWorkspacesWithCurrentUser.user.name,
    }),
    workspaceName: userWorkspacesWithCurrentUser.user.name as any,
    dispatch: jest.fn(),
    filters: [],
    loadingProjects: initialCommunication,
    projects: [],
    pagination: {
      currentPage: 0,
      pageSize: 5,
      totalCount: 20,
    },
  };

  return shallow(<ProjectsView {...defaultProps} {...props} />);
};

describe('(feature) Projects', () => {
  it('should render projects if they are not empty', () => {
    const component = makeShallowComponent({
      projects: currentUserProjects,
    });

    expect(component.find(ProjectWidget).length).toBe(
      currentUserProjects.length
    );
  });
  it('should render pagination if projects are not empty', () => {
    const component = makeShallowComponent({
      projects: currentUserProjects,
    });

    expect(component.find(Pagination).length).toBe(1);
  });

  it('should render preloader if projects are loading', () => {
    const component = makeShallowComponent({
      loadingProjects: {
        isRequesting: true,
        error: undefined,
        isSuccess: false,
      },
    });

    expect(component.find(Preloader).length).toBe(1);
  });
  it('should render error if projects loading is failed', () => {
    const component = makeShallowComponent({
      loadingProjects: {
        isRequesting: false,
        error: createCodeError('error'),
        isSuccess: false,
      },
    });

    expect(component.find(PageCommunicationError).length).toBe(1);
  });

  it('should render "Not found Projects" if a user doesn`t have projects which match filters', () => {
    const component = makeShallowComponent({
      projects: [],
      filters: [makeDefaultStringFilter('name', 'project', 'LIKE')],
    });

    expect(component.find(NoResultsStub).length).toBe(1);
  });

  it('should render "Not found Projects if a user doesn`t have projects on pagination current page', () => {
    const component = makeShallowComponent({
      projects: [],
      pagination: { currentPage: 1, pageSize: 5, totalCount: 20 },
    });

    expect(component.find(NoResultsStub).length).toBe(1);
  });
});
