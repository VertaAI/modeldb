import { bind } from 'decko';
import * as React from 'react';

import { removeProtocols } from 'core/shared/utils/formatters/urlFormatter';
import Button from '../Button/Button';
import ClickOutsideListener from '../ClickOutsideListener/ClickOutsideListener';
import CopyButton from '../CopyButton/CopyButton';
import Fai from '../Fai/Fai';
import { Icon } from '../Icon/Icon';
import styles from './ShareLink.module.css';

interface ILocalProps {
  link: string;
  buttonType: 'fai' | 'default';
}

interface ILocalState {
  isShowLink: boolean;
}

class ShareLink extends React.PureComponent<ILocalProps, ILocalState> {
  public state: ILocalState = {
    isShowLink: false,
  };

  public render() {
    const { link, buttonType } = this.props;
    const { isShowLink } = this.state;
    return (
      <ClickOutsideListener onClickOutside={this.hideLink}>
        <div className={styles.root}>
          {buttonType === 'default' && (
            <Button size="medium" onClick={this.showLink}>
              Share Page
            </Button>
          )}
          {buttonType === 'fai' && (
            <Fai
              theme="primary"
              variant="outlined"
              icon={<Icon type="share" />}
              onClick={this.showLink}
            />
          )}
          {isShowLink && (
            <div
              className={
                buttonType === 'fai'
                  ? styles.linkContainer__fai
                  : styles.linkContainer
              }
            >
              <div className={styles.linkContainer__link}>
                {removeProtocols(link)}
              </div>
              <CopyButton value={removeProtocols(link)} />
              <div className={styles.close_button}>
                <Icon type="cancel" onClick={this.hideLink} />
              </div>
            </div>
          )}
        </div>
      </ClickOutsideListener>
    );
  }

  @bind
  private showLink() {
    if (!this.state.isShowLink) {
      this.setState({ isShowLink: true });
    }
  }

  @bind
  private hideLink() {
    if (this.state.isShowLink) {
      this.setState({ isShowLink: false });
    }
  }
}

export default ShareLink;
