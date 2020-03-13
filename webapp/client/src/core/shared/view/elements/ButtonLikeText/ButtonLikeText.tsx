import cn from 'classnames';
import * as React from 'react';

import styles from './ButtonLikeText.module.css';

interface ILocalProps {
  children: React.ReactChild;
  isDisabled?: boolean;
  to?: string;
  dataTest?: string;
  fullWidth?: boolean;
  onClick?(): void;
}

class ButtonLikeText extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      to,
      children,
      isDisabled: disabled,
      fullWidth,
      dataTest,
      onClick,
    } = this.props;
    const Elem = (props: React.HTMLProps<any>) =>
      to ? (
        <a href={to} {...props} data-test={dataTest} />
      ) : (
        <button {...props} data-test={dataTest} type="button" />
      );
    return (
      <Elem
        className={cn(styles.button_like_text, {
          [styles.full_width]: Boolean(fullWidth),
          [styles.disabled]: Boolean(disabled),
        })}
        disabled={disabled}
        onClick={onClick}
      >
        {children}
      </Elem>
    );
  }
}

export default ButtonLikeText;
