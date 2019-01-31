import ModelRecord from 'models/ModelRecord';
import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';
import styles from './ExperimentRuns.module.css';
import { fetchExperimentRuns } from '../../store/experiment-runs';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';

export interface IUrlProps {
  projectId: string;
}

interface IPropsFromState {
  experimentRuns?: ModelRecord[] | null;
  loading: boolean;
}

type AllProps = RouteComponentProps<IUrlProps> & IPropsFromState & IConnectedReduxProps;
// type AllProps = IPropsFromState & IConnectedReduxProps;

class ExperimentRuns extends React.Component<AllProps> {
  public componentDidMount() {
    this.props.dispatch(fetchExperimentRuns(this.props.match.params.projectId));
  }

  public render() {
    return (
      <div>
        <h2>Experiment Runs</h2>
        <pre>
          <code>{JSON.stringify(this.props.experimentRuns, null, 4)}</code>
        </pre>
      </div>
    );
  }
}

const mapStateToProps = ({ experimentRuns }: IApplicationState) => ({
  loading: experimentRuns.loading,
  experimentRuns: experimentRuns.data
});

export default connect(mapStateToProps)(ExperimentRuns);
