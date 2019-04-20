import { bind } from 'decko';
import * as React from 'react';

import ButtonLikeText from 'components/shared/ButtonLikeText/ButtonLikeText';
import Icon from 'components/shared/Icon/Icon';
import styles from './DeveloperKeyHint.module.css';

interface ILocalProps {
  onHide(): void;
}

class DeveloperKeyHint extends React.PureComponent<ILocalProps> {
  public render() {
    const { onHide } = this.props;
    return (
      <div className={styles.root}>
        <Icon type="key" className={styles.key_icon} />
        <span className={styles.text}>
          We will store your developer key on a settings page and you’re able to
          generate a new one key.{' '}
          <ButtonLikeText onClick={onHide}>OK, Got it!</ButtonLikeText>
        </span>
      </div>
    );
  }
}

export default DeveloperKeyHint;
