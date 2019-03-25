import * as React from 'react';
import cn from 'classnames';

import styles from './Button.module.css';

interface IProps {
  children: React.ReactChild | React.ReactChildren;
  disabled?: boolean;
  onClick(): void;
  // todo rename
  variant?: 'like-link' | 'default';
}

class Button extends React.PureComponent<IProps> {
  public render() {
    const { children, variant, onClick } = this.props;
    return (
      <button className={cn(styles.button, { variant: variant || 'default' })} onClick={onClick}>
        {children}
      </button>
    );
  }
}

export default Button;
