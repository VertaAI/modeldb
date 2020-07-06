import * as React from 'react';

import { Icon, IconType } from 'shared/view/elements/Icon/Icon';

import styles from './Icon.module.css';

interface ILocalProps {
  type: IconType;
}

class MarkdownIcon extends React.PureComponent<ILocalProps> {
  public render() {
    return <Icon className={styles.root} type={this.props.type} />;
  }
}

export default MarkdownIcon;
