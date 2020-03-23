import cn from 'classnames';
import * as React from 'react';
import ReactModal from 'react-modal';

import Button from '../Button/Button';
import { Icon } from '../Icon/Icon';
import styles from './Dialog.module.css';

interface ILocalProps {
  title: Exclude<React.ReactNode, null | undefined>;
  isOpen: boolean;
  type: 'info' | 'warning';
  children: Exclude<React.ReactNode, null | undefined>;
  cancelButtonProps: Omit<
    React.ComponentProps<typeof Button>,
    'children' | 'size' | 'theme'
  > & { text?: string };
  okButtonProps: Omit<
    React.ComponentProps<typeof Button>,
    'children' | 'size' | 'theme'
  > & { text?: string };
  onRequestClose(e: React.MouseEvent): void;
}

const appElement = document.getElementById('root')!;

class Dialog extends React.Component<ILocalProps> {
  public render() {
    const {
      title,
      isOpen,
      children,
      type,
      cancelButtonProps,
      okButtonProps,
      onRequestClose,
    } = this.props;
    return (
      <ReactModal
        className={cn(styles.dialog, {
          [styles.type_warning]: type === 'warning',
          [styles.type_info]: type === 'info',
        })}
        overlayClassName={styles.overlay}
        appElement={appElement || document.createElement('div')}
        isOpen={isOpen}
        onRequestClose={onRequestClose as any}
      >
        <Icon type="close" className={styles.close} onClick={onRequestClose} />
        <div className={styles.header}>
          {type === 'warning' && (
            <Icon className={styles.warningIcon} type="exclamation-triangle" />
          )}
          <div className={styles.title}>{title}</div>
        </div>
        <div className={styles.content}>{children}</div>
        <div className={styles.actions}>
          <div className={styles.action}>
            <Button theme="secondary" {...cancelButtonProps}>
              {cancelButtonProps.text || 'Cancel'}
            </Button>
          </div>
          <div className={styles.action}>
            <Button theme="primary" {...okButtonProps}>
              {okButtonProps.text || 'Ok'}
            </Button>
          </div>
        </div>
      </ReactModal>
    );
  }
}

export default Dialog;
