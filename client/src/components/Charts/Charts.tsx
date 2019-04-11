import _ from 'lodash';
import React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';
import routes, { GetRouteParams } from 'routes';

import Preloader from 'components/shared/Preloader/Preloader';
import ModelRecord from 'models/ModelRecord';
import { Project } from 'models/Project';
import {
  selectExperimentRuns,
  selectIsLoadingExperimentRuns,
} from 'store/experiment-runs';
import { selectProjects } from 'store/projects';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

import Tag from '../shared/TagBlock/Tag';
import tagStyles from '../shared/TagBlock/TagBlock.module.css';
import styles from './Charts.module.css';
import ModelExploration from './ModelExploration/ModelExploration';
import ModelSummary from './ModelSummary/ModelSummary';

export type IUrlProps = GetRouteParams<typeof routes.charts>;

interface ILocalState {
  selectedMetric: string;
}

interface IPropsFromState {
  projects?: Project[] | null;
  experimentRuns: ModelRecord[] | null;
  loading: boolean;
}

type AllProps = RouteComponentProps<IUrlProps> &
  IPropsFromState &
  IConnectedReduxProps;
class Charts extends React.Component<AllProps, ILocalState> {
  public state: ILocalState = { selectedMetric: 'test_loss' };
  private initialMetric: string = '';
  private initialBarSelection: any;
  private timeProj: any;

  public render() {
    const { experimentRuns, loading, projects } = this.props;
    const projectId = this.props.match.params.projectId;

    if (experimentRuns) {
      this.initialBarSelection = {
        initialHyperparam: experimentRuns[0].hyperparameters[0].key,
        initialMetric: this.state.selectedMetric,
      };
    }
    if (projects !== undefined && projects !== null) {
      this.timeProj = projects.filter(d => d.name === 'Timeseries')[0];
    }

    return loading ? (
      <Preloader variant="dots" />
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
          <p>Summary Chart</p>
          <ModelSummary experimentRuns={experimentRuns} />
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

const mapStateToProps = (state: IApplicationState): IPropsFromState => ({
  experimentRuns: selectExperimentRuns(state),
  projects: selectProjects(state),
  loading: selectIsLoadingExperimentRuns(state),
});

export default connect(mapStateToProps)(Charts);
