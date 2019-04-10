import copy from 'copy-to-clipboard';
import { bind } from 'decko';
import * as React from 'react';

import Tooltip from '../Tooltip/Tooltip';

interface ILocalProps {
  text: string;
  children: (onCopy: () => void) => React.ReactNode;
}

interface ILocalState {
  isShownNotificationAboutCopied: boolean;
}

class CopyToClipboard extends React.Component<ILocalProps, ILocalState> {
  public state: ILocalState = { isShownNotificationAboutCopied: false };

  public render() {
    const { children } = this.props;
    const { isShownNotificationAboutCopied } = this.state;

    return (
      <Tooltip visible={isShownNotificationAboutCopied} content="Copied">
        {children(this.onCopy)}
      </Tooltip>
    );
  }

  @bind
  private onCopy() {
    copy(this.props.text);
    this.setState({ isShownNotificationAboutCopied: true });
    setTimeout(
      () => this.setState({ isShownNotificationAboutCopied: false }),
      1000
    );
  }
}

export default CopyToClipboard;
