import * as React from 'react';

import styles from './ButtonLikeLink.module.css';

interface IProps {
  children: React.ReactChild;
  to?: string;
  onClick?(): void;
}

class ButtonLikeLink extends React.PureComponent<IProps> {
  public render() {
    const { to, children, onClick } = this.props;
    const Elem = (props: React.HTMLProps<any>) =>
      to ? <a href={to} {...props} /> : <button {...props} />;
    return (
      <Elem className={styles.button_like_link} onClick={onClick}>
        {children}
      </Elem>
    );
  }
}

export default ButtonLikeLink;
