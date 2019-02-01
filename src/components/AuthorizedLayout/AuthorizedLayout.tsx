import React from 'react';
import { BrowserRouter as Router, Link, Redirect, Route, RouteComponentProps, RouteProps, Switch } from 'react-router-dom';
import AuthorizedLayoutHeader from '../AuthorizedLayoutHeader/AuthorizedLayoutHeader';
import { FilterSelect } from '../FilterSelect/FilterSelect';
import { GenericNotFound } from '../GenericNotFound/GenericNotFound';
import Model from '../Model/Model';
import Models from '../Models/Models';
import Projects from '../Projects/Projects';
import styles from './AuthorizedLayout.module.css';

const notFoundRedirect = () => <Redirect to="/not-found" />;

// tslint:disable-next-line:variable-name
export const RouteWithFilter = ({ component, ...rest }: RouteProps) => {
  if (!component) {
    throw Error('component is undefined');
  }

  // tslint:disable-next-line:variable-name
  const Component = component; // JSX Elements have to be uppercase.
  const render = (props: RouteComponentProps<any>): React.ReactNode => {
    return (
      <div className={styles.content_area}>
        <div className={styles.filters_bar}>
          <FilterSelect placeHolderText="Search models and filters" />
        </div>
        <div className={styles.content}>
          <Component {...props} />
        </div>
      </div>
    );
  };

  return <Route {...rest} render={render} />;
};

export default class AuthorizedLayout extends React.PureComponent {
  public render() {
    return (
      <Router>
        <div className={styles.layout}>
          <div className={styles.header}>
            <AuthorizedLayoutHeader />
          </div>
          <Switch>
            <RouteWithFilter exact={true} path={'/'} component={Projects} />
            <RouteWithFilter path={'/project/:projectId/models'} component={Models} />
            <RouteWithFilter path={'/project/:projectId/model/:modelId'} component={Model} />
            <Route component={GenericNotFound} />
          </Switch>
        </div>
      </Router>
    );
  }
}
