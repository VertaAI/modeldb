import moment from 'moment';
import * as R from 'ramda';

import { IGroupedCommitsByDate, ICommitView } from '../store/types';

const groupCommitsByDatesInDescOrder = (
  commits: ICommitView[]
): IGroupedCommitsByDate[] => {
  return R.groupWith(
    (c1, c2) => moment(c1.dateCreated).isSame(moment(c2.dateCreated), 'date'),
    R.sortBy(({ dateCreated }) => dateCreated, commits).reverse()
  ).map(group => {
    const res: IGroupedCommitsByDate = {
      dateCreated: group[0].dateCreated,
      commits: group,
    };
    return res;
  });
};

export default groupCommitsByDatesInDescOrder;
