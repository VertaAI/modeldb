import React from 'react';
import { Link } from 'react-router-dom';

import { IconType, Icon } from 'shared/view/elements/Icon/Icon';

import styles from './NavigationItem.module.css';

interface ILocalProps {
  to: string;
  name: string;
  iconType?: IconType;
}

const NavigationItem: React.FC<ILocalProps> = ({ to, name, iconType }) => {
  return (
    <div className={styles.root}>
      <div className={styles.iconWrapper}>
        {iconType && <Icon className={styles.icon} type={iconType} />}
      </div>

      <div className={styles.name} title={name}>
        <Link className={styles.name__link} to={to}>
          {name}
        </Link>
      </div>
    </div>
  );
};

export default React.memo(NavigationItem);
