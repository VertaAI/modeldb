import moment from 'moment';
import * as React from 'react';
import { connect } from 'react-redux';

import { CompareCommits } from 'features/versioning/compareCommits';
import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  ICommit,
  CommitPointerHelpers,
} from 'core/shared/models/Versioning/RepositoryData';
import ShortenedSHA from 'core/shared/view/domain/Versioning/ShortenedSHA/ShortenedSHA';
import Avatar from 'core/shared/view/elements/Avatar/Avatar';
import Button from 'core/shared/view/elements/Button/Button';
import DefaultMatchRemoteData from 'core/shared/view/elements/MatchRemoteDataComponents/DefaultMatchRemoteData';
import routes from 'core/shared/routes';
import { IApplicationState } from 'store/store';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';
import { PageCard, PageHeader } from 'core/shared/view/elements/PageComponents';
import { RepositoryNavigation } from 'features/versioning/repositoryNavigation';
import AssociatedExperimentRuns from 'core/shared/view/domain/Versioning/AssociatedExperimentRuns/AssociatedExperimentRuns';

import styles from './Commit.module.css';
import { useCommitDetailsQuery } from '../../store/commitDetails/useCommitDetails';

interface ILocalProps {
  repository: IRepository;
  commitSha: ICommit['sha'];
}

const mapStateToProps = (state: IApplicationState) => {
  return {
    currentWorkspaceName: selectCurrentWorkspaceName(state),
  };
};

type AllProps = ILocalProps & ReturnType<typeof mapStateToProps>;

const Commit = ({ repository, commitSha, currentWorkspaceName }: AllProps) => {
  const { communication: loadingCommitDetails, data } = useCommitDetailsQuery({
    repositoryId: repository.id,
    commitSha: commitSha,
  });

  return (
    <PageCard>
      <PageHeader
        title={repository.name}
        rightContent={<RepositoryNavigation />}
        withoutSeparator={true}
      />
      <DefaultMatchRemoteData data={data} communication={loadingCommitDetails}>
        {({ commit, diffs, experimentRuns }) => (
          <div className={styles.content}>
            <div className={styles.browseFiles}>
              <Button
                size="small"
                theme="secondary"
                to={routes.repositoryDataWithLocation.getRedirectPath({
                  workspaceName: currentWorkspaceName,
                  commitPointer: CommitPointerHelpers.makeFromCommitSha(
                    commit.sha
                  ),
                  type: 'folder',
                  location: CommitComponentLocation.makeRoot(),
                  repositoryName: repository.name,
                })}
              >
                Browse Files
              </Button>
            </div>
            <div className={styles.commit}>
              <div className={styles.commit__header}>
                <div className={styles.commit__message}>{commit.message}</div>
              </div>
              <div className={styles.commit__meta}>
                <div className={styles.commit__metaColumn}>
                  <div className={styles.author}>
                    <Avatar
                      sizeInPx={28}
                      username={commit.author.username}
                      picture={commit.author.picture}
                    />
                    <div className={styles.author__username}>
                      {commit.author.username}
                    </div>
                  </div>
                  <div className={styles.date}>
                    committed {moment(commit.dateCreated).fromNow()}
                  </div>
                </div>
                <div className={styles.commit__metaColumn}>
                  <div className={styles.shaContainer}>
                    commit <ShortenedSHA sha={commit.sha} />
                  </div>
                </div>
              </div>
            </div>
            <div className={styles.diff}>
              <CompareCommits
                diffs={diffs}
                commitASha={commit.parentSha}
                commitBSha={commit.sha}
              />
            </div>
            <div className={styles.experimentRuns}>
              <AssociatedExperimentRuns
                data={experimentRuns}
                workspaceName={currentWorkspaceName}
              />
            </div>
          </div>
        )}
      </DefaultMatchRemoteData>
    </PageCard>
  );
};

export default connect(mapStateToProps)(Commit);
