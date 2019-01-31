import React from 'react';
import { BrowserRouter as Router, Link, Route, Switch } from 'react-router-dom';
import Callback from '../../components/Callback/Callback';
import Footer from '../../components/Footer/Footer';
import Login from '../../components/Login/Login';
import AnonymousLayoutHeader from '../AnonymousLayoutHeader/AnonymousLayoutHeader';
import styles from './AnonymousLayout.module.css';

export default class AnonymousLayout extends React.PureComponent {
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
              <Route path="/callback" component={Callback} />
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
