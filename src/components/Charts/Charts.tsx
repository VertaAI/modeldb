import * as _ from 'lodash';
import React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import { fetchExperimentRuns } from '../../store/experiment-runs';
import ModelRecord from '../../models/ModelRecord';
import loader from '../images/loader.gif';
import styles from './Charts.module.css';
// import SummaryChart from './SummaryChart/SummaryChart';
import ScatterChart from './ScatterChart/ScatterChart';
import MetricBar from './MetricBar/MetricBar';
// import Brush from './Brush/Brush'

// interface keyValPair {
//   date: Date;
//   [key:string]: number | string | Date;
// }

const paramList: any = new Set();
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
  public expName: string = '';
  public constructor(props: AllProps) {
    super(props);
    this.state = {
      chartData: {}
    };
  }

  public render() {
    const { experimentRuns, loading } = this.props;
    if (experimentRuns !== undefined) {
      this.expName = experimentRuns[0].name;
      this.flatArray = this.dataCompute(experimentRuns);
    }

    return loading ? (
      <img src={loader} className={styles.loader} />
    ) : experimentRuns && this.flatArray ? (
      <div>
        <div className={styles.summary_wrapper}>
          <h3>{this.expName}</h3>
          <h5>Summary Chart</h5>
          <ScatterChart data={this.flatArray} />
        </div>
        <br />
        <div className={styles.summary_wrapper}>
          <h5>Explore Metrics</h5>
          {[...paramList].map((param: string, i: number) => {
            return <MetricBar key={i} data={param} />;
          })}
        </div>
      </div>
    ) : (
      ''
    );
  }

  // utility functions
  public dataCompute = (arr: ModelRecord[]) => {
    return _.map(arr, obj => {
      const metricField = _.pick(obj, 'startTime', 'metrics');
      const flatMetric: any = { date: metricField.startTime };
      metricField.metrics.forEach((kvPair: any) => {
        paramList.add(kvPair.key);
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
