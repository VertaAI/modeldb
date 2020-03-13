import cn from 'classnames';
import React from 'react';
import { IconType, Icon } from '../Icon/Icon';

import styles from './ActionIcon.module.css';

interface ILocalProps {
  onClick?: (e: React.MouseEvent<HTMLButtonElement, MouseEvent>) => void;
  disabled?: boolean;
  dataTest?: string;
  iconType: IconType;
  className?: string;
}

const ActionIcon: React.FC<ILocalProps> = ({
  onClick,
  disabled,
  iconType,
  dataTest,
  className,
}) => {
  return (
    <Icon
      className={cn(styles.icon, className, { [styles.disabled]: disabled })}
      type={iconType}
      onClick={disabled ? e => e : onClick}
      dataTest={dataTest}
    />
  );
};

export default React.memo(ActionIcon);
