import * as _ from 'lodash';
import React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import { fetchExperimentRuns } from '../../store/experiment-runs';
import ModelRecord from '../../models/ModelRecord';
import { Project } from '../../models/Project';
import loader from '../images/loader.gif';
import styles from './Charts.module.css';
import ScatterChart from './ScatterChart/ScatterChart';
import MetricBar from './MetricBar/MetricBar';
import ExpSubMenu from '../ExpSubMenu/ExpSubMenu';
import Tag from '../TagBlock/Tag';
import tag_styles from '../TagBlock/TagBlock.module.css';

const paramList: any = new Set();
export interface IUrlProps {
  projectId: string;
}

interface ILocalState {
  chartData: any;
}

interface IPropsFromState {
  projects: Project[] | null | undefined;
  experimentRuns?: ModelRecord[] | undefined;
  loading: boolean;
}

type AllProps = RouteComponentProps<IUrlProps> & IPropsFromState & IConnectedReduxProps;
class Charts extends React.Component<AllProps, ILocalState> {
  public flatArray: any;
  public expName: string = '';
  public timeProj: any;
  public constructor(props: AllProps) {
    super(props);
    this.state = {
      chartData: {}
    };
  }

  public render() {
    const { experimentRuns, loading, projects } = this.props;
    if (experimentRuns !== undefined) {
      this.expName = experimentRuns[0].name;
      this.flatArray = this.dataCompute(experimentRuns);
    }
    if (projects !== undefined && projects !== null) {
      this.timeProj = projects.filter(d => d.name === 'Timeseries')[0];
    }

    return loading ? (
      <img src={loader} className={styles.loader} />
    ) : experimentRuns && this.flatArray ? (
      <div>
        <div className={styles.sub_menu}>
          <ExpSubMenu projectId={this.props.match.params.projectId} active="charts" />
        </div>

        <div className={styles.summary_wrapper}>
          {this.timeProj !== undefined && this.timeProj !== null ? (
            <div>
              <h3>{this.timeProj.name}</h3>
              <div style={{ float: 'right', marginTop: '-40px', marginLeft: '-60px', padding: '0 50px 0 0px' }}>
                <div>
                  <span style={{ fontSize: '0.85em' }}>Author:</span> {this.timeProj.Author.name}
                </div>
                <br />
                <div>
                  <span style={{ fontSize: '0.85em' }}>Tags:</span>
                  <div className={tag_styles.tag_block}>
                    <ul className={tag_styles.tags}>
                      {this.timeProj.tags.map((tag: string, i: number) => {
                        return (
                          <li key={i}>
                            <Tag tag={tag} />
                          </li>
                        );
                      })}
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          ) : (
            ''
          )}
          <p style={{ fontSize: '1.15em' }}>Summary Chart</p>
          <ScatterChart data={this.flatArray} paramList={paramList} />
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

const mapStateToProps = ({ experimentRuns, projects }: IApplicationState) => ({
  projects: projects.data,
  experimentRuns: experimentRuns.data,
  loading: experimentRuns.loading
});

export default connect(mapStateToProps)(Charts);
