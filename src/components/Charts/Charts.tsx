import _ from 'lodash';
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
import SomeChart from './ScatterChart/SomeChart';
import Shirley from './Shirley';
import BarChart from './sher/Chart';
import ModelExploration from './ModelExploration/ModelExploration';

let paramList: any = new Set();
const xAxisParams: any = new Set();
const enumListAgg: string[] = ['average', 'sum', 'median', 'variance', 'stdev', 'count'];
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
  public timeProj: any;
  public flatFields: any = [];
  public damalFlat: any;
  public exploreSelector = { yAxis: {}, xAxis: {}, aggregate: Aggregate };

  public constructor(props: AllProps) {
    super(props);
    this.state = {
      chartData: {},
      selectedMetric: 'val_acc'
    };
  }

  public render() {
    const { experimentRuns, loading, projects } = this.props;
    if (experimentRuns !== undefined) {
      this.expName = experimentRuns[0].name;
      this.flatArray = this.dataCompute(experimentRuns);
      this.flatFields = this.computeFlatFields(experimentRuns);
      // this.damalFlat = this.generateMetricObjs(experimentRuns);
    }
    if (projects !== undefined && projects !== null) {
      this.timeProj = projects.filter(d => d.name === 'Timeseries')[0];
    }

    return loading ? (
      <img src={loader} className={styles.loader} />
    ) : experimentRuns ? (
      <div>
        <div className={styles.sub_menu}>
          <ExpSubMenu projectId={this.props.match.params.projectId} active="charts" />
        </div>
        <ModelExploration expRuns={experimentRuns} />
        <br />
        <div className={styles.summary_wrapper}>
          <div className={styles.chart_selector}>
            Y Axis: {'  '}
            <select name="selected-yaxis">
              {[...paramList].map((param: string, i: number) => {
                return (
                  <option key={i} value={param}>
                    {param}
                  </option>
                );
              })}
            </select>
          </div>

          <div className={styles.chart_selector}>
            X Axis: {'  '}
            <select name="selected-xaxis">
              {[...xAxisParams].map((param: string, i: number) => {
                return (
                  <option key={i} value={param}>
                    {param}
                  </option>
                );
              })}
            </select>
          </div>
          {/* 
          <div className={styles.chart_selector}>
            Group By: {'  '}
            <select name="selected-groupby">
              {[...paramList].map((param: string, i: number) => {
                return (
                  <option key={i} value={param}>
                    {param}
                  </option>
                );
              })}
            </select>
          </div> */}

          <div className={styles.chart_selector}>
            Aggregate: {'  '}
            <select name="selected-aggregate">
              {enumListAgg.map((param: string, i: number) => {
                return (
                  <option key={i} value={param}>
                    {param}
                  </option>
                );
              })}
            </select>
          </div>
        </div>

        <br />

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
          {/* {console.log(paramList)}
          {console.log(this.flatArray)}
          {console.log(this.flatFields)} */}
          {/* dei yena savadikreane ivan, mudiyala da saami, yenala mudiyala;a */}
          <p style={{ fontSize: '1.15em' }}>Summary Chart</p>
          {/* <div>

</div> */}

          <select name="selected-metric" onChange={this.handleMetricChange}>
            {[...paramList].map((param: string, i: number) => {
              return (
                <option key={i} value={param}>
                  {param}
                </option>
              );
            })}
          </select>

          {/* <Shirley /> */}
          <BarChart flatdata={this.flatArray} selectedMetric={this.state.selectedMetric} />
          {/* <SomeChart data={this.flatArray} selectedMetric={this.state.selectedMetric} /> */}
          {/* <ScatterChart data={this.flatArray} paramList={paramList} /> */}
        </div>

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

  // /react by default handle the proces of enter update

  public handleMetricChange = (event: React.FormEvent<HTMLSelectElement>) => {
    const element = event.target as HTMLSelectElement;
    this.setState({ selectedMetric: element.value });
  };

  // utility functions
  public dataCompute = (arr: ModelRecord[]) => {
    return _.map(arr, obj => {
      const metricField = _.pick(obj, 'startTime', 'metrics');
      // console.log(metricField);
      // const list = ['kgf_32323', 'lkg_637334', 'kgf_dheera'];
      // list.forEach((lstr: string) => {
      //   const matched = lstr.match(/(kgf)/i);
      //   if (matched) {
      //     // console.log(matched.input);
      //   }
      //   return lstr.match(/(kgf)/g);
      // });
      // console.log('damal_shot'.match(/(damal)/g));
      const flatMetric: any = { date: metricField.startTime };
      paramList = new Set();
      metricField.metrics.forEach((kvPair: any) => {
        paramList.add(kvPair.key);
        flatMetric[kvPair.key] = kvPair.value;
      });
      return flatMetric;
    });
  };

  public generateMetricObjs = (arr: any[]) => {
    return _.map(arr, obj => {
      const metricField = _.pick(obj, 'startTime', 'metrics');
      const flatMetric: any = { date: metricField.startTime };
      paramList = new Set();
      metricField.metrics.forEach((kvPair: any) => {
        paramList.add(kvPair.key);
        if (kvPair.key === 'val_acc') {
          flatMetric.high = +kvPair.value;
        } else {
          return '';
        }
      });
      return flatMetric;
    });
  };

  public computeFlatFields = (arr: ModelRecord[]) => {
    return _.map(arr, obj => {
      const fields: any = {};
      if (obj.metrics) {
        obj.metrics.forEach((kvPair: any) => {
          xAxisParams.add(kvPair.key);
          fields[`metrics_${kvPair.key}`] = kvPair.value;
        });
      }
      if (obj.hyperparameters) {
        obj.hyperparameters.forEach((kvPair: any) => {
          xAxisParams.add(kvPair.key);
          fields[`hyperparameters_${kvPair.key}`] = kvPair.value;
        });
      }
      if (obj.datasets) {
        obj.datasets.forEach((kvPair: any) => {
          xAxisParams.add(kvPair.key);
          fields[`dataset_${kvPair.key}`] = kvPair.path;
        });
      }
      if (obj.artifacts) {
        obj.artifacts.forEach((kvPair: any) => {
          xAxisParams.add(kvPair.key);
          fields[`artifact_${kvPair.key}`] = kvPair.path;
        });
      }
      fields.experiment_id = obj.experimentId;
      xAxisParams.add('experimentId');
      fields.project_id = obj.projectId;
      xAxisParams.add('projectId');
      fields.exp_run_id = obj.id;
      xAxisParams.add('id');
      fields.start_time = obj.startTime;
      xAxisParams.add('startTime');
      if (obj.codeVersion) {
        fields.code_version = obj.codeVersion;
        xAxisParams.add('codeVersion');
      }
      if (obj.owner) {
        fields.owner = obj.owner;
        fields.code_version = obj.owner;
      }
      if (obj.tags) {
        obj.tags.forEach((tag: string) => {
          fields[`tag_${tag}`] = tag;
        });
      }
      return fields;
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
