import * as _ from 'lodash';
import React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import { fetchExperimentRuns } from '../../store/experiment-runs';
import ModelRecord from '../../models/ModelRecord';
import loader from '../images/loader.gif';
import styles from './Charts.module.css';
import SummaryChart from './SummaryChart/SummaryChart';
// import Brush from './Brush/Brush'

// interface keyValPair {
//   date: Date;
//   [key:string]: number | string | Date;
// }

export interface IUrlProps {
  projectId: string;
}

interface ILocalState {
  chartData: any;
}

interface IPropsFromState {
  experimentRuns?: ModelRecord[] | undefined;
  loading: boolean;
}

type AllProps = RouteComponentProps<IUrlProps> & IPropsFromState & IConnectedReduxProps;
class Charts extends React.Component<AllProps, ILocalState> {
  public flatArray: any;
  public constructor(props: AllProps) {
    super(props);
    this.state = {
      chartData: {}
    };
  }

  public render() {
    const { experimentRuns, loading } = this.props;
    if (experimentRuns !== undefined) {
      this.flatArray = this.dataCompute(experimentRuns);
    }

    return loading ? (
      <img src={loader} className={styles.loader} />
    ) : experimentRuns && this.flatArray ? (
      <div>
        <SummaryChart chartData={this.flatArray} />
      </div>
    ) : (
      ''
    );
  }

  // utility functions
  public dataCompute = (arr: ModelRecord[]) => {
    return _.map(arr, obj => {
      const metricField = _.pick(obj, 'dateCreated', 'metrics');
      const flatMetric: any = { date: metricField.dateCreated };
      metricField.metrics.forEach((kvPair: any) => {
        flatMetric[kvPair.key] = kvPair.value;
      });
      return flatMetric;
    });
  };

  public componentDidMount() {
    this.props.dispatch(fetchExperimentRuns(this.props.match.params.projectId));
  }
}

const mapStateToProps = ({ experimentRuns }: IApplicationState) => ({
  experimentRuns: experimentRuns.data,
  loading: experimentRuns.loading
});

export default connect(mapStateToProps)(Charts);
