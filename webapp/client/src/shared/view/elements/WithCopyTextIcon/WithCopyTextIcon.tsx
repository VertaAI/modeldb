import * as React from 'react';

import CopyToClipboard from '../CopyToClipboard/CopyToClipboard';
import styles from './WithCopyTextIcon.module.css';
import { Icon } from '../Icon/Icon';

interface ILocalProps {
  text: string;
  children: React.ReactNode;
  onClick?(e: React.MouseEvent): void;
}

const WithCopyTextIcon = ({ children, text, onClick }: ILocalProps) => {
  return (
    <div className={styles.root}>
      {children}
      <span className={styles.copyIcon} onClick={onClick}>
        <CopyToClipboard text={text}>
          {onCopy => <Icon type="copy-to-clipboard" onClick={onCopy} />}
        </CopyToClipboard>
      </span>
    </div>
  );
};

export default WithCopyTextIcon;
