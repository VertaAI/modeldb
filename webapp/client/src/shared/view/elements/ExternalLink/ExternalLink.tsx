import cn from 'classnames';
import * as React from 'react';

import { Icon } from 'shared/view/elements/Icon/Icon';

import styles from './ExternalLink.module.css';

interface ILocalProps {
  url: string;
  text: string;
  additionalClassname?: string;
  rootStyle?: object;
}

class ExternalLink extends React.PureComponent<ILocalProps> {
  public render() {
    const { url, text, additionalClassname, rootStyle } = this.props;
    return (
      <a
        className={cn(styles.root, additionalClassname)}
        href={url}
        target="blank"
        style={rootStyle}
      >
        <span className={styles.text}>{text}</span>
        &nbsp;
        <Icon className={styles.icon} type="external-link" />
      </a>
    );
  }
}

export default ExternalLink;
