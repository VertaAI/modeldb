import { Popper } from '@material-ui/core';
import cn from 'classnames';
import copy from 'copy-to-clipboard';
import React, { useState, useCallback, useRef } from 'react';

import styles from './TextWithCopyTooltip.module.css';

interface ILocalProps {
  children: React.ReactChild;
  copyText: string;
  withEllipsis?: boolean;
}

export const TextWithCopyTooltip: React.FC<ILocalProps> = ({
  children,
  copyText,
  withEllipsis,
}) => {
  const [isCopied, changeIsCopied] = useState(false);
  const [isShowTooltip, changeIsShowTooltip] = useState(false);
  const rootRef = useRef(null);

  const onMouseEnter = () => changeIsShowTooltip(true);
  const onMouseLeave = () => {
    changeIsCopied(false);
    changeIsShowTooltip(false);
  };

  const onCopy = useCallback(() => {
    copy(copyText);
    changeIsCopied(true);
  }, [copyText, changeIsCopied]);

  return (
    <React.Fragment>
      <span
        className={cn(styles.root, { [styles.ellipsis]: withEllipsis })}
        onClick={onCopy}
        onMouseEnter={onMouseEnter}
        onMouseLeave={onMouseLeave}
        ref={rootRef}
      >
        {children}
        <Popper
          open={isShowTooltip}
          anchorEl={rootRef.current}
          placement="top"
          style={{ zIndex: 10 }}
        >
          <Tooltip
            copyText={copyText}
            isCopied={isCopied}
            isForCopied={false}
          />
        </Popper>
        <Popper
          open={isShowTooltip}
          anchorEl={rootRef.current}
          placement="top"
          style={{ zIndex: 10 }}
        >
          <Tooltip copyText={copyText} isCopied={isCopied} isForCopied={true} />
        </Popper>
      </span>
    </React.Fragment>
  );
};

const Tooltip = ({
  copyText,
  isCopied,
  isForCopied,
}: {
  copyText: string;
  isCopied: boolean;
  isForCopied: boolean;
}) => {
  return (
    <div
      className={cn(styles.tooltip, {
        [styles.visible]: isCopied === isForCopied,
      })}
    >
      {isForCopied ? (
        <>
          <span>Copied to clipboard</span>
          <span className={styles.copyText}>{copyText}</span>
        </>
      ) : (
        'Copy to clipboard'
      )}
    </div>
  );
};
