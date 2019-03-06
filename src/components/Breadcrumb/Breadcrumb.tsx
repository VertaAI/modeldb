import { UnregisterCallback } from 'history';
import { Project } from 'models/Project';
import React from 'react';
import { connect } from 'react-redux';
import { Link, RouteComponentProps, withRouter } from 'react-router-dom';
import { BreadcrumbItem } from '../../models/BreadcrumbItem';
import ModelRecord from '../../models/ModelRecord';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import styles from './Breadcrumb.module.css';
import headerArrow from './images/header-arrow.svg';

interface IPropsFromState {
  projects?: Project[] | null;
  experimentRuns?: ModelRecord[] | null;
  modelRecord?: ModelRecord | null;
}

interface ILocalState {
  url: string;
}

type AllProps = IPropsFromState & IConnectedReduxProps & RouteComponentProps;

class Breadcrumb extends React.Component<AllProps, ILocalState> {
  private unlistenCallback: UnregisterCallback | undefined = undefined;

  private indexBreadcrumbItem = new BreadcrumbItem(/^\/$/, '/', 'PROJECTS');
  private projectBreadcrumbItem = new BreadcrumbItem(/^\/project\/([\w-]*)\/exp-runs.?$/);
  private modelBreadcrumbItem = new BreadcrumbItem(/^\/project\/([\w-]*)\/exp-run\/([\w-]*).?$/);

  public constructor(props: AllProps) {
    super(props);

    this.state = {
      url: this.props.history.location.pathname.toLowerCase()
    };

    this.prepareItem = this.prepareItem.bind(this);
  }

  public componentDidMount() {
    this.unlistenCallback = this.props.history.listen((location, action) => {
      this.setState({ ...this.state, url: location.pathname.toLowerCase() });
    });
  }

  public componentWillUnmount() {
    if (this.unlistenCallback) {
      this.unlistenCallback();
    }
  }

  public render() {
    const content: JSX.Element[] = [];
    let currentItem = this.prepareItem();
    if (!currentItem) return '';

    while (currentItem) {
      content.push(this.renderItem(currentItem));
      currentItem = currentItem.previousItem;
    }

    return (
      <div className={styles.content}>
        {content.reverse().map((value: JSX.Element, index: number) => {
          return index === content.length - 1 ? (
            <div className={`${styles.breadcrumb_item} ${styles.active_link}`} key={index}>
              {value}
            </div>
          ) : (
            <React.Fragment key={index}>
              <div className={styles.breadcrumb_item}>{value}</div>
              <img className={styles.arrow} src={headerArrow} />
            </React.Fragment>
          );
        })}
      </div>
    );
  }

  private prepareItem(): BreadcrumbItem | undefined {
    const { experimentRuns, modelRecord, projects } = this.props;
    let projectName = 'experiment runs';
    if (projects && projects.length > 0 && experimentRuns && experimentRuns.length > 0) {
      const neededProject = projects.find((project: Project) => project.id === experimentRuns[0].projectId);
      if (neededProject) {
        projectName = neededProject.name;
      }
    }
    this.projectBreadcrumbItem.name = projectName;

    this.projectBreadcrumbItem.path = `/project/${
      experimentRuns && experimentRuns.length > 0 ? experimentRuns[0].projectId : modelRecord ? modelRecord.projectId : ''
    }/exp-runs`;
    this.projectBreadcrumbItem.previousItem = this.indexBreadcrumbItem;

    this.modelBreadcrumbItem.name = modelRecord ? modelRecord.name : '';
    this.modelBreadcrumbItem.path = modelRecord ? `/project/${modelRecord.projectId}/exp-run/${modelRecord.id}` : '';
    this.modelBreadcrumbItem.previousItem = this.projectBreadcrumbItem;

    const breadcrumbItems: BreadcrumbItem[] = [];
    breadcrumbItems.push(this.indexBreadcrumbItem);
    breadcrumbItems.push(this.projectBreadcrumbItem);
    breadcrumbItems.push(this.modelBreadcrumbItem);

    return breadcrumbItems.find(x => x.shouldMatch.test(this.state.url));
  }

  private renderItem(item: BreadcrumbItem) {
    return (
      <Link className={styles.link} to={item.path}>
        {item.name}
      </Link>
    );
  }
}

const mapStateToProps = ({ experimentRuns, modelRecord, projects }: IApplicationState) => ({
  experimentRuns: experimentRuns.data,
  modelRecord: modelRecord.data,
  projects: projects.data
});

export default withRouter(connect(mapStateToProps)(Breadcrumb));
