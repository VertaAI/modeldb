import cn from 'classnames';
import * as React from 'react';

import { Icon } from 'core/shared/view/elements/Icon/Icon';

import styles from './ExternalLink.module.css';

interface ILocalProps {
  url: string;
  text: string;
  additionalClassname?: string;
}

class ExternalLink extends React.PureComponent<ILocalProps> {
  public render() {
    const { url, text, additionalClassname } = this.props;
    return (
      <a
        className={cn(styles.root, additionalClassname)}
        href={url}
        target="blank"
      >
        {text} &nbsp;
        <Icon type="external-link" />
      </a>
    );
  }
}

export default ExternalLink;
