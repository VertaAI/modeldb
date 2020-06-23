import React from 'react';
import { Link } from 'react-router-dom';

import * as CommitComponentLocation from 'shared/models/Versioning/CommitComponentLocation';

import styles from './Breadcrumbs.module.css';

interface ILocalProps {
  breadcrumbItems: IBreadcrumb[];
}

interface IBreadcrumb {
  to: string;
  name: string;
  isDisabled?: boolean;
}

const Breadcrumbs: React.FC<ILocalProps> = ({ breadcrumbItems }) => {
  return (
    <div className={styles.root}>
      {breadcrumbItems.map((item, i) => (
        <Breadcrumb
          key={i}
          {...item}
          isDisabled={item.isDisabled || i === breadcrumbItems.length - 1}
        />
      ))}
    </div>
  );
};

const Breadcrumb = React.memo(({ to, name, isDisabled }: IBreadcrumb) => {
  return (
    <div className={styles.breadcrumb} data-test="breadcrumb">
      {isDisabled ? (
        name
      ) : (
        <Link to={to} className={styles.breadcrumbLink}>
          {name}
        </Link>
      )}
    </div>
  );
});

export const generateBreadcrumbs = (
  location: CommitComponentLocation.CommitComponentLocation,
  locationPathnameToBreadcrumb: (
    name: string,
    locationPathname: CommitComponentLocation.CommitComponentLocation
  ) => IBreadcrumb,
  firstBreadcrumb?: IBreadcrumb
): IBreadcrumb[] => {
  const locationBreadcrumbs = location.map((el, index) =>
    locationPathnameToBreadcrumb(
      el,
      CommitComponentLocation.getLocationByIndex(location, index)
    )
  );

  return firstBreadcrumb
    ? [firstBreadcrumb, ...locationBreadcrumbs]
    : locationBreadcrumbs;
};

export default React.memo(Breadcrumbs);
