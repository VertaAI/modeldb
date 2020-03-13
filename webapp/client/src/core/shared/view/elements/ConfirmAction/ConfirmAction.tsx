import * as React from 'react';

import { bind } from 'decko';

import Confirm from '../Confirm/Confirm';

interface ILocalProps {
  confirmText: Exclude<React.ReactNode, null | undefined>;
  children: (
    withConfirmAction: <T extends any[]>(
      f: (...args: T) => any
    ) => (...args: T) => any
  ) => React.ReactNode;
}

interface ILocalState {
  isShowConfirm: boolean;
}

class ConfirmAction extends React.Component<ILocalProps, ILocalState> {
  public state: ILocalState = { isShowConfirm: false };

  private action: ((...args: any[]) => any) | null = null;

  public render() {
    return (
      <>
        {this.props.children(this.saveConfirmAction)}
        <Confirm
          isOpen={this.state.isShowConfirm}
          title="Warning!"
          confirmButtonTheme="red"
          onCancel={this.closeConfirm}
          onConfirm={this.confirmAction}
        >
          {this.props.confirmText}
        </Confirm>
      </>
    );
  }

  @bind
  private saveConfirmAction(action: (...args: any[]) => any) {
    return (...args: any[]) => {
      this.action = () => action(...args);
      this.showConfirm();
    };
  }

  @bind
  private confirmAction() {
    if (this.action) {
      this.action();
    } else {
      console.error('this.action is undefined!');
    }
    this.closeConfirm();
  }

  @bind
  private showConfirm() {
    this.setState({ isShowConfirm: true });
  }
  @bind
  private closeConfirm() {
    this.setState({ isShowConfirm: false });
  }
}

export default ConfirmAction;
