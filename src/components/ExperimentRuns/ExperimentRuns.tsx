import * as React from 'react';
import { connect } from 'react-redux';
import { fetchExperimentRuns } from '../../store/experiment-runs';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import { Link } from 'react-router-dom';
import ModelRecord from '../../models/ModelRecord';
import { RouteComponentProps } from 'react-router';
import styles from './ExperimentRuns.module.css';

export interface IUrlProps {
  projectId: string;
}

interface IPropsFromState {
  data?: ModelRecord[] | null;
  loading: boolean;
}

type AllProps = RouteComponentProps<IUrlProps> & IPropsFromState & IConnectedReduxProps;

class ExperimentRuns extends React.Component<AllProps> {
  public componentDidMount() {
    this.props.dispatch(fetchExperimentRuns(this.props.match.params.projectId));
  }

  public render() {
    const notNullModelRecord = this.props.data || [new ModelRecord()];
    return (
      <div>
        <h2>Experiment Runs</h2>
        {notNullModelRecord.map((element: ModelRecord, key: number) => {
          return (
            <div key={key}>
              Model ID:
              <Link to={`/project/${element.ProjectId}/exp-run/${element.Id}`}>
                <h5>{element.Id}</h5>
              </Link>
              <pre>
                <code>{JSON.stringify(element, null, 4)}</code>
              </pre>
            </div>
          );
        })}
      </div>
    );
  }
}

const mapStateToProps = ({ experimentRuns }: IApplicationState) => ({
  data: experimentRuns.data,
  loading: experimentRuns.loading
});

export default connect(mapStateToProps)(ExperimentRuns);
