import cn from 'classnames';
import * as React from 'react';
import ReactModal from 'react-modal';

import { Icon, IconType } from '../Icon/Icon';
import styles from './Popup.module.css';

interface ILocalProps {
  title: Exclude<React.ReactNode, null | undefined>;
  isOpen: boolean;
  height?: number;
  contentLabel?: string;
  children: React.ReactChild | React.ReactChild[] | React.ReactChildren;
  titleIcon?: IconType;
  width?: number;
  size?: 'small' | 'medium' | 'large' | 'extraLarge';
  position?: 'top' | 'center';
  onRequestClose(e: React.MouseEvent | MouseEvent): void;
}

const appElement = document.getElementById('root')!;

class Popup extends React.Component<ILocalProps> {
  public render() {
    const {
      title,
      height,
      isOpen,
      contentLabel,
      width,
      children,
      titleIcon,
      size = 'medium',
      position = 'top',
      onRequestClose,
    } = this.props;
    return (
      <ReactModal
        className={cn(styles.popup, {
          [styles.size_large]: size === 'large',
          [styles.size_medium]: size === 'medium',
          [styles.size_small]: size === 'small',
          [styles.size_extra_large]: size === 'extraLarge',
          [styles.position_top]: position === 'top',
          [styles.position_center]: position === 'center',
        })}
        overlayClassName={styles.overlay}
        appElement={appElement || document.createElement('div')}
        isOpen={isOpen}
        contentLabel={contentLabel}
        style={{ content: { height, width } }}
        onRequestClose={onRequestClose as any}
      >
        <div className={styles.header}>
          <div className={titleIcon ? styles.title_with_icon : styles.title}>
            {titleIcon && (
              <Icon className={styles.title_icon} type={titleIcon} />
            )}
            <div>{title}</div>
          </div>
          <Icon
            type="close"
            className={styles.close}
            onClick={onRequestClose}
          />
        </div>
        <div className={styles.content}>{children}</div>
      </ReactModal>
    );
  }
}

export default Popup;
