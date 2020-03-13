import * as React from 'react';

import Button, { IButtonLocalProps } from '../Button/Button';
import { IconType } from '../Icon/Icon';
import Popup from '../Popup/Popup';
import styles from './Confirm.module.css';

interface ILocalProps {
  isOpen: boolean;
  title: string;
  titleIcon?: IconType;
  children: Exclude<React.ReactNode, null | undefined>;
  confirmButtonTheme?: IButtonLocalProps['theme'];
  cancelButtonTheme?: IButtonLocalProps['theme'];
  onConfirm(e: React.MouseEvent): void;
  onCancel(e: React.MouseEvent | MouseEvent): void;
}

class Confirm extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      children,
      isOpen,
      title,
      titleIcon,
      onConfirm,
      onCancel,
      confirmButtonTheme,
      cancelButtonTheme,
    } = this.props;
    return (
      <Popup
        title={title}
        isOpen={isOpen}
        onRequestClose={onCancel}
        titleIcon={titleIcon}
      >
        <div className={styles.root} data-test="confirm">
          <div className={styles.message}>{children}</div>
          <div className={styles.actions}>
            <div className={styles.action}>
              <Button
                theme={cancelButtonTheme || 'secondary'}
                onClick={onCancel}
                dataTest="confirm-cancel-button"
              >
                Cancel
              </Button>
            </div>
            <div className={styles.action}>
              <Button
                theme={confirmButtonTheme || 'primary'}
                onClick={onConfirm}
                dataTest="confirm-ok-button"
              >
                Ok
              </Button>
            </div>
          </div>
        </div>
      </Popup>
    );
  }
}

export default Confirm;
