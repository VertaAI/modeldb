import * as React from 'react';

import Dialog from '../Dialog/Dialog';

interface ILocalProps {
  isOpen: boolean;
  title: string;
  children: Exclude<React.ReactNode, null | undefined>;
  confirmButtonText?: string;
  cancelButtonText?: string;
  onConfirm(e: React.MouseEvent): void;
  onCancel(e: React.MouseEvent | MouseEvent): void;
}

class Confirm extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      children,
      isOpen,
      title,
      onConfirm,
      onCancel,
      cancelButtonText,
      confirmButtonText,
    } = this.props;
    return (
      <Dialog
        title={title}
        type="warning"
        cancelButtonProps={{
          dataTest: 'confirm-cancel-button',
          text: cancelButtonText,
          onClick: onCancel as any,
        }}
        isOpen={isOpen}
        okButtonProps={{
          dataTest: 'confirm-ok-button',
          text: confirmButtonText,
          onClick: onConfirm as any,
        }}
        onRequestClose={onCancel}
      >
        {children}
      </Dialog>
    );
  }
}

export default Confirm;
