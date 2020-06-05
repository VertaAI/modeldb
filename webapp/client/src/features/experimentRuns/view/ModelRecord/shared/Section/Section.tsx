import * as React from 'react';

import { IconType, Icon } from 'core/shared/view/elements/Icon/Icon';

import styles from './Section.module.css';

interface ILocalProps {
  title: string;
  iconType?: IconType;
  children: React.ReactNode;
}

const Section = ({ title, children, iconType }: ILocalProps) => {
  return (
    <div className={styles.root}>
      <div className={styles.header}>
        {iconType && <Icon type={iconType} className={styles.icon} />}
        <div className={styles.title}>{title}</div>
      </div>
      <div className={styles.content}>{children}</div>
    </div>
  );
};

export default Section;
