import _ from 'lodash';
import React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';
import ModelRecord from '../../models/ModelRecord';
import { Project } from '../../models/Project';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import loader from '../images/loader.gif';
import Tag from '../TagBlock/Tag';
import tag_styles from '../TagBlock/TagBlock.module.css';
import styles from './Charts.module.css';
import ModelExploration from './ModelExploration/ModelExploration';
import ScatterChart from './ModelSummary/ScatterChart';

let paramList: any = new Set();
const xAxisParams: any = new Set();
export interface IUrlProps {
  projectId: string;
}

interface ILocalState {
  chartData: any;
  selectedMetric: string;
}

interface IPropsFromState {
  projects: Project[] | null | undefined;
  experimentRuns?: ModelRecord[] | undefined;
  loading: boolean;
}

enum Aggregate {
  average,
  sum,
  median,
  variance,
  stdev,
  count
}

type AllProps = RouteComponentProps<IUrlProps> & IPropsFromState & IConnectedReduxProps;
class Charts extends React.Component<AllProps, ILocalState> {
  public flatArray: any;
  public expName: string = '';
  public initialMetric: string = '';
  public initialBarSelection: any;
  public timeProj: any;
  public exploreSelector = { yAxis: {}, xAxis: {}, aggregate: Aggregate };

  public constructor(props: AllProps) {
    super(props);
    this.state = {
      chartData: {},
      selectedMetric: 'test_loss' // imopse a condition that this is the first val in the set
    };
  }

  public render() {
    const { experimentRuns, loading, projects } = this.props;

    if (experimentRuns !== undefined) {
      this.expName = experimentRuns[0].name;
      // this.setState({ selectedMetric: experimentRuns[0].metrics[0].key })
      this.initialBarSelection = { initialMetric: this.state.selectedMetric, initialHyperparam: experimentRuns[0].hyperparameters[0].key };
      this.flatArray = this.dataCompute(experimentRuns);
    }
    if (projects !== undefined && projects !== null) {
      this.timeProj = projects.filter(d => d.name === 'Timeseries')[0];
    }

    return loading ? (
      <img src={loader} className={styles.loader} />
    ) : experimentRuns ? (
      <div>
        <div className={styles.summary_wrapper}>
          {this.timeProj !== undefined && this.timeProj !== null ? (
            <div>
              <p style={{ fontSize: '20px', fontWeight: 500 }}>{this.timeProj.name}</p>
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
          <p style={{ fontSize: '1.2em' }}>Summary Chart</p>
          <div className={styles.chart_selector}>
            Metric :{' '}
            <select name="selected-metric" onChange={this.handleMetricChange} className={styles.dropdown}>
              {[...paramList].map((param: string, i: number) => {
                return (
                  <option key={i} value={param}>
                    {param}
                  </option>
                );
              })}
            </select>
          </div>
          <ScatterChart flatdata={this.flatArray} selectedMetric={this.state.selectedMetric} />
        </div>
        <br />
        <ModelExploration expRuns={experimentRuns} initialSelection={this.initialBarSelection} />
      </div>
    ) : (
      ''
    );
  }

  public handleMetricChange = (event: React.FormEvent<HTMLSelectElement>) => {
    const element = event.target as HTMLSelectElement;
    this.setState({ selectedMetric: element.value });
  };

  // utility functions
  public dataCompute = (arr: ModelRecord[]) => {
    return _.map(arr, obj => {
      const metricField = _.pick(obj, 'startTime', 'metrics');
      const flatMetric: any = { date: metricField.startTime };
      paramList = new Set();
      metricField.metrics.forEach((kvPair: any) => {
        paramList.add(kvPair.key);
        flatMetric[kvPair.key] = kvPair.value;
      });
      return flatMetric;
    });
  };
}

const mapStateToProps = ({ experimentRuns, projects }: IApplicationState) => ({
  projects: projects.data,
  experimentRuns: experimentRuns.data,
  loading: experimentRuns.loading
});

export default connect(mapStateToProps)(Charts);
