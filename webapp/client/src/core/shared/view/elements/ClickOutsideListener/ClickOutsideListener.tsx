import { bind } from 'decko';
import * as React from 'react';
import onClickOutside from 'react-onclickoutside';

interface ILocalProps {
  onClickOutside: () => void;
  children: React.ReactNode;
}

class ClickOutsideListener extends React.Component<ILocalProps> {
  public render() {
    return this.props.children;
  }

  @bind
  private handleClickOutside() {
    this.props.onClickOutside();
  }
}

export default onClickOutside(ClickOutsideListener);
