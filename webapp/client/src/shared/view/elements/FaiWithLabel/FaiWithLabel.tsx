import cn from 'classnames';
import * as React from 'react';

import { Icon, IconType } from '../Icon/Icon';
import styles from './FaiWithLabel.module.css';

interface ILocalProps {
  theme: 'blue' | 'green';
  iconType: IconType;
  label: string;
  dataTest?: string;
  isDisabled?: boolean;
  onClick(event: React.MouseEvent<HTMLButtonElement, MouseEvent>): void;
  onHover?(): void;
  onUnhover?(): void;
}

class FaiWithLabel extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      theme,
      onClick,
      isDisabled: disabled,
      iconType,
      label,
      dataTest,
      onHover,
      onUnhover,
    } = this.props;
    return (
      <div
        className={cn(styles.root, {
          [styles.disabled]: Boolean(disabled),
          [styles.theme_blue]: theme === 'blue',
          [styles.theme_green]: theme === 'green',
        })}
      >
        <button
          className={styles.fai}
          disabled={disabled}
          data-test={dataTest}
          onClick={onClick}
          onMouseEnter={onHover}
          onMouseLeave={onUnhover}
        >
          <Icon className={styles.icon} type={iconType} />
        </button>
        <span className={styles.label}>{label}</span>
      </div>
    );
  }
}

export type IFaiWithLabelLocalProps = ILocalProps;
export default FaiWithLabel;
