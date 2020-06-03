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

import { IComparedCommitsInfo } from '../../../model';
import makeComparePropertiesTable, {
  makeHighlightCellBackground,
} from '../../shared/ComparePropertiesTable/ComparePropertiesTable';

interface ILocalProps {
  diff: IGitCodeDiff['data'];
  comparedCommitsInfo: IComparedCommitsInfo;
}

const tableComponents = makeComparePropertiesTable<IGitCodeBlob['data']>();
const highlightCellBackground = makeHighlightCellBackground<
  IGitCodeBlob['data']
>();

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
          type="hash"
          getCellStyle={highlightCellBackground(({ data }) =>
            Boolean(data.commitHash && diffBlobProperties.commitHash)
          )}
          render={({ data }) => {
            return data && data.commitHash ? (
              <HashProp
                commitHash={data.commitHash}
                remoteRepoUrl={data.remoteRepoUrl}
              />
            ) : null;
          }}
        />
        <tableComponents.PropDefinition
          title="Dirty"
          type="dirty"
          getCellStyle={highlightCellBackground(
            ({ data }) => !R.isNil(data.isDirty) && diffBlobProperties.isDirty
          )}
          render={({ data }) => {
            return data && !R.isNil(data.isDirty) ? (
              <span data-test="git-dirty">
                {data.isDirty ? 'TRUE' : 'FALSE'}
              </span>
            ) : null;
          }}
        />
        <tableComponents.PropDefinition
          title="Branch"
          type="branch"
          getCellStyle={highlightCellBackground(({ data }) =>
            Boolean(data.branch && diffBlobProperties.branch)
          )}
          render={({ data }) => {
            return data && data.branch ? (
              <BranchProp
                branch={data.branch}
                remoteRepoUrl={data.remoteRepoUrl}
              />
            ) : null;
          }}
        />
        <tableComponents.PropDefinition
          title="Tag"
          type="tag"
          getCellStyle={highlightCellBackground(({ data }) =>
            Boolean(data.tag && diffBlobProperties.tag)
          )}
          render={({ data }) => {
            return data && data.tag ? (
              <TagProp remoteRepoUrl={data.remoteRepoUrl} tag={data.tag} />
            ) : null;
          }}
        />
        <tableComponents.PropDefinition
          title="Repo"
          type="repo"
          getCellStyle={highlightCellBackground(({ data }) =>
            Boolean(data.remoteRepoUrl && diffBlobProperties.remoteRepoUrl)
          )}
          render={({ data }) => {
            return data && data.remoteRepoUrl ? (
              <RepoProp remoteRepoUrl={data.remoteRepoUrl} />
            ) : null;
          }}
        />
      </tableComponents.Table>
    </BlobDataBox>
  );
};

export default GitDiffView;
