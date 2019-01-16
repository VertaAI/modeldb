import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';
import { Dispatch } from 'redux';

import { Model } from '../../models/Model';
import { fetchModels } from '../../store/model';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import styles from './Models.module.css';

export interface IModelsProps {
  projectId: string;
}

interface IPropsFromState {
  data?: Model[] | null;
  loading: boolean;
}

type AllProps = RouteComponentProps<IModelsProps> & IPropsFromState & IConnectedReduxProps;

class Models extends React.Component<AllProps> {
  public render() {
    const { data, loading } = this.props;
    console.log(data);
    return <div>{data ? data.map((model, i) => <div key={i}>{model.Id}</div>) : ''}</div>;
  }

  public componentDidMount() {
    this.props.dispatch(fetchModels(this.props.match.params.projectId));
  }
}

const mapStateToProps = ({ models }: IApplicationState) => ({
  data: models.data,
  loading: models.loading
});

export default connect(mapStateToProps)(Models);
