import cn from 'classnames';
import * as React from 'react';

import styles from './Fai.module.css';

interface ILocalProps {
  isActive?: boolean;
  isDisabled?: boolean;
  theme: 'primary' | 'red' | 'tertiary';
  icon: React.ReactNode;
  variant?: 'outlined';
  dataTest?: string;
  unmutedIcon?: boolean;
  onClick(event: React.MouseEvent<HTMLButtonElement, MouseEvent>): void;
}

// Float Action for Icons
class Fai extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      icon,
      theme = 'primary',
      isDisabled = false,
      isActive = false,
      dataTest,
      onClick,
      unmutedIcon,
    } = this.props;
    return (
      <button
        className={cn(styles.fai, {
          [styles.disabled]: isDisabled,
          [styles.active]: isActive,
          [styles.unmuted_icon]: unmutedIcon,
          [styles.variant_outlined]: 'outlined',
          [styles.theme_primary]: theme === 'primary',
          [styles.theme_red]: theme === 'red',
          [styles.theme_tertiary]: theme === 'tertiary',
        })}
        disabled={isDisabled}
        data-test={dataTest}
        onClick={onClick}
      >
        <div className={styles.icon}>{icon}</div>
      </button>
    );
  }
}

export type IFaiLocalProps = ILocalProps;
export default Fai;
