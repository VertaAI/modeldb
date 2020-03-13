import cn from 'classnames';
import * as React from 'react';

import { Icon, IconType } from 'core/shared/view/elements/Icon/Icon';

import styles from './Pile.module.css';

interface ILocalProps {
  additionalClassname?: string;
  title: string;
  label: string;
  iconType: IconType;
  dataTest?: string;
  labelDataTest?: string;
  onClick(): void;
}

class Pile extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      additionalClassname,
      label,
      labelDataTest,
      title,
      dataTest,
      iconType,
      onClick,
    } = this.props;
    return (
      <div
        className={cn(styles.root, additionalClassname)}
        title={title}
        data-test={dataTest}
        onClick={onClick}
      >
        <div className={styles.notif}>
          <Icon className={styles.notif_icon} type={iconType} />
        </div>
        <div className={styles.label} data-test={labelDataTest}>
          {label}
        </div>
      </div>
    );
  }
}

export default Pile;
