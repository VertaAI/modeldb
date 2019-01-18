import React, { Component } from 'react';
import { connect } from 'react-redux';
import { BrowserRouter as Router, Link, Route, Switch } from 'react-router-dom';
import Login from '../../components/Login/Login';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import LayoutHeader from '../LayoutHeader/LayoutHeader';
import styles from './AnonymousLayout.module.css';

type AllProps = IConnectedReduxProps;

class AnonymousLayout extends Component<AllProps> {
  public render() {
    return (
      <Router>
        <div className={styles.layout}>
          <div className={styles.header}>
            <LayoutHeader />
          </div>
          <div className={styles.content}>
            <Switch>
              <Route exact={true} path="/" component={Login} />
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

export default connect<{}, {}, {}, IApplicationState>(mapStateToProps)(AnonymousLayout);
