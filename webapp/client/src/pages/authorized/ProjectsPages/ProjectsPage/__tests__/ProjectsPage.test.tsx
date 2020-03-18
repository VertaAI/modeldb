import { shallow } from 'enzyme';
import * as React from 'react';

import ProjectWidget from 'components/ProjectWidget/ProjectWidget';
import { createCodeError } from 'core/shared/models/Error';
import { makeDefaultStringFilter } from 'core/features/filter/Model';
import { initialCommunication } from 'core/shared/utils/redux/communication';
import { makeRouterMockProps } from 'core/shared/utils/tests/react/routeComponentProps';
import PageCommunicationError from 'core/shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import NoEntitiesStub from 'core/shared/view/elements/NoEntitiesStub/NoEntitiesStub';
import NoResultsStub from 'core/shared/view/elements/NoResultsStub/NoResultsStub';
import Pagination from 'core/shared/view/elements/Pagination/Pagination';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import { GetRouteParams } from 'routes';
import { currentUserProjects } from 'utils/tests/mocks/models/projectsMocks';
import { userWorkspacesWithCurrentUser } from 'utils/tests/mocks/models/workspace';

import {
  IProjectsPageAllProps,
  ProjectsPageView,
  CurrentRoute,
} from '../ProjectsPage';
import styles from '../ProjectsPage.module.css';

const makeShallowComponent = (props: Partial<IProjectsPageAllProps> = {}) => {
  const defaultProps: IProjectsPageAllProps = {
    ...makeRouterMockProps<GetRouteParams<CurrentRoute>>({
      workspaceName: userWorkspacesWithCurrentUser.user.name,
    }),
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

  return shallow(<ProjectsPageView {...defaultProps} {...props} />);
};

describe('pages', () => {
  describe('ProjectsPage', () => {
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
        filters: [makeDefaultStringFilter('name', 'project', true)],
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

    it('should render the "not entities" stub if a user has not projects', () => {
      const component = makeShallowComponent({ projects: [] });

      expect(component.find(`${styles.root}`).children.length).toEqual(1);
      expect(component.find(NoEntitiesStub).length).toBe(1);
    });
  });
});
