import * as R from 'ramda';
import * as React from 'react';

import { IGitCodeBlob } from 'core/shared/models/Repository/Blob/CodeBlob';
import {
  RecordInfo,
  PageHeader,
} from 'core/shared/view/elements/PageComponents';
import BranchProp from 'core/shared/view/domain/Repository/Blob/CodeBlob/GitBlob/BranchProp/BranchProp';
import HashProp from 'core/shared/view/domain/Repository/Blob/CodeBlob/GitBlob/HashProp/HashProp';
import RepoProp from 'core/shared/view/domain/Repository/Blob/CodeBlob/GitBlob/RepoProp/RepoProp';
import TagProp from 'core/shared/view/domain/Repository/Blob/CodeBlob/GitBlob/TagProp/TagProp';

import styles from './GitBlobView.module.css';

interface ILocalProps {
  blob: IGitCodeBlob;
}

const GitBlobView = ({ blob: { data } }: ILocalProps) => {
  return (
    <div className={styles.root}>
      <div className={styles.title}>
        <PageHeader title="Git" size="medium" />
      </div>
      {!R.isNil(data.commitHash) && (
        <RecordInfo label="Hash">
          <HashProp
            commitHash={data.commitHash}
            remoteRepoUrl={data.remoteRepoUrl}
          />
        </RecordInfo>
      )}
      {!R.isNil(data.isDirty) && (
        <RecordInfo label="Dirty">{data.isDirty ? 'TRUE' : 'FALSE'}</RecordInfo>
      )}
      {!R.isNil(data.branch) && (
        <RecordInfo label="Branch">
          <BranchProp branch={data.branch} remoteRepoUrl={data.remoteRepoUrl} />
        </RecordInfo>
      )}
      {!R.isNil(data.tag) && (
        <RecordInfo label="Tag">
          <TagProp tag={data.tag} remoteRepoUrl={data.remoteRepoUrl} />
        </RecordInfo>
      )}
      <RecordInfo label="Repo">
        <RepoProp remoteRepoUrl={data.remoteRepoUrl} />
      </RecordInfo>
    </div>
  );
};

export default GitBlobView;
