import cn from 'classnames';
import * as React from 'react';
import ReactModal from 'react-modal';

import Button from '../Button/Button';
import { Icon, IconType } from '../Icon/Icon';
import styles from './Dialog.module.css';

interface ILocalProps {
  title: Exclude<React.ReactNode, null | undefined>;
  isOpen: boolean;
  type: 'info' | 'warning' | 'success';
  titleIcon?: IconType;
  children: Exclude<React.ReactNode, null | undefined>;
  cancelButtonProps?: Omit<
    React.ComponentProps<typeof Button>,
    'children' | 'size' | 'theme'
  > & { text?: string };
  okButtonProps?: Omit<
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
      titleIcon,
      cancelButtonProps,
      okButtonProps,
      onRequestClose,
    } = this.props;
    return (
      <ReactModal
        className={cn(styles.dialog, {
          [styles.type_warning]: type === 'warning',
          [styles.type_info]: type === 'info',
          [styles.type_success]: type === 'success',
          [styles.withoutActions]: Boolean(
            !okButtonProps && !cancelButtonProps
          ),
        })}
        overlayClassName={styles.overlay}
        appElement={appElement || document.createElement('div')}
        isOpen={isOpen}
        onRequestClose={onRequestClose as any}
      >
        <Icon
          type="close"
          dataTest="dialog-close"
          className={styles.close}
          onClick={onRequestClose}
        />
        <div className={styles.header}>
          {!titleIcon && type === 'warning' && (
            <Icon
              className={cn(styles.icon, styles.warningIcon)}
              type="exclamation-triangle"
            />
          )}
          {titleIcon && <Icon className={styles.icon} type={titleIcon} />}
          <div className={styles.title}>{title}</div>
        </div>
        <div className={styles.content}>{children}</div>
        <div className={styles.actions}>
          {cancelButtonProps && (
            <div className={styles.action}>
              <Button theme="secondary" {...cancelButtonProps}>
                {cancelButtonProps.text || 'Cancel'}
              </Button>
            </div>
          )}
          {okButtonProps && (
            <div className={styles.action}>
              <Button
                theme="primary"
                {...okButtonProps}
                dataTest={okButtonProps.dataTest || 'confirm'}
              >
                {okButtonProps.text || 'Ok'}
              </Button>
            </div>
          )}
        </div>
      </ReactModal>
    );
  }
}

export default Dialog;
