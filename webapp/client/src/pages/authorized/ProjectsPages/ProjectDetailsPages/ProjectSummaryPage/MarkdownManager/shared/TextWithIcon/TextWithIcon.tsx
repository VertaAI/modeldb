import * as React from 'react';

import { IconType } from 'core/shared/view/elements/Icon/Icon';

import MarkdownIcon from '../Icon/Icon';
import styles from './TextWithIcon.module.css';

interface ILocalProps {
  text: string;
  icon: IconType;
}

class TextWithIcon extends React.PureComponent<ILocalProps> {
  public render() {
    const { text, icon } = this.props;
    return (
      <div className={styles.root}>
        <div className={styles.icon}>
          <MarkdownIcon type={icon} />
        </div>
        {text}
      </div>
    );
  }
}

export default TextWithIcon;
