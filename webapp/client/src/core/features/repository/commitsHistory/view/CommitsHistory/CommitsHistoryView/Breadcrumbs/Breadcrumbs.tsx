import React from 'react';
import { connect } from 'react-redux';
import { Link, useLocation } from 'react-router-dom';

import * as DataLocation from 'core/shared/models/Repository/DataLocation';
import routes from 'routes';
import { IRepository } from 'core/shared/models/Repository/Repository';
import { IApplicationState } from 'store/store';
import { selectCurrentWorkspaceName } from 'store/workspaces';
import { ICommitHistorySettings } from 'core/features/repository/commitsHistory/store/types';

import styles from './Breadcrumbs.module.css';

const mapStateToProps = (state: IApplicationState) => ({
  currentWorkspaceName: selectCurrentWorkspaceName(state),
});

interface ILocalProps {
  settings: ICommitHistorySettings;
  repositoryName: IRepository['name'];
}

type AllProps = ILocalProps & ReturnType<typeof mapStateToProps>;

interface IRepositoryBreadcrumb {
  to: string;
  name: string;
  isDisabled?: boolean;
}

const Breadcrumbs: React.FC<AllProps> = ({ repositoryName, settings }) => {
  const location = useLocation();
  const locationBreadcrumbs = settings.location.map((el, index) => ({
    to: routes.repositoryCommitsHistory.getRedirectPathWithQueryParams({
      params: {
        ...routes.repositoryCommitsHistory.getMatch(location.pathname)!,
        locationPathname: DataLocation.getLocationByIndex(
          settings.location,
          index
        ) as any,
        commitPointerValue: settings.branch,
      },
      queryParams: {},
    }),
    name: el,
  }));

  const breacrumbItems: IRepositoryBreadcrumb[] = [
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
    },
    ...locationBreadcrumbs,
  ];

  return (
    <div className={styles.root}>
      <div className={styles.text}>History for</div>
      <div className={styles.breadcrumbs}>
        {breacrumbItems.map((item, i) => (
          <Breadcrumb
            key={i}
            {...item}
            isDisabled={i === breacrumbItems.length - 1}
          />
        ))}
      </div>
    </div>
  );
};

const Breadcrumb = React.memo(
  ({ to, name, isDisabled }: IRepositoryBreadcrumb) => {
    return isDisabled ? (
      <div className={styles.breadcrumb}>{name}</div>
    ) : (
      <Link to={to} className={styles.breadcrumb}>
        {name}
      </Link>
    );
  }
);

export default connect(mapStateToProps)(React.memo(Breadcrumbs));
