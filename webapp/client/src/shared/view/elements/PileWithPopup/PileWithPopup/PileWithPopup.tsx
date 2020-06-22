import { bind } from 'decko';
import * as React from 'react';

interface ILocalProps {
  pileComponent: (props: { showPopup(): void }) => React.ReactNode;
  popupComponent: (props: {
    isOpen: boolean;
    closePopup(): void;
  }) => React.ReactNode;
}

type GetReactComponentProps<T> = T extends React.Component<infer P> ? P : never;

interface ILocalState {
  isOpenPopup: boolean;
}

class PileWithPopup extends React.Component<ILocalProps, ILocalState> {
  public state: ILocalState = {
    isOpenPopup: false,
  };

  public render() {
    return (
      <>
        {this.props.pileComponent({ showPopup: this.showPopup })}
        {this.props.popupComponent({
          isOpen: this.state.isOpenPopup,
          closePopup: this.closePopup,
        })}
      </>
    );
  }

  @bind
  private showPopup() {
    this.setState({ isOpenPopup: true });
  }
  @bind
  private closePopup() {
    this.setState({ isOpenPopup: false });
  }
}

export default PileWithPopup;
