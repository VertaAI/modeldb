import React, { Component } from 'react';
import { connect } from 'react-redux';
import { BrowserRouter as Router, Link, Route, Switch } from 'react-router-dom';
import Footer from '../../components/Footer/Footer';
import Login from '../../components/Login/Login';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import AnonymousLayoutHeader from '../AnonymousLayoutHeader/AnonymousLayoutHeader';
import styles from './AnonymousLayout.module.css';

type AllProps = IConnectedReduxProps;

class AnonymousLayout extends Component<AllProps> {
  public render() {
    return (
      <Router>
        <div className={styles.layout}>
          <div className={styles.header}>
            <AnonymousLayoutHeader />
          </div>
          <div className={styles.content}>
            <Switch>
              <Route exact={true} path="/" component={Login} />
            </Switch>
          </div>
          <div className={styles.footer}>
            <Footer />
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
