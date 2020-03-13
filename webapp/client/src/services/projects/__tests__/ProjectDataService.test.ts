import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';

import {
  ServerFilterValueType,
  ServerFilterOperator,
} from 'core/features/filter/service/serverModel/Filters/Filters';
import {
  makeDefaultTagFilter,
  makeDefaultStringFilter,
} from 'core/features/filter/Model';
import { IPagination } from 'core/shared/models/Pagination';
import { userWorkspacesWithCurrentUser } from 'utils/tests/mocks/models/workspace';

import ProjectDataService from '../ProjectDataService';
import { ILoadProjectsRequest } from '../responseRequest/makeLoadProjectsRequest';

const mockSupportedFiltersMap = {
  description: makeDefaultStringFilter('description', 'imdb rating', true),
  name: { ...makeDefaultStringFilter('name', 'INSOME ', true), invert: true },
  tag: makeDefaultTagFilter('adf'),
};

const mockPagination: IPagination = {
  currentPage: 0,
  pageSize: 5,
  totalCount: 20,
};

const workspaceName = userWorkspacesWithCurrentUser.user.name;

describe('services', () => {
  describe('ProjectDataService', () => {
    describe('loadProjects', () => {
      it('should correct convert filters to server filters', async () => {
        const mock = new MockAdapter(axios);

        mock
          .onPost('/v1/modeldb/hydratedData/findHydratedProjects')
          .reply(200, {});

        await new ProjectDataService().loadProjects(
          Object.values(mockSupportedFiltersMap),
          mockPagination,
          workspaceName
        );

        const requestData = JSON.parse(
          mock.history.post[0].data
        ) as ILoadProjectsRequest;

        const expectedServerPredicates: ILoadProjectsRequest['predicates'] = [
          {
            key: mockSupportedFiltersMap.description.name,
            value: mockSupportedFiltersMap.description.value,
            value_type: ServerFilterValueType.STRING,
            operator: ServerFilterOperator.CONTAIN,
          },
          {
            key: mockSupportedFiltersMap.name.name,
            value: mockSupportedFiltersMap.name.value,
            value_type: ServerFilterValueType.STRING,
            operator: ServerFilterOperator.NOT_CONTAIN,
          },
          {
            key: mockSupportedFiltersMap.tag.name,
            value: mockSupportedFiltersMap.tag.value,
            value_type: ServerFilterValueType.STRING,
            operator: ServerFilterOperator.EQ,
          },
        ];
        expect(requestData.predicates).toEqual(expectedServerPredicates);
      });

      it('should correct convert pagination to server pagination', async () => {
        const mock = new MockAdapter(axios);

        mock
          .onPost('/v1/modeldb/hydratedData/findHydratedProjects')
          .reply(200, {});

        await new ProjectDataService().loadProjects(
          [],
          mockPagination,
          workspaceName
        );

        const requestData = JSON.parse(
          mock.history.post[0].data
        ) as ILoadProjectsRequest;

        expect(requestData).toMatchObject({
          page_limit: mockPagination.pageSize,
          page_number: mockPagination.currentPage + 1,
        } as ILoadProjectsRequest);
      });
    });
  });
});

export {};
