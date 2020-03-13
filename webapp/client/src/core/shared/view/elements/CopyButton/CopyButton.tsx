import React from 'react';

import ButtonLikeText from '../ButtonLikeText/ButtonLikeText';
import CopyToClipboard from '../CopyToClipboard/CopyToClipboard';

interface ILocalProps {
  value: string;
  children?: string;
  isDisabled?: boolean;
}

class CopyButton extends React.PureComponent<ILocalProps> {
  public render() {
    const { children = 'Copy' } = this.props;
    return (
      <CopyToClipboard text={this.props.value}>
        {onCopy => (
          <ButtonLikeText
            isDisabled={this.props.isDisabled}
            onClick={onCopy}
            dataTest="copy-button"
          >
            {children}
          </ButtonLikeText>
        )}
      </CopyToClipboard>
    );
  }
}

export default CopyButton;
