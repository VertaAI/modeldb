import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';

import * as DataLocation from 'core/shared/models/Versioning/DataLocation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import { IFullDataLocationComponents } from 'core/shared/models/Versioning/RepositoryData';
import { IApplicationState } from 'store/store';
import { selectCurrentWorkspaceName } from 'store/workspaces';
import * as RouteHelpers from '../../routeHelpers';

import styles from './RepositoryBreadcrumbs.module.css';

const mapStateToProps = (state: IApplicationState) => ({
  currentWorkspaceName: selectCurrentWorkspaceName(state),
});

interface ILocalProps {
  fullDataLocationComponents: IFullDataLocationComponents;
  repositoryName: IRepository['name'];
}

type AllProps = ILocalProps & ReturnType<typeof mapStateToProps>;

interface IRepositoryBreadcrumb {
  to: string;
  name: string;
  isDisabled?: boolean;
}

const RepositoryBreadcrumbs: React.FC<AllProps> = ({
  repositoryName,
  fullDataLocationComponents: { commitPointer, location },
}) => {
  const locationBreadcrumbs = location.map((el, index) => ({
    to: RouteHelpers.getRedirectPath({
      commitPointer,
      location: DataLocation.getLocationByIndex(location, index),
      repositoryName,
      type: 'folder',
    }),
    name: el,
  }));

  const breacrumbItems: IRepositoryBreadcrumb[] = [
    {
      to: RouteHelpers.getRedirectPath({
        commitPointer,
        repositoryName,
        location: DataLocation.makeRoot(),
        type: 'folder',
      }),
      name: repositoryName,
    },
    ...locationBreadcrumbs,
  ];

  return (
    <div className={styles.root}>
      {breacrumbItems.map((item, i) => (
        <Breadcrumb
          key={i}
          {...item}
          isDisabled={i === breacrumbItems.length - 1}
        />
      ))}
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

export default connect(mapStateToProps)(React.memo(RepositoryBreadcrumbs));
