import * as React from 'react';
import { connect } from 'react-redux';
import { Link, RouteComponentProps, Router, withRouter } from 'react-router-dom';
import { Model } from '../../models/Model';
import Project from '../../models/Project';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import styles from './Breadcrumb.module.css';

interface IPropsFromState {
  project?: Project | null;
  model?: Model | null;
}

interface ILocalState {
  url: string;
}

enum Paths {
  Index = 0,
  Project = 1,
  Model = 2
}

type AllProps = IPropsFromState & IConnectedReduxProps & RouteComponentProps;

class Breadcrumb extends React.Component<AllProps, ILocalState> {
  public constructor(props: AllProps) {
    super(props);

    this.state = {
      url: this.props.history.location.pathname.toLowerCase()
    };
  }

  public componentDidMount() {
    this.props.history.listen((location, action) => {
      this.setState({ ...this.state, url: location.pathname.toLowerCase() });
    });
  }

  public render() {
    let currentPath = Paths.Index;

    const reg = /(model[^s])/;
    if (this.state.url.includes('models')) {
      currentPath = Paths.Project;
    }
    if (reg.test(this.state.url)) {
      currentPath = Paths.Model;
    }

    return (
      <div className={styles.content}>
        {currentPath >= Paths.Index ? this.renderCrumb(currentPath === Paths.Index, '/', 'PROJECTS', false) : ''}
        {currentPath >= Paths.Project
          ? this.renderCrumb(
              currentPath === Paths.Project,
              `/project/${this.props.project ? this.props.project.Id : this.props.model ? this.props.model.ProjectId : ''}/models`,
              'MODELS'
            )
          : ''}
        {currentPath >= Paths.Model && this.props.model
          ? this.renderCrumb(
              currentPath === Paths.Model,
              `/project/${this.props.model.ProjectId}/model/${this.props.model.Id}`,
              this.props.model.Name.toLocaleUpperCase()
            )
          : ''}
      </div>
    );
  }

  private renderCrumb(active: boolean, linkTo: string, linkName: string, needArrow: boolean = true) {
    return (
      <span>
        {needArrow ? (
          <span className={styles.arrow}>
            <i className="fa fa-angle-right" />
          </span>
        ) : (
          ''
        )}
        <Link className={`${styles.link} ${active ? styles.active_link : ''}`} to={linkTo}>
          {linkName}
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
