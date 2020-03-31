import * as React from 'react';
import { bindActionCreators, Dispatch } from 'redux';
import { connect } from 'react-redux';

import { selectCurrentWorkspaceName } from 'store/workspaces';
import { ICommit } from 'core/shared/models/Versioning/RepositoryData';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import { IApplicationState } from 'store/store';
import { PageHeader, PageCard } from 'core/shared/view/elements/PageComponents';
import DefaultMatchRemoteData from 'core/shared/view/elements/MatchRemoteDataComponents/DefaultMatchRemoteData';
import ExperimentRunsInfo from 'core/shared/view/domain/Versioning/ExperimentRunsInfo/ExperimentRunsInfo';

import styles from './ExperimentRuns.module.css';
import { selectors, actions } from '../../../store';

interface ILocalProps {
  repositoryId: IRepository['id'];
  commitSha: ICommit['sha'];
}

const mapStateToProps = (state: IApplicationState) => {
  return {
    experimentRunsInfo: selectors.selectCommitExperimentRunsInfo(state),
    loadingCommitExperimentRuns: selectors.selectCommunications(state)
      .loadingCommitExperimentRuns,
    workspaceName: selectCurrentWorkspaceName(state),
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      loadCommitExperimentRuns: actions.loadCommitExperimentRuns,
    },
    dispatch
  );
};

type AllProps = ILocalProps &
  ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps>;

const ExperimentRuns = (props: AllProps) => {
  const {
    loadCommitExperimentRuns,
    repositoryId,
    commitSha,
    loadingCommitExperimentRuns,
    experimentRunsInfo,
    workspaceName,
  } = props;
  React.useEffect(() => {
    loadCommitExperimentRuns({
      repositoryId,
      commitSha,
    });
  }, [repositoryId, commitSha]);

  return (
    <PageCard>
      <div className={styles.root}>
        <PageHeader
          title="Associated experiment runs"
          size="small"
          withoutSeparator={true}
        />
        <DefaultMatchRemoteData
          communication={loadingCommitExperimentRuns}
          data={experimentRunsInfo}
        >
          {loadedExperimentRunsInfo => (
            <div className={styles.content}>
              <ExperimentRunsInfo
                data={loadedExperimentRunsInfo}
                workspaceName={workspaceName}
              />
            </div>
          )}
        </DefaultMatchRemoteData>
      </div>
    </PageCard>
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ExperimentRuns);
