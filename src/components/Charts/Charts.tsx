import React from 'react';
import { connect } from 'react-redux';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import ModelRecord from '../../models/ModelRecord';
import styles from './Charts.module.css';
import SummaryChart from './SummaryChart/SummaryChart';
// import Brush from './Brush/Brush'

interface ILocalState {
  chartData: object;
}

interface IPropsFromState {
  data?: ModelRecord[] | undefined;
  loading: boolean;
}

type AllProps = IConnectedReduxProps & IPropsFromState;
class Charts extends React.Component<AllProps, ILocalState> {
  public constructor(props: AllProps) {
    super(props);
    this.state = {
      chartData: {}
    };
  }

  render() {
    return (
      <div>
        <SummaryChart data={this.state.chartData} />
        {/* <Brush /> */}
      </div>
    );
  }
}

const mapStateToProps = ({ experimentRuns }: IApplicationState) => ({
  data: experimentRuns.data,
  loading: experimentRuns.loading
});

export default connect(mapStateToProps)(Charts);
