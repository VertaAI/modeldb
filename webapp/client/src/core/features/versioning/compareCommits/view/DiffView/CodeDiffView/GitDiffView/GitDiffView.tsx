import * as R from 'ramda';
import * as React from 'react';

import { shortenSHA } from 'core/shared/view/domain/Repository/ShortenedSHA/ShortenedSHA';
import { IGitCodeDiff } from 'core/shared/models/Versioning/Blob/CodeBlob';
import {
  DiffType,
  ComparedCommitType,
  getDiffBlobsData,
} from 'core/shared/models/Versioning/Blob/Diff';
import { getObjsPropsDiff } from 'core/shared/utils/collection';

import { IComparedCommitsInfo } from '../../../types';
import { diffColors } from '../../shared/styles';
import CompareTable from './CompareTable/CompareTable';
import styles from './GitDiffView.module.css';
import HashProp from 'core/shared/view/domain/Repository/Blob/CodeBlob/GitBlob/HashProp/HashProp';
import BranchProp from 'core/shared/view/domain/Repository/Blob/CodeBlob/GitBlob/BranchProp/BranchProp';
import TagProp from 'core/shared/view/domain/Repository/Blob/CodeBlob/GitBlob/TagProp/TagProp';
import RepoProp from 'core/shared/view/domain/Repository/Blob/CodeBlob/GitBlob/RepoProp/RepoProp';

interface ILocalProps {
  diff: IGitCodeDiff;
  comparedCommitsInfo: IComparedCommitsInfo;
}

const GitDiffView = ({ diff, comparedCommitsInfo }: ILocalProps) => {
  const { blobAData: blobA, blobBData: blobB } = getDiffBlobsData(diff);

  return (
    <div className={styles.root}>
      <CompareTable
        blobA={blobA || undefined}
        blobB={blobB}
        diffInfo={getObjsPropsDiff(
          blobA ? blobA.data : ({} as any),
          blobB ? blobB.data : ({} as any)
        )}
        columns={{
          property: {
            title: 'Properties',
            width: 190,
          },
          A: {
            title: `From Commit SHA: ${shortenSHA(
              comparedCommitsInfo.commitA.sha
            )}`,
          },
          B: {
            title: `To Commit SHA: ${shortenSHA(
              comparedCommitsInfo.commitB.sha
            )}`,
          },
        }}
      >
        <CompareTable.PropDefinition
          title="Hash"
          render={({ blobData, diffBlobProperties, type }) => {
            return blobData && blobData.commitHash ? (
              <HashProp
                commitHash={blobData.commitHash}
                remoteRepoUrl={blobData.remoteRepoUrl}
                rootStyles={
                  diffBlobProperties.commitHash
                    ? { backgroundColor: getDiffStyles(diff.diffType, type) }
                    : {}
                }
              />
            ) : null;
          }}
        />
        <CompareTable.PropDefinition
          title="Dirty"
          render={({ blobData, diffBlobProperties, type }) => {
            return blobData && !R.isNil(blobData.isDirty) ? (
              <span
                style={
                  diffBlobProperties.isDirty
                    ? { backgroundColor: getDiffStyles(diff.diffType, type) }
                    : {}
                }
              >
                {blobData.isDirty ? 'TRUE' : 'FALSE'}
              </span>
            ) : null;
          }}
        />
        <CompareTable.PropDefinition
          title="Branch"
          render={({ blobData, diffBlobProperties, type }) => {
            return blobData && blobData.branch ? (
              <BranchProp
                branch={blobData.branch}
                remoteRepoUrl={blobData.remoteRepoUrl}
                rootStyles={
                  diffBlobProperties.branch
                    ? { backgroundColor: getDiffStyles(diff.diffType, type) }
                    : {}
                }
              />
            ) : null;
          }}
        />
        <CompareTable.PropDefinition
          title="Tag"
          render={({ blobData, diffBlobProperties, type }) => {
            return blobData && blobData.tag ? (
              <TagProp
                remoteRepoUrl={blobData.remoteRepoUrl}
                tag={blobData.tag}
                rootStyles={
                  diffBlobProperties.tag
                    ? { backgroundColor: getDiffStyles(diff.diffType, type) }
                    : {}
                }
              />
            ) : null;
          }}
        />
        <CompareTable.PropDefinition
          title="Repo"
          render={({ blobData, diffBlobProperties, type }) => {
            return blobData && blobData.remoteRepoUrl ? (
              <RepoProp
                remoteRepoUrl={blobData.remoteRepoUrl}
                rootStyles={
                  diffBlobProperties.remoteRepoUrl
                    ? { backgroundColor: getDiffStyles(diff.diffType, type) }
                    : {}
                }
              />
            ) : null;
          }}
        />
      </CompareTable>
    </div>
  );
};

const getDiffStyles = (diffType: DiffType, type: ComparedCommitType) => {
  if (diffType === 'deleted') {
    return diffColors.red;
  }
  if (diffType === 'added') {
    return diffColors.green;
  }
  return type === 'A' ? diffColors.red : diffColors.green;
};

export default GitDiffView;
