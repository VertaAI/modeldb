import React, { useEffect } from 'react';
import { connect } from 'react-redux';
import { Dispatch, bindActionCreators } from 'redux';

import DefaultMatchRemoteData from 'core/shared/view/elements/MatchRemoteDataComponents/DefaultMatchRemoteData';
import {
  actions,
  selectors,
} from 'core/features/repository/compareCommits/store';
import ShortenedSHA from 'core/shared/view/domain/Repository/ShortenedSHA/ShortenedSHA';
import * as DataLocationHelpers from 'core/shared/models/Versioning/DataLocation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import { SHA } from 'core/shared/models/Versioning/RepositoryData';
import { PageCard } from 'core/shared/view/elements/PageComponents';
import Placeholder from 'core/shared/view/elements/Placeholder/Placeholder';
import { IApplicationState } from 'store/store';

import styles from './CompareCommits.module.css';
import DiffView from './DiffView/DiffView';

const mapStateToProps = (state: IApplicationState) => ({
  loadingCommitsDiff: selectors.selectCommunications(state).loadingCommitsDiff,
  diffs: selectors.selectDiff(state),
});

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      loadCommitsDiff: actions.loadCommitsDiff,
      resetLoadingCommitsDiff: actions.loadCommitsDiff.reset,
    },
    dispatch
  );
};

interface ILocalProps {
  repository: IRepository;
  commitASha?: SHA;
  commitBSha: SHA;
}

type AllProps = ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps> &
  ILocalProps;

const CompareCommits: React.FC<AllProps> = ({
  loadCommitsDiff,
  loadingCommitsDiff,
  repository,
  resetLoadingCommitsDiff,
  commitASha,
  commitBSha,
  diffs,
}) => {
  useEffect(() => {
    if (commitASha) {
      loadCommitsDiff({
        repositoryId: repository.id,
        commitASha,
        commitBSha,
      });
    }
  }, [repository.id, commitASha, commitBSha]);

  useEffect(() => {
    return () => {
      resetLoadingCommitsDiff();
    };
  }, []);

  return (
    <PageCard>
      <div className={styles.root}>
        {commitASha ? (
          <>
            <div className={styles.commitsInfo}>
              <div className={styles.commit}>
                <span className={styles.commitTitle}>From Commit SHA: </span>
                <ShortenedSHA sha={commitASha} />
              </div>
              <div className={styles.commit}>
                <span className={styles.commitTitle}>To Commit SHA: </span>
                <ShortenedSHA sha={commitBSha} />
              </div>
            </div>
            <div className={styles.diff}>
              <DefaultMatchRemoteData
                communication={loadingCommitsDiff}
                data={diffs}
              >
                {loadedDiffs =>
                  loadedDiffs.length > 0 ? (
                    loadedDiffs.map(d => (
                      <DiffView
                        key={DataLocationHelpers.toPathname(d.location)}
                        diff={d}
                        comparedCommitsInfo={{
                          commitA: { sha: commitASha },
                          commitB: { sha: commitBSha },
                        }}
                      />
                    ))
                  ) : (
                    <Placeholder>Nothing to compare</Placeholder>
                  )
                }
              </DefaultMatchRemoteData>
            </div>
          </>
        ) : (
          <Placeholder>Nothing to compare</Placeholder>
        )}
      </div>
    </PageCard>
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(React.memo(CompareCommits));
