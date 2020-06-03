import { ICommunication } from 'core/shared/utils/redux/communication';
import { bind } from 'decko';
import _ from 'lodash';
import React from 'react';
import { connect } from 'react-redux';

import Fai from 'core/shared/view/elements/Fai/Fai';
import { Icon } from 'core/shared/view/elements/Icon/Icon';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import ShareLink from 'core/shared/view/elements/ShareLink/ShareLink';
import { Project } from 'models/Project';
import { chartsPageSettings } from 'store/experimentRuns';
import { selectProject } from 'store/projects';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

import styles from './ChartRangeFilter.module.css';

interface IPropsFromState {
  project: Project | undefined;
}

interface ILocalProps {
  projectId: string;
  loadingSequentialChartData: ICommunication;
  paginatedDataLength: number;
  updateIsRangeAppliedAtParent(isRangeApplied: boolean): void;
  resetChartConfigAtParent(): void;
}
type AllProps = ILocalProps & IPropsFromState & IConnectedReduxProps;

interface ILocalState {
  isRangeFilterApplied: boolean;
}

class ChartRangeFilter extends React.Component<AllProps, ILocalState> {
  public state: ILocalState = {
    isRangeFilterApplied: false,
  };

  public render() {
    const { project, loadingSequentialChartData } = this.props;
    return (
      <div className={styles.chart_section_wrapper}>
        <div className={styles.range_filter_container}>
          <div className={styles.chart_actions_panel}>
            <div className={styles.panel_heading}>
              {project ? project.name : 'Unnamed Project'}
            </div>
            <div className={styles.action_wrapper}>
              <div className={styles.action_container}>
                <div className={styles.share_label}>Reset:</div>
                <div className={styles.share_link}>
                  <Fai
                    theme="primary"
                    variant="outlined"
                    icon={<Icon type="share-change" />}
                    onClick={this.resetChartConfig}
                  />
                </div>
              </div>
              <div className={styles.action_container}>
                <div className={styles.share_label}>Share:</div>
                <div className={styles.share_link}>
                  <ShareLink link={window.location.href} buttonType="fai" />
                </div>
              </div>
            </div>
          </div>
          <div className={styles.data_range_container}>
            <div className={styles.pagination_info}>
              Data count:{' '}
              <span className={styles.pagination_value}>
                {this.props.paginatedDataLength}
              </span>
            </div>
            {this.props.paginatedDataLength >=
              chartsPageSettings.datapointLimit && (
              <div className={styles.pageSizeWarnMeta}>
                <div className={styles.pageSizeWarnLogo}>
                  <Icon
                    className={styles.desc_action_icon}
                    type={'exclamation-triangle-lite'}
                  />
                </div>
                Max data limit reached, consider applying filters.
              </div>
            )}
            {loadingSequentialChartData.isRequesting && (
              <div className={styles.loading_container}>
                <Preloader variant="dots" />
                <div className={styles.loading_info}>Loading more data...</div>
              </div>
            )}
          </div>
        </div>
      </div>
    );
  }

  @bind
  private resetChartConfig() {
    this.props.resetChartConfigAtParent();
  }
}

const mapStateToProps = (
  state: IApplicationState,
  localProps: ILocalProps
): IPropsFromState => ({
  project: selectProject(state, localProps.projectId),
});

export default connect(mapStateToProps)(ChartRangeFilter);
