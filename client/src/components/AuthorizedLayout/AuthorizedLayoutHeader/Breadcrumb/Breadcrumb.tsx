import { bind } from 'decko';
import { UnregisterCallback } from 'history';
import { Project } from 'models/Project';
import React from 'react';
import { connect } from 'react-redux';
import { Link, RouteComponentProps, withRouter } from 'react-router-dom';

import Icon from 'components/shared/Icon/Icon';
import Preloader from 'components/shared/Preloader/Preloader';
import ModelRecord from 'models/ModelRecord';
import routes from 'routes';
import { selectExperimentRuns } from 'store/experiment-runs';
import { selectModelRecord } from 'store/model-record';
import { selectProjects } from 'store/projects';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

import styles from './Breadcrumb.module.css';
import BreadcrumbsBuilder from './BreadcrumbsBuilder';

interface IPropsFromState {
  projects?: Project[] | null;
  experimentRuns?: ModelRecord[] | null;
  modelRecord?: ModelRecord | null;
}

interface ILocalState {
  pathname: string;
}

type AllProps = IPropsFromState & IConnectedReduxProps & RouteComponentProps;

class Breadcrumb extends React.Component<AllProps, ILocalState> {
  public state: ILocalState = {
    pathname: this.props.history.location.pathname.toLowerCase(),
  };

  private unlistenCallback: UnregisterCallback | undefined = undefined;

  public componentDidMount() {
    this.unlistenCallback = this.props.history.listen((location, action) => {
      this.setState({
        ...this.state,
        pathname: location.pathname.toLowerCase(),
      });
    });
  }

  public componentWillUnmount() {
    if (this.unlistenCallback) {
      this.unlistenCallback();
    }
  }

  public render() {
    const breadcrumbs = this.getBreadcrumbs();

    return (
      <div className={styles.content}>
        {!breadcrumbs.checkLoaded() ? (
          <Preloader variant="dots" />
        ) : (
          <>
            {breadcrumbs
              .map(item => (
                <Link className={styles.link} to={item.redirectPath}>
                  {item.getName()}
                </Link>
              ))
              .map((value, index, content) => {
                return index === content.length - 1 ? (
                  <div
                    className={`${styles.breadcrumb_item} ${
                      styles.active_link
                    }`}
                    key={index}
                  >
                    {value}
                  </div>
                ) : (
                  <React.Fragment key={index}>
                    <div className={styles.breadcrumb_item}>{value}</div>
                    <Icon className={styles.arrow} type="arrow-right" />
                  </React.Fragment>
                );
              })}
          </>
        )}
      </div>
    );
  }

  @bind
  private getBreadcrumbs() {
    const { projects, modelRecord } = this.props;

    return BreadcrumbsBuilder()
      .then({
        routes: [routes.mainPage],
        getName: () => 'projects',
      })
      .then({
        routes: [routes.experimentRuns, routes.charts],
        checkLoaded: params =>
          Boolean(
            projects &&
              projects.some(project => project.id === params.projectId)
          ),
        getName: params =>
          projects!.find(project => project.id === params.projectId)!.name,
      })
      .then({
        routes: [routes.modelRecord],
        checkLoaded: () => Boolean(modelRecord),
        getName: () => modelRecord!.name,
      })
      .build(this.state.pathname);
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => ({
  experimentRuns: selectExperimentRuns(state),
  modelRecord: selectModelRecord(state),
  projects: selectProjects(state),
});

export default withRouter(connect(mapStateToProps)(Breadcrumb));
