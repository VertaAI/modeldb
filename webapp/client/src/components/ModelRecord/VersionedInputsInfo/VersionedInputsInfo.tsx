import * as R from 'ramda';
import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';

import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';
import { CommitPointerHelpers } from 'core/shared/models/Versioning/RepositoryData';
import ScrollableContainer from 'core/shared/view/elements/ScrollableContainer/ScrollableContainer';
import { IVersionedInputs } from 'models/ModelRecord';
import routes from 'routes';
import { IApplicationState } from 'store/store';
import { selectCurrentWorkspaceName } from 'store/workspaces';

import { ModelMeta } from '../shared/ModelMeta/ModelMeta';
import Record from '../shared/Record/Record';
import styles from './VersionedInputsInfo.module.css';

const mapStateToProps = (state: IApplicationState) => ({
  currentWorkspaceName: selectCurrentWorkspaceName(state),
});

interface ILocalProps {
  versionedInputs: IVersionedInputs;
}

type AllProps = ILocalProps & ReturnType<typeof mapStateToProps>;

const VersionedInputsInfo: React.FC<AllProps> = ({
  versionedInputs,
  currentWorkspaceName,
}) => {
  return (
    <ScrollableContainer maxHeight={180} containerOffsetValue={12}>
      <Record label="Version" additionalValueClassName={styles.value}>
        <ModelMeta
          label="Repository"
          valueTitle={versionedInputs.repositoryName}
        >
          <Link
            to={routes.repositoryData.getRedirectPath({
              repositoryName: versionedInputs.repositoryName,
              workspaceName: currentWorkspaceName,
            })}
          >
            {versionedInputs.repositoryName}
          </Link>
        </ModelMeta>
        <ModelMeta label="Commit" valueTitle={versionedInputs.commitSha}>
          <Link
            to={routes.repositoryCommit.getRedirectPath({
              repositoryName: versionedInputs.repositoryName,
              workspaceName: currentWorkspaceName,
              commitSha: versionedInputs.commitSha,
            })}
          >
            {versionedInputs.commitSha}
          </Link>
        </ModelMeta>

        {R.keys(versionedInputs.keyLocationMap).map(key => {
          return (
            <ModelMeta
              label={key as string}
              key={key}
              valueTitle={CommitComponentLocation.toPathname(
                versionedInputs.keyLocationMap[key].location
              )}
            >
              <Link
                to={routes.repositoryDataWithLocation.getRedirectPath({
                  workspaceName: currentWorkspaceName,
                  commitPointer: CommitPointerHelpers.makeFromCommitSha(
                    versionedInputs.commitSha
                  ),
                  repositoryName: versionedInputs.repositoryName,
                  location: versionedInputs.keyLocationMap[key].location,
                  type: 'blob',
                })}
              >
                {CommitComponentLocation.toPathname(
                  versionedInputs.keyLocationMap[key].location
                )}
              </Link>
            </ModelMeta>
          );
        })}
      </Record>
    </ScrollableContainer>
  );
};

export default connect(mapStateToProps)(React.memo(VersionedInputsInfo));
