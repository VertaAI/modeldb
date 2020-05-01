import * as R from 'ramda';
import * as React from 'react';

import { IGitCodeBlob } from 'core/shared/models/Versioning/Blob/CodeBlob';
import BranchProp from 'core/shared/view/domain/Versioning/Blob/CodeBlob/GitBlob/BranchProp/BranchProp';
import HashProp from 'core/shared/view/domain/Versioning/Blob/CodeBlob/GitBlob/HashProp/HashProp';
import RepoProp from 'core/shared/view/domain/Versioning/Blob/CodeBlob/GitBlob/RepoProp/RepoProp';
import TagProp from 'core/shared/view/domain/Versioning/Blob/CodeBlob/GitBlob/TagProp/TagProp';
import makePropertiesTableComponents from 'core/shared/view/domain/Versioning/Blob/PropertiesTable/PropertiesTable';
import { BlobDataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';

interface ILocalProps {
  blob: IGitCodeBlob;
}

const tableComponents = makePropertiesTableComponents<IGitCodeBlob['data']>();

const GitBlobView = ({ blob: { data } }: ILocalProps) => {
  return (
    <BlobDataBox title={'Git'}>
      <tableComponents.Table data={data}>
        <tableComponents.PropDefinition
          title="Hash"
          render={({ data }) => {
            return data.commitHash ? (
              <HashProp
                commitHash={data.commitHash}
                remoteRepoUrl={data.remoteRepoUrl}
              />
            ) : null;
          }}
        />
        <tableComponents.PropDefinition
          title="Dirty"
          render={({ data }) => {
            return data && !R.isNil(data.isDirty) ? (
              <span>{data.isDirty ? 'TRUE' : 'FALSE'}</span>
            ) : null;
          }}
        />
        <tableComponents.PropDefinition
          title="Branch"
          render={({ data }) => {
            return data.branch ? (
              <BranchProp
                branch={data.branch}
                remoteRepoUrl={data.remoteRepoUrl}
              />
            ) : null;
          }}
        />
        <tableComponents.PropDefinition
          title="Tag"
          render={({ data }) => {
            return data.tag ? (
              <TagProp remoteRepoUrl={data.remoteRepoUrl} tag={data.tag} />
            ) : null;
          }}
        />
        <tableComponents.PropDefinition
          title="Repo"
          render={({ data }) => {
            return data.remoteRepoUrl ? (
              <RepoProp remoteRepoUrl={data.remoteRepoUrl} />
            ) : null;
          }}
        />
      </tableComponents.Table>
    </BlobDataBox>
  );
};

export default GitBlobView;
