import React from 'react';

import * as CommitComponentLocationHelpers from 'shared/models/Versioning/CommitComponentLocation';
import { SHA } from 'shared/models/Versioning/RepositoryData';
import ComparedCommitsInfo from 'shared/view/domain/Versioning/RepositoryData/ComparedCommitsInfo/ComparedCommitsInfo';
import Placeholder from 'shared/view/elements/Placeholder/Placeholder';
import { Diff } from 'shared/models/Versioning/Blob/Diff';

import styles from './CompareCommits.module.css';
import DiffView from './DiffView/DiffView';

interface ILocalProps {
  diffs: Diff[];
  commitASha?: SHA;
  commitBSha: SHA;
}

type AllProps = ILocalProps;

const CompareCommits: React.FC<AllProps> = ({
  diffs,
  commitASha,
  commitBSha,
}) => {
  return (
    <div className={styles.root}>
      {commitASha ? (
        <>
          <ComparedCommitsInfo
            commitASha={commitASha}
            commitBSha={commitBSha}
          />
          <div className={styles.diff}>
            {diffs.length > 0 ? (
              diffs.map(d => (
                <DiffView
                  key={CommitComponentLocationHelpers.toPathname(d.location)}
                  diff={d}
                  comparedCommitsInfo={{
                    commitA: { sha: commitASha },
                    commitB: { sha: commitBSha },
                  }}
                />
              ))
            ) : (
              <Placeholder>Nothing to compare</Placeholder>
            )}
          </div>
        </>
      ) : (
        <Placeholder>Nothing to compare</Placeholder>
      )}
    </div>
  );
};

export default React.memo(CompareCommits);
