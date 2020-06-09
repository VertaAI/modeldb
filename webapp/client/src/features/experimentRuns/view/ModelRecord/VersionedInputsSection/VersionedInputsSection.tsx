import * as R from 'ramda';
import React from 'react';
import { useSelector } from 'react-redux';
import { Link } from 'react-router-dom';

import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';
import { CommitPointerHelpers } from 'core/shared/models/Versioning/RepositoryData';
import ScrollableContainer from 'core/shared/view/elements/ScrollableContainer/ScrollableContainer';
import { IVersionedInputs } from 'core/shared/models/ModelRecord';
import routes from 'core/shared/routes';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';

import { RecordInfo } from '../shared/RecordInfo/RecordInfo';
import Section from '../shared/Section/Section';
import styles from './VersionedInputsSection.module.css';

interface ILocalProps {
  versionedInputs: IVersionedInputs;
}

const VersionedInputsSection: React.FC<ILocalProps> = ({ versionedInputs }) => {
  const currentWorkspaceName = useSelector(selectCurrentWorkspaceName);

  return (
    <Section title="Version">
      <RecordInfo
        label="Repository"
        valueTitle={versionedInputs.repositoryName}
      >
        <Link
          className={styles.link}
          to={routes.repositoryData.getRedirectPath({
            repositoryName: versionedInputs.repositoryName,
            workspaceName: currentWorkspaceName,
          })}
        >
          {versionedInputs.repositoryName}
        </Link>
      </RecordInfo>
      <RecordInfo label="Commit" valueTitle={versionedInputs.commitSha}>
        <Link
          className={styles.link}
          to={routes.repositoryCommit.getRedirectPath({
            repositoryName: versionedInputs.repositoryName,
            workspaceName: currentWorkspaceName,
            commitSha: versionedInputs.commitSha,
          })}
        >
          {versionedInputs.commitSha}
        </Link>
      </RecordInfo>

      <ScrollableContainer maxHeight={180} containerOffsetValue={12}>
        <div>
          {R.keys(versionedInputs.keyLocationMap).map(key => {
            return (
              <RecordInfo
                label={key as string}
                key={key}
                valueTitle={CommitComponentLocation.toPathname(
                  versionedInputs.keyLocationMap[key].location
                )}
              >
                <Link
                  className={styles.link}
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
              </RecordInfo>
            );
          })}
        </div>
      </ScrollableContainer>
    </Section>
  );
};

export default React.memo(VersionedInputsSection);
