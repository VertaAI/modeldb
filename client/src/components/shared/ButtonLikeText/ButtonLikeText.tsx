import * as React from 'react';

import styles from './ButtonLikeText.module.css';

interface ILocalProps {
  children: React.ReactChild;
  to?: string;
  onClick?(): void;
}

class ButtonLikeText extends React.PureComponent<ILocalProps> {
  public render() {
    const { to, children, onClick } = this.props;
    const Elem = (props: React.HTMLProps<any>) =>
      to ? <a href={to} {...props} /> : <button {...props} type="button" />;
    return (
      <Elem className={styles.button_like_text} onClick={onClick}>
        {children}
      </Elem>
    );
  }
}

export default ButtonLikeText;
