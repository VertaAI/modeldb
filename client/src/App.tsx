import React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps, withRouter } from 'react-router-dom';

import { IApplicationState, IConnectedReduxProps } from './store/store';
import { selectIsCheckingUserAuthentication, checkUserAuthentication, selectIsUserAuthenticated } from './store/user';
import AnonymousLayout from 'components/AnonymousLayout/AnonymousLayout';
import AuthorizedLayout from 'components/AuthorizedLayout/AuthorizedLayout';
import GlobalPreloader from 'components/GlobalPreloader/GlobalPreloader';

interface IPropsFromState {
  isUserAuthenticated: boolean;
  isCheckingUserAuthentication: boolean;
}

// Create an intersection type of the component props and our Redux props.
type AllProps = IPropsFromState & IConnectedReduxProps & RouteComponentProps;

class App extends React.Component<AllProps> {
  public componentDidMount() {
    this.props.dispatch(checkUserAuthentication());
  }

  public render() {
    const { isCheckingUserAuthentication, isUserAuthenticated } = this.props;
    if (isCheckingUserAuthentication) {
      return <GlobalPreloader />;
    }
    return isUserAuthenticated ? <AuthorizedLayout /> : <AnonymousLayout />;
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => ({
  isUserAuthenticated: selectIsUserAuthenticated(state),
  isCheckingUserAuthentication: selectIsCheckingUserAuthentication(state)
});

export default withRouter(connect(mapStateToProps)(App));
