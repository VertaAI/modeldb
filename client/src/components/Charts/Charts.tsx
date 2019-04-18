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
  projects: Project[] | undefined | null;
  experimentRuns?: ModelRecord[] | undefined | null;
  loading: boolean;
}

interface IInitialSelection {
  initialHyperparam: string;
  initialMetric: string;
}

type AllProps = RouteComponentProps<IUrlProps> &
  IPropsFromState &
  IConnectedReduxProps;
class Charts extends React.Component<AllProps> {
  public initialSelection: IInitialSelection = {
    initialHyperparam: 'not available',
    initialMetric: 'not available',
  };
  public currentProject: Project = new Project();

  public render() {
    const { experimentRuns, loading, projects } = this.props;

    if (
      experimentRuns !== undefined &&
      experimentRuns !== null &&
      experimentRuns.length > 0
    ) {
      this.initialSelection = {
        initialHyperparam:
          experimentRuns[0].hyperparameters[0] === undefined ||
          experimentRuns[0].hyperparameters[0] === null
            ? 'not available'
            : experimentRuns[0].hyperparameters[0].key,
        initialMetric:
          experimentRuns[0].metrics[0] === undefined ||
          experimentRuns[0].metrics[0] === null
            ? 'not available'
            : experimentRuns[0].metrics[0].key,
      };
    }
    if (
      projects !== undefined &&
      projects !== null &&
      projects.length > 0 &&
      experimentRuns !== undefined &&
      experimentRuns !== null &&
      experimentRuns.length > 0
    ) {
      this.currentProject = projects.filter(
        d => d.id === experimentRuns[0].projectId
      )[0];
    }

    return loading ? (
      <img src={loader} className={styles.loader} />
    ) : experimentRuns ? (
      <div>
        <div className={styles.summary_wrapper}>
          {this.currentProject !== undefined && this.currentProject !== null ? (
            <div>
              <div className={styles.chartHeader}>
                Summary Chart: <span>{this.currentProject.name}</span>
              </div>

              <div className={styles.chartsBlock}>
                {this.currentProject.Author && (
                  <div className={styles.chartMeta}>
                    <div className={styles.subHeading}>Author: </div>
                    {this.currentProject.Author.name}
                  </div>
                )}
                {this.currentProject.tags && this.currentProject.tags.length ? (
                  <div className={styles.chartMeta}>
                    <span className={styles.subHeading}>Tags:</span>
                    <div className={styles.tagBlock}>
                      <div className={tagStyles.tag_block}>
                        <ul className={tagStyles.tags}>
                          {this.currentProject.tags.map(
                            (tag: string, i: number) => {
                              return (
                                <li key={i}>
                                  <Tag tag={tag} />
                                </li>
                              );
                            }
                          )}
                        </ul>
                      </div>
                    </div>
                  </div>
                ) : (
                  ''
                )}
              </div>
            </div>
          ) : (
            ''
          )}

          <ModelSummary
            experimentRuns={experimentRuns}
            initialYSelection={this.initialSelection.initialMetric}
          />
        </div>
        <br />
        <ModelExploration
          expRuns={experimentRuns}
          initialSelection={this.initialSelection}
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
