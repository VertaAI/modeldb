import React from 'react';
import { connect } from 'react-redux';
import { useLocation } from 'react-router-dom';

import { ICommitHistorySettings } from 'features/versioning/commitsHistory/store/types';
import * as CommitComponentLocation from 'shared/models/Versioning/CommitComponentLocation';
import { IRepository } from 'shared/models/Versioning/Repository';
import Breadcrumbs, {
  generateBreadcrumbs,
} from 'shared/view/domain/Versioning/Breadcrumbs/Breadcrumbs';
import routes from 'shared/routes';
import { IApplicationState } from 'store/store';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';

const mapStateToProps = (state: IApplicationState) => ({
  currentWorkspaceName: selectCurrentWorkspaceName(state),
});

interface ILocalProps {
  settings: ICommitHistorySettings;
  repositoryName: IRepository['name'];
}

type AllProps = ILocalProps & ReturnType<typeof mapStateToProps>;

const CommitsHistoryBreadcrumbs: React.FC<AllProps> = ({
  repositoryName,
  settings,
}) => {
  const location = useLocation();

  const breadcrumbItems = generateBreadcrumbs(
    settings.location,
    (name, locationPathname) => ({
      to: routes.repositoryCommitsHistory.getRedirectPathWithQueryParams({
        params: {
          ...routes.repositoryCommitsHistory.getMatch(location.pathname)!,
          locationPathname: CommitComponentLocation.toPathname(
            locationPathname
          ),
          commitPointerValue: settings.branch,
        },
        queryParams: {},
      }),
      name,
    }),
    {
      to: routes.repositoryCommitsHistory.getRedirectPathWithQueryParams({
        params: {
          ...routes.repositoryCommitsHistory.getMatch(location.pathname)!,
          locationPathname: undefined,
          commitPointerValue: settings.branch,
        },
        queryParams: {},
      }),
      name: repositoryName,
    }
  );

  return <Breadcrumbs breadcrumbItems={breadcrumbItems} />;
};

export default connect(mapStateToProps)(React.memo(CommitsHistoryBreadcrumbs));
