import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';

import User from 'models/User';
import { IApplicationState } from 'store/store';
import { selectCurrentUser } from 'store/user';

import DeveloperKeyHint from './DeveloperKeyHint/DeveloperKeyHint';
import DeveloperKeyInfo from './DeveloperKeyInfo/DeveloperKeyInfo';

interface IState {
  display: null | 'info' | 'hint';
}

interface IPropsFromState {
  user: User;
}

type IProps = IPropsFromState;

// todo maybe rename
class DevelopmentKeyManager extends React.PureComponent<IProps, IState> {
  public state: IState = { display: 'info' };

  public render() {
    const { user } = this.props;
    switch (this.state.display) {
      case null:
        return '';
      case 'info':
        return (
          <DeveloperKeyInfo
            user={user}
            onHide={this.makeChangeDisplay('hint')}
          />
        );
      case 'hint':
        return <DeveloperKeyHint onHide={this.makeChangeDisplay(null)} />;
    }
  }

  @bind
  private makeChangeDisplay(display: IState['display']) {
    return () => this.setState({ display });
  }
}

const mapStateProps = (state: IApplicationState): IPropsFromState => {
  return {
    user: selectCurrentUser(state)!,
  };
};

export default connect(mapStateProps)(DevelopmentKeyManager);
