import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import ComparedEntitesManager from 'components/CompareEntities/ComparedEntitesManager/ComparedEntitesManager';
import { ExperimentRunsTableConfig } from 'core/features/experimentRunsTableConfig';
import Fai from 'core/shared/view/elements/Fai/Fai';
import { Icon } from 'core/shared/view/elements/Icon/Icon';
import ShareLink from 'core/shared/view/elements/ShareLink/ShareLink';
import routes from 'routes';
import { ComparedEntityIds } from 'store/compareEntities';
import { resetExperimentRunsSettings } from 'store/experimentRuns';

import styles from './DashboardActions.module.css';

interface ILocalProps {
  projectId: string;
  isEnableBulkDeletionMenuToggler: boolean;
  onToggleShowingBulkDeletion(): void;
}

interface IActionProps {
  resetExperimentRunsSettings: typeof resetExperimentRunsSettings;
}

interface ILocalState {
  isBulkMenuSelected: boolean;
}

type AllProps = ILocalProps & IActionProps;

class DashboardActions extends React.PureComponent<AllProps> {
  public state: ILocalState = { isBulkMenuSelected: false };
  public render() {
    const { projectId, isEnableBulkDeletionMenuToggler } = this.props;
    const { isBulkMenuSelected } = this.state;
    return (
      <div className={styles.root}>
        <div className={styles.compared_models_manager}>
          <ComparedEntitesManager
            containerId={projectId}
            getCompareUrl={this.getCompareUrl}
          />
        </div>
        <div className={styles.dashboard_settings_container}>
          {isEnableBulkDeletionMenuToggler && (
            <div className={styles.action_container}>
              <div className={styles.dashboard_actione_label}>
                Bulk actions:
              </div>
              <div>
                <Fai
                  theme="primary"
                  variant="outlined"
                  icon={<Icon type="list" />}
                  isActive={isBulkMenuSelected}
                  onClick={this.handleBulkMenuSelection}
                />
              </div>
            </div>
          )}
          <div className={styles.action_container}>
            <div className={styles.dashboard_actione_label}>Reset:</div>
            <div>
              <Fai
                theme="primary"
                variant="outlined"
                icon={<Icon type="share-change" />}
                onClick={this.resetExperimentRunsSettings}
              />
            </div>
          </div>
          <div className={styles.action_container}>
            <div className={styles.dashboard_actione_label}>Share:</div>
            <div>
              <ShareLink link={window.location.href} buttonType="fai" />
            </div>
          </div>
          <div className={styles.experiment_runs_table_config_wrapper}>
            <ExperimentRunsTableConfig />
          </div>
        </div>
      </div>
    );
  }

  @bind
  private handleBulkMenuSelection() {
    const prevSelection = this.state.isBulkMenuSelected;
    this.setState({
      isBulkMenuSelected: !prevSelection,
    });
    this.props.onToggleShowingBulkDeletion();
  }

  @bind
  private getCompareUrl([modelRecordId1, modelRecordId2]: Required<
    ComparedEntityIds
  >) {
    return routes.compareModels.getRedirectPathWithCurrentWorkspace({
      projectId: this.props.projectId,
      modelRecordId1,
      modelRecordId2,
    });
  }

  @bind
  private resetExperimentRunsSettings() {
    this.props.resetExperimentRunsSettings(this.props.projectId);
  }
}

const mapDispatchToProps = (dispatch: Dispatch): IActionProps => {
  return bindActionCreators({ resetExperimentRunsSettings }, dispatch);
};

export default connect(
  null,
  mapDispatchToProps
)(DashboardActions);
