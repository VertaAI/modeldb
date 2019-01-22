import React, { Component } from 'react';
import { connect } from 'react-redux';
import { BrowserRouter as Router, Link, Route, Switch } from 'react-router-dom';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import AuthorizedLayoutHeader from '../AuthorizedLayoutHeader/AuthorizedLayoutHeader';
import Model from '../Model/Model';
import Models from '../Models/Models';
import Projects from '../Projects/Projects';
import styles from './AuthorizedLayout.module.css';

// Create an intersection type of the component props and our Redux props.
type AllProps = IConnectedReduxProps;

class AuthorizedLayout extends Component<AllProps> {
  public render() {
    return (
      <Router>
        <div className={styles.layout}>
          <div className={styles.header}>
            <AuthorizedLayoutHeader />
          </div>
          <div className={styles.filters_bar} />
          <div className={styles.content}>
            <Switch>
              <Route exact={true} path="/" component={Projects} />
              <Route path="/project/:projectId/models" component={Models} />
              <Route path="/model/:modelId" component={Model} />
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

export default connect<{}, {}, {}, IApplicationState>(mapStateToProps)(AuthorizedLayout);
