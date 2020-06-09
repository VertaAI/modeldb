import moment from 'moment';

import { IHydratedCommit } from 'core/shared/models/Versioning/RepositoryData';
import { users } from 'core/shared/utils/tests/mocks/models/users';

import { IGroupedCommitsByDate } from '../../store/types';
import groupCommitsByDatesInDescOrder from '../groupCommitsByDatesInDescOrder';

describe('(feature commitsHistory) helpers', () => {
  describe('groupCommitsByDatesInDescOrder', () => {
    it('should group commits by date and sort groups in desc order', () => {
      const today = moment(new Date())
        .endOf('day')
        .toDate();
      const yesterday = moment(today)
        .subtract(1, 'day')
        .toDate();
      const commits: IHydratedCommit[] = [
        {
          author: users[0],
          dateCreated: moment(today)
            .subtract(4, 'hour')
            .toDate(),
          message: 'blabla',
          sha: 'sha-1',
          type: 'withParent',
        },
        {
          author: users[0],
          dateCreated: yesterday,
          message: 'blabla',
          sha: 'sha-2',
          type: 'withParent',
        },
        {
          author: users[0],
          dateCreated: today,
          message: 'blabla',
          sha: 'sha-3',
          type: 'withParent',
        },
      ];

      const expected: IGroupedCommitsByDate[] = [
        {
          dateCreated: today,
          commits: [commits[2], commits[0]],
        },
        {
          dateCreated: yesterday,
          commits: [commits[1]],
        },
      ];
      const res = groupCommitsByDatesInDescOrder(commits);
      expect(res.length).toEqual(expected.length);
      expect(res).toEqual(expected);
    });
  });
});
