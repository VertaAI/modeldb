import React, { useEffect } from 'react';
import { connect } from 'react-redux';
import { Dispatch, bindActionCreators } from 'redux';

import DefaultMatchRemoteData from 'core/shared/view/elements/MatchRemoteDataComponents/DefaultMatchRemoteData';
import {
  actions,
  selectors,
} from 'core/features/versioning/compareCommits/store';
import ShortenedSHA from 'core/shared/view/domain/Versioning/ShortenedSHA/ShortenedSHA';
import * as CommitComponentLocationHelpers from 'core/shared/models/Versioning/CommitComponentLocation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import { SHA } from 'core/shared/models/Versioning/RepositoryData';
import { PageCard } from 'core/shared/view/elements/PageComponents';
import Placeholder from 'core/shared/view/elements/Placeholder/Placeholder';
import { IApplicationState } from 'store/store';

import styles from './CompareCommits.module.css';
import DiffView from './DiffView/DiffView';
import { getComparedCommitName } from './DiffView/shared/comparedCommitsNames';
import { diffColors } from './DiffView/shared/styles';

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
    <div className={styles.root}>
      {commitASha ? (
        <>
          <div className={styles.header}>
            <div className={styles.commitsInfo}>
              <div className={styles.commit}>
                {getComparedCommitName(
                  (fromCommitSha, commitSha) => (
                    <>
                      <span className={styles.commitTitle}>
                        {fromCommitSha}{' '}
                      </span>
                      <ShortenedSHA
                        sha={commitSha}
                        additionalClassName={styles.commitSha}
                      />
                    </>
                  ),
                  'A',
                  commitASha
                )}
              </div>
              <div className={styles.commit}>
                {getComparedCommitName(
                  (toCommitSha, commitSha) => (
                    <>
                      <span className={styles.commitTitle}>{toCommitSha} </span>
                      <ShortenedSHA
                        sha={commitSha}
                        additionalClassName={styles.commitSha}
                      />
                    </>
                  ),
                  'B',
                  commitBSha
                )}
              </div>
            </div>
            <div className={styles.diffColorKeys}>
              <div className={styles.diffColorKey}>
                <div className={styles.diffColorKey__name}>Before</div>
                <div
                  className={styles.diffColorKey__value}
                  style={{ backgroundColor: diffColors.red }}
                />
              </div>
              <div className={styles.diffColorKey}>
                <div className={styles.diffColorKey__name}>After</div>
                <div
                  className={styles.diffColorKey__value}
                  style={{ backgroundColor: diffColors.green }}
                />
              </div>
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
                      key={CommitComponentLocationHelpers.toPathname(
                        d.location
                      )}
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
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(React.memo(CompareCommits));
