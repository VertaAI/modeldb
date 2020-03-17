import moment from 'moment';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import DefaultMatchRemoteData from 'core/shared/view/elements/MatchRemoteDataComponents/DefaultMatchRemoteData';
import { CompareCommits } from 'core/features/repository/compareCommits';
import { getRedirectPathToRepositoryDataPage } from 'core/features/repository/repositoryData';
import ShortenedSHA from 'core/shared/view/domain/Repository/ShortenedSHA/ShortenedSHA';
import * as DataLocation from 'core/shared/models/Versioning/DataLocation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  ICommit,
  CommitPointerHelpers,
} from 'core/shared/models/Versioning/RepositoryData';
import Avatar from 'core/shared/view/elements/Avatar/Avatar';
import Button from 'core/shared/view/elements/Button/Button';
import { IApplicationState } from 'store/store';

import { selectors, actions } from '../../store';
import styles from './Commit.module.css';

interface ILocalProps {
  repository: IRepository;
  commitSha: ICommit['sha'];
}

const mapStateToProps = (state: IApplicationState) => {
  return {
    commit: selectors.selectCommit(state).commit,
    loadingCommit: selectors.selectCommunications(state).loadingCommit,
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      loadCommit: actions.loadCommit,
    },
    dispatch
  );
};

type AllProps = ILocalProps &
  ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps>;

const Commit = ({
  repository,
  commitSha,
  loadCommit,
  commit,
  loadingCommit,
}: AllProps) => {
  React.useEffect(() => {
    loadCommit({
      commitSha,
      repositoryId: repository.id,
    });
  }, [repository.id, commitSha]);

  return (
    <div className={styles.root}>
      <DefaultMatchRemoteData data={commit} communication={loadingCommit}>
        {loadedCommit => (
          <div className={styles.content}>
            <div className={styles.commit}>
              <div className={styles.commit__header}>
                <div className={styles.commit__message}>
                  {loadedCommit.message}
                </div>
                <div className={styles.commit__browseFiles}>
                  <Button
                    size="small"
                    to={getRedirectPathToRepositoryDataPage({
                      commitPointer: CommitPointerHelpers.makeFromCommitSha(
                        loadedCommit.sha
                      ),
                      type: 'folder',
                      location: DataLocation.makeRoot(),
                      repositoryName: repository.name,
                    })}
                  >
                    Browse Files
                  </Button>
                </div>
              </div>
              <div className={styles.commit__meta}>
                <div className={styles.commit__metaColumn}>
                  <div className={styles.author}>
                    <Avatar
                      sizeInPx={20}
                      username={loadedCommit.author.username}
                      picture={loadedCommit.author.picture}
                    />
                    <div className={styles.author__username}>
                      {loadedCommit.author.username}
                    </div>
                  </div>
                  <div className={styles.date}>
                    committed {moment(loadedCommit.dateCreated).fromNow()}
                  </div>
                </div>
                <div className={styles.commit__metaColumn}>
                  <div className={styles.shaContainer}>
                    commit <ShortenedSHA sha={loadedCommit.sha} />
                  </div>
                </div>
              </div>
            </div>
            <div className={styles.diff}>
              <CompareCommits
                repository={repository}
                commitASha={
                  loadedCommit.type === 'initial'
                    ? undefined
                    : loadedCommit.parentShas[0]
                }
                commitBSha={loadedCommit.sha}
              />
            </div>
          </div>
        )}
      </DefaultMatchRemoteData>
    </div>
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Commit);
