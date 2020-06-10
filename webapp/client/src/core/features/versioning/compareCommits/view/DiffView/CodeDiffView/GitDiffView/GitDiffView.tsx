import * as R from 'ramda';
import * as React from 'react';

import {
  IGitCodeDiff,
  IGitCodeBlob,
} from 'core/shared/models/Versioning/Blob/CodeBlob';
import { getABCData } from 'core/shared/models/Versioning/Blob/Diff';
import { getObjsPropsDiff } from 'core/shared/utils/collection';
import HashProp from 'core/shared/view/domain/Versioning/Blob/CodeBlob/GitBlob/HashProp/HashProp';
import BranchProp from 'core/shared/view/domain/Versioning/Blob/CodeBlob/GitBlob/BranchProp/BranchProp';
import TagProp from 'core/shared/view/domain/Versioning/Blob/CodeBlob/GitBlob/TagProp/TagProp';
import RepoProp from 'core/shared/view/domain/Versioning/Blob/CodeBlob/GitBlob/RepoProp/RepoProp';
import { BlobDataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';

import { IComparedCommitsInfo } from '../../../model';
import { makeHighlightCellBackground } from '../../shared/makeHighlightCellBackground';
import ComparePropertiesTable from '../../shared/ComparePropertiesTable/ComparePropertiesTable';

interface ILocalProps {
  diff: IGitCodeDiff['data'];
  comparedCommitsInfo: IComparedCommitsInfo;
}

const highlightCellBackground = makeHighlightCellBackground<
  IGitCodeBlob['data'] | undefined
>();

const GitDiffView = ({ diff, comparedCommitsInfo }: ILocalProps) => {
  const { A, B, C } = getABCData(diff);

  const diffBlobProperties = getObjsPropsDiff(
    A ? A.data : ({} as any),
    B ? B.data : ({} as any)
  );

  return (
    <BlobDataBox title="Git">
      <ComparePropertiesTable
        comparedCommitsInfo={comparedCommitsInfo}
        A={A && A.data}
        B={B && B.data}
        C={C && C.data}
        propDefinitions={[
          {
            title: 'Hash',
            type: 'hash',
            getPropCellStyle: highlightCellBackground(({ data }) =>
              Boolean(data && data.commitHash && diffBlobProperties.commitHash)
            ),
            render: ({ data }) =>
              data && data.commitHash ? (
                <HashProp
                  commitHash={data.commitHash}
                  remoteRepoUrl={data.remoteRepoUrl}
                />
              ) : null,
          },
          {
            title: 'Dirty',
            type: 'dirty',
            getPropCellStyle: highlightCellBackground(({ data }) =>
              Boolean(
                data && !R.isNil(data.isDirty) && diffBlobProperties.isDirty
              )
            ),
            render: ({ data }) =>
              data && !R.isNil(data.isDirty) ? (
                <span data-test="git-dirty">
                  {data.isDirty ? 'TRUE' : 'FALSE'}
                </span>
              ) : null,
          },
          {
            title: 'Branch',
            type: 'branch',
            getPropCellStyle: highlightCellBackground(({ data }) =>
              Boolean(data && data.branch && diffBlobProperties.branch)
            ),
            render: ({ data }) =>
              data && data.branch ? (
                <BranchProp
                  branch={data.branch}
                  remoteRepoUrl={data.remoteRepoUrl}
                />
              ) : null,
          },
          {
            title: 'Tag',
            type: 'tag',
            getPropCellStyle: highlightCellBackground(({ data }) =>
              Boolean(data && data.tag && diffBlobProperties.tag)
            ),
            render: ({ data }) =>
              data && data.tag ? (
                <TagProp remoteRepoUrl={data.remoteRepoUrl} tag={data.tag} />
              ) : null,
          },
          {
            title: 'Repo',
            type: 'repo',
            getPropCellStyle: highlightCellBackground(({ data }) =>
              Boolean(
                data && data.remoteRepoUrl && diffBlobProperties.remoteRepoUrl
              )
            ),
            render: ({ data }) =>
              data && data.remoteRepoUrl ? (
                <RepoProp remoteRepoUrl={data.remoteRepoUrl} />
              ) : null,
          },
        ]}
      />
    </BlobDataBox>
  );
};

export default GitDiffView;
