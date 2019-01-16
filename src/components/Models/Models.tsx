import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';
import { Dispatch } from 'redux';

import { Model } from 'models/Model';
import Project from 'models/Project';
import { fetchProjectWithModels } from '../../store/project';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import styles from './Models.module.css';

export interface IModelsProps {
  projectId: string;
}

interface IPropsFromState {
  data?: Project | null;
  loading: boolean;
}

type AllProps = RouteComponentProps<IModelsProps> & IPropsFromState & IConnectedReduxProps;

class Models extends React.Component<AllProps> {
  public render() {
    const { data, loading } = this.props;
    console.log(data);
    return <div>{data ? data.Models.map((model, i) => <div key={i}>{model.Id}</div>) : ''}</div>;
  }

  public componentDidMount() {
    this.props.dispatch(fetchProjectWithModels(this.props.match.params.projectId));
  }
}

const mapStateToProps = ({ project }: IApplicationState) => ({
  data: project.data,
  loading: project.loading
});

export default connect(mapStateToProps)(Models);
