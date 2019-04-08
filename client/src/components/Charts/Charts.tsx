import _ from 'lodash';
import React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';
import routes, { GetRouteParams } from 'routes';

import ModelRecord from 'models/ModelRecord';
import { Project } from 'models/Project';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

import loader from '../images/loader.gif';
import Tag from '../TagBlock/Tag';
import tagStyles from '../TagBlock/TagBlock.module.css';
import styles from './Charts.module.css';
import ModelExploration from './ModelExploration/ModelExploration';
import ModelSummary from './ModelSummary/ModelSummary';

export type IUrlProps = GetRouteParams<typeof routes.charts>;

interface IPropsFromState {
  projects: Project[] | null | undefined;
  experimentRuns?: ModelRecord[] | undefined;
  loading: boolean;
}

type AllProps = RouteComponentProps<IUrlProps> &
  IPropsFromState &
  IConnectedReduxProps;
class Charts extends React.Component<AllProps> {
  public initialBarSelection: any;
  public timeProj: any;

  public render() {
    const { experimentRuns, loading, projects } = this.props;
    const projectId = this.props.match.params.projectId;

    if (experimentRuns !== undefined) {
      this.initialBarSelection = {
        initialHyperparam: experimentRuns[0].hyperparameters[0].key,
        initialMetric: experimentRuns[0].metrics[0].key,
      };
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
              <p className={styles.chartsHeading}>{this.timeProj.name}</p>
              <div className={styles.chartsBlock}>
                <div>
                  <span>Author:</span> {this.timeProj.Author.name}
                </div>
                <br />
                <div>
                  <span>Tags:</span>
                  <div className={tagStyles.tag_block}>
                    <ul className={tagStyles.tags}>
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
          <ModelSummary
            experimentRuns={experimentRuns}
            initialYSelection={this.initialBarSelection.initialMetric}
          />
        </div>
        <br />
        <ModelExploration
          expRuns={experimentRuns}
          initialSelection={this.initialBarSelection}
        />
      </div>
    ) : (
      ''
    );
  }
}

const mapStateToProps = ({ experimentRuns, projects }: IApplicationState) => ({
  experimentRuns: experimentRuns.data,
  projects: projects.data,
  loading: experimentRuns.loading,
});

export default connect(mapStateToProps)(Charts);
