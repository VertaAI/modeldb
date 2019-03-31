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
import ModelSummary from './ModelSummary/ModelSummary';

export interface IUrlProps {
  projectId: string;
}

interface ILocalState {
  selectedMetric: string;
}

interface IPropsFromState {
  projects: Project[] | null | undefined;
  experimentRuns?: ModelRecord[] | undefined;
  loading: boolean;
}

type AllProps = RouteComponentProps<IUrlProps> &
  IPropsFromState &
  IConnectedReduxProps;
class Charts extends React.Component<AllProps, ILocalState> {
  public initialMetric: string = '';
  public initialBarSelection: any;
  public timeProj: any;

  public constructor(props: AllProps) {
    super(props);
    this.state = {
      selectedMetric: 'test_loss', // imopse a condition that this is the first val in the set
    };
  }

  public render() {
    const { experimentRuns, loading, projects } = this.props;

    if (experimentRuns !== undefined) {
      this.initialBarSelection = {
        initialMetric: this.state.selectedMetric,
        initialHyperparam: experimentRuns[0].hyperparameters[0].key,
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
              <p style={{ fontSize: '20px', fontWeight: 500 }}>
                {this.timeProj.name}
              </p>
              <div
                style={{
                  float: 'right',
                  marginTop: '-40px',
                  marginLeft: '-60px',
                  padding: '0 50px 0 0px',
                }}
              >
                <div>
                  <span style={{ fontSize: '0.85em' }}>Author:</span>{' '}
                  {this.timeProj.Author.name}
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

const mapStateToProps = ({ experimentRuns, projects }: IApplicationState) => ({
  experimentRuns: experimentRuns.data,
  projects: projects.data,
  loading: experimentRuns.loading,
});

export default connect(mapStateToProps)(Charts);
