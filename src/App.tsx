import User from 'models/User';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { BrowserRouter as Router, Link, Route, Switch } from 'react-router-dom';
import { IApplicationState, IConnectedReduxProps } from 'store/store';
import AuthorizedLayout from './components/AuthorizedLayout/AuthorizedLayout';

// Any additional component props go here.
interface IOwnProps {
  history: any;
}

interface IPropsFromState {
  user: User | undefined;
}

// Create an intersection type of the component props and our Redux props.
type AllProps = IPropsFromState & IConnectedReduxProps & IOwnProps;

class App extends Component<AllProps> {
  public render() {
    return <AuthorizedLayout history={this.props.history} />;
  }
}

const mapStateToProps = ({ layout }: IApplicationState) => ({
  user: layout.user
});

export default connect<IPropsFromState, {}, IOwnProps, IApplicationState>(mapStateToProps)(App);
