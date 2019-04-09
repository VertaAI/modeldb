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

interface IInitialSelection {
  initialHyperparam: string;
  initialMetric: string;
}

type AllProps = RouteComponentProps<IUrlProps> &
  IPropsFromState &
  IConnectedReduxProps;
class Charts extends React.Component<AllProps> {
  public initialSelection: IInitialSelection = {
    initialHyperparam: '',
    initialMetric: '',
  };
  public currentProject: Project = new Project();

  public render() {
    const { experimentRuns, loading, projects } = this.props;

    if (experimentRuns !== undefined) {
      this.initialSelection = {
        initialHyperparam: experimentRuns[0].hyperparameters[0].key,
        initialMetric: experimentRuns[0].metrics[0].key,
      };
    }
    if (
      projects !== undefined &&
      projects !== null &&
      experimentRuns !== undefined
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
              <p className={styles.chartsHeading}>{this.currentProject.name}</p>
              <div className={styles.chartsBlock}>
                <div>
                  <span>Author:</span> {this.currentProject.Author.name}
                </div>
                <br />
                <div>
                  <span>Tags:</span>
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
            </div>
          ) : (
            ''
          )}
          <p style={{ fontSize: '1.2em' }}>Summary Chart</p>
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
