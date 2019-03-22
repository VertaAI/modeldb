import * as React from 'react';

import styles from './Button.module.css';

interface IProps {
  children: React.ReactChild | React.ReactChildren;
  disabled?: boolean;
  onClick(): void;
}

class Button extends React.PureComponent<IProps> {
  public render() {
    const { children, onClick } = this.props;
    return (
      <button className={styles.button} onClick={onClick}>
        {children}
      </button>
    );
  }
}

export default Button;
