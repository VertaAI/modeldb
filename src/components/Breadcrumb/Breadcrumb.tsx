import { UnregisterCallback } from 'history';
import * as React from 'react';
import { connect } from 'react-redux';
import { Link, RouteComponentProps, Router, withRouter } from 'react-router-dom';
import { Model } from '../../models/Model';
import Project from '../../models/Project';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import styles from './Breadcrumb.module.css';
import { BreadcrumbItem } from './BreadcrumbItem';

interface IPropsFromState {
  project?: Project | null;
  model?: Model | null;
}

interface ILocalState {
  url: string;
}

type AllProps = IPropsFromState & IConnectedReduxProps & RouteComponentProps;

class Breadcrumb extends React.Component<AllProps, ILocalState> {
  private unlistenCallback: UnregisterCallback | undefined = undefined;

  private indexBreadcrumbItem = new BreadcrumbItem(/^\/$/, '/', 'PROJECTS');
  private projectBreadcrumbItem = new BreadcrumbItem(/^\/project\/([\w-]*)\/models.?$/);
  private modelBreadcrumbItem = new BreadcrumbItem(/^\/project\/([\w-]*)\/model\/([\w-]*).?$/);

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
    content.push(this.renderItem(currentItem));

    while (currentItem.PreviousItem) {
      currentItem = currentItem.PreviousItem;
      content.push(this.renderItem(currentItem));
    }

    return (
      <div className={styles.content}>
        {content.reverse().map((value: JSX.Element, index: number) => {
          return (
            <span className={index === content.length - 1 ? styles.active_link : ''} key={index}>
              {value}
            </span>
          );
        })}
      </div>
    );
  }

  private prepareItem(): BreadcrumbItem {
    const { project, model } = this.props;

    this.projectBreadcrumbItem.Name = project ? project.Name.toLocaleUpperCase() : model ? model.ProjectName.toLocaleUpperCase() : '';
    this.projectBreadcrumbItem.Path = `/project/${project ? project.Id : model ? model.ProjectId : ''}/models`;
    this.projectBreadcrumbItem.PreviousItem = this.indexBreadcrumbItem;

    this.modelBreadcrumbItem.Name = model ? model.Name.toLocaleUpperCase() : '';
    this.modelBreadcrumbItem.Path = model ? `/project/${model.ProjectId}/model/${model.Id}` : '';
    this.modelBreadcrumbItem.PreviousItem = this.projectBreadcrumbItem;

    const breadcrumbItems: BreadcrumbItem[] = [];
    breadcrumbItems.push(this.indexBreadcrumbItem);
    breadcrumbItems.push(this.projectBreadcrumbItem);
    breadcrumbItems.push(this.modelBreadcrumbItem);

    return breadcrumbItems.find(x => x.ShouldMatch.test(this.state.url)) || this.indexBreadcrumbItem;
  }

  private renderItem(item: BreadcrumbItem) {
    return (
      <span>
        {item.PreviousItem ? <i className={`fa fa-angle-right ${styles.arrow}`} /> : ''}
        <Link className={styles.link} to={item.Path}>
          {item.Name}
        </Link>
      </span>
    );
  }
}

const mapStateToProps = ({ project, model }: IApplicationState) => ({
  model: model.data,
  project: project.data
});

export default withRouter(connect(mapStateToProps)(Breadcrumb));
