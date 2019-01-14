import React, { Component } from 'react';
import { connect } from 'react-redux';
import { BrowserRouter as Router, Link, Route, Switch } from 'react-router-dom';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import LayoutHeader from '..//LayoutHeader/LayoutHeader';
import Projects from '../Projects/Projects';
import styles from './AuthorizedLayout.module.css';

// Any additional component props go here.
interface IOwnProps {
  history: any;
}

interface IPropsFromState {}

// Create an intersection type of the component props and our Redux props.
type AllProps = IPropsFromState & IConnectedReduxProps & IOwnProps;

class AuthorizedLayout extends Component<AllProps> {
  public render() {
    return (
      <Router>
        <div className={styles.layout}>
          <div className={styles.header}>
            <LayoutHeader />
          </div>
          <div className={styles.filters_bar} />
          <div className={styles.content}>
            <Switch>
              <Route exact={true} path="/" component={Projects} />
            </Switch>
          </div>
        </div>
      </Router>
    );
  }
}

const mapStateToProps = ({ layout }: IApplicationState) => ({
  user: layout.user
});

export default connect<IPropsFromState, {}, IOwnProps, IApplicationState>(
  mapStateToProps
)(AuthorizedLayout);
