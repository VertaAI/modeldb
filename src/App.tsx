import React, { Component } from 'react';
import { connect } from 'react-redux';
import { IApplicationState, IConnectedReduxProps } from 'store/store';
import AnonymousLayout from './components/AnonymousLayout/AnonymousLayout';
import AuthorizedLayout from './components/AuthorizedLayout/AuthorizedLayout';
import User from './models/User';

interface IPropsFromState {
  user: User | null;
}

// Create an intersection type of the component props and our Redux props.
type AllProps = IPropsFromState & IConnectedReduxProps;

class App extends Component<AllProps> {
  public render() {
    const user = this.props.user;
    return user ? <AuthorizedLayout /> : <AnonymousLayout />;
  }
}

const mapStateToProps = ({ layout }: IApplicationState) => ({
  user: layout.user
});

export default connect(mapStateToProps)(App);
