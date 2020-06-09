import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';
import { defaultBranch } from 'core/shared/models/Versioning/RepositoryData';
import routes from 'core/shared/routes';

import { ICommitHistorySettings } from '../store/types';

const parseCommitsHistorySettings = (location: {
  pathname: string;
  search: string;
}): ICommitHistorySettings => {
  const match = routes.repositoryCommitsHistory.getMatch(location.pathname);
  const params = routes.repositoryCommitsHistory.parseQueryParams(
    location.search
  );
  return {
    branch: match ? match.commitPointerValue : defaultBranch,
    currentPage: params && params.page ? Number(params.page) - 1 : 0,
    location:
      match && match.locationPathname
        ? CommitComponentLocation.makeFromPathname(match.locationPathname)
        : CommitComponentLocation.makeRoot(),
  };
};

export default parseCommitsHistorySettings;
