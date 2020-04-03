import * as R from 'ramda';
import * as React from 'react';

import {
  IGitCodeDiff,
  IGitCodeBlob,
} from 'core/shared/models/Versioning/Blob/CodeBlob';
import { getABData } from 'core/shared/models/Versioning/Blob/Diff';
import { getObjsPropsDiff } from 'core/shared/utils/collection';
import HashProp from 'core/shared/view/domain/Versioning/Blob/CodeBlob/GitBlob/HashProp/HashProp';
import BranchProp from 'core/shared/view/domain/Versioning/Blob/CodeBlob/GitBlob/BranchProp/BranchProp';
import TagProp from 'core/shared/view/domain/Versioning/Blob/CodeBlob/GitBlob/TagProp/TagProp';
import RepoProp from 'core/shared/view/domain/Versioning/Blob/CodeBlob/GitBlob/RepoProp/RepoProp';
import { BlobDataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';

import { IComparedCommitsInfo, getCssDiffColor } from '../../../model';
import makeComparePropertiesTable from '../../shared/ComparePropertiesTable/ComparePropertiesTable';

interface ILocalProps {
  diff: IGitCodeDiff['data'];
  comparedCommitsInfo: IComparedCommitsInfo;
}

const tableComponents = makeComparePropertiesTable<IGitCodeBlob['data']>();

const GitDiffView = ({ diff, comparedCommitsInfo }: ILocalProps) => {
  const { A, B } = getABData(diff);

  const diffBlobProperties = getObjsPropsDiff(
    A ? A.data : ({} as any),
    B ? B.data : ({} as any)
  );

  return (
    <BlobDataBox title="Git">
      <tableComponents.Table
        comparedCommitsInfo={comparedCommitsInfo}
        A={A && A.data}
        B={B && B.data}
      >
        <tableComponents.PropDefinition
          title="Hash"
          render={({ data, type }) => {
            return data && data.commitHash ? (
              <HashProp
                commitHash={data.commitHash}
                remoteRepoUrl={data.remoteRepoUrl}
                rootStyles={
                  diffBlobProperties.commitHash
                    ? { backgroundColor: getCssDiffColor(type) }
                    : {}
                }
              />
            ) : null;
          }}
        />
        <tableComponents.PropDefinition
          title="Dirty"
          render={({ data, type }) => {
            return data && !R.isNil(data.isDirty) ? (
              <span
                data-test="git-dirty"
                style={
                  diffBlobProperties.isDirty
                    ? { backgroundColor: getCssDiffColor(type) }
                    : {}
                }
              >
                {data.isDirty ? 'TRUE' : 'FALSE'}
              </span>
            ) : null;
          }}
        />
        <tableComponents.PropDefinition
          title="Branch"
          render={({ data, type }) => {
            return data && data.branch ? (
              <BranchProp
                branch={data.branch}
                remoteRepoUrl={data.remoteRepoUrl}
                rootStyles={
                  diffBlobProperties.branch
                    ? { backgroundColor: getCssDiffColor(type) }
                    : {}
                }
              />
            ) : null;
          }}
        />
        <tableComponents.PropDefinition
          title="Tag"
          render={({ data, type }) => {
            return data && data.tag ? (
              <TagProp
                remoteRepoUrl={data.remoteRepoUrl}
                tag={data.tag}
                rootStyles={
                  diffBlobProperties.tag
                    ? { backgroundColor: getCssDiffColor(type) }
                    : {}
                }
              />
            ) : null;
          }}
        />
        <tableComponents.PropDefinition
          title="Repo"
          render={({ data, type }) => {
            return data && data.remoteRepoUrl ? (
              <RepoProp
                remoteRepoUrl={data.remoteRepoUrl}
                rootStyles={
                  diffBlobProperties.remoteRepoUrl
                    ? { backgroundColor: getCssDiffColor(type) }
                    : {}
                }
              />
            ) : null;
          }}
        />
      </tableComponents.Table>
    </BlobDataBox>
  );
};

export default GitDiffView;
