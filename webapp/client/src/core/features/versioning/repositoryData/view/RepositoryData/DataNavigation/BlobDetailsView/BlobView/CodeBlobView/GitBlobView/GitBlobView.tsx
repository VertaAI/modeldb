import * as React from 'react';

import { IGitCodeBlob } from 'core/shared/models/Versioning/Blob/CodeBlob';
import { BlobDataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';
import BranchProp from 'core/shared/view/domain/Versioning/Blob/CodeBlob/GitBlob/BranchProp/BranchProp';
import HashProp from 'core/shared/view/domain/Versioning/Blob/CodeBlob/GitBlob/HashProp/HashProp';
import RepoProp from 'core/shared/view/domain/Versioning/Blob/CodeBlob/GitBlob/RepoProp/RepoProp';
import TagProp from 'core/shared/view/domain/Versioning/Blob/CodeBlob/GitBlob/TagProp/TagProp';

import PropertiesTable from '../../shared/PropertiesTable/PropertiesTable';

interface ILocalProps {
  blob: IGitCodeBlob;
}

const GitBlobView = ({ blob: { data } }: ILocalProps) => {
  return (
    <BlobDataBox title={'Git'}>
      <PropertiesTable
        data={data}
        propDefinitions={[
          {
            title: 'Hash',
            render: ({ commitHash, remoteRepoUrl }) =>
              commitHash ? (
                <HashProp
                  commitHash={commitHash}
                  remoteRepoUrl={remoteRepoUrl}
                />
              ) : null,
          },
          {
            title: 'Dirty',
            render: ({ isDirty }) => <span>{isDirty ? 'TRUE' : 'FALSE'}</span>,
          },
          {
            title: 'Branch',
            render: ({ branch, remoteRepoUrl }) =>
              branch ? (
                <BranchProp branch={branch} remoteRepoUrl={remoteRepoUrl} />
              ) : null,
          },
          {
            title: 'Tag',
            render: ({ tag }) =>
              tag ? (
                <TagProp remoteRepoUrl={data.remoteRepoUrl} tag={tag} />
              ) : null,
          },
          {
            title: 'Repo',
            render: ({ remoteRepoUrl }) =>
              remoteRepoUrl ? <RepoProp remoteRepoUrl={remoteRepoUrl} /> : null,
          },
        ]}
      />
    </BlobDataBox>
  );
};

export default GitBlobView;
