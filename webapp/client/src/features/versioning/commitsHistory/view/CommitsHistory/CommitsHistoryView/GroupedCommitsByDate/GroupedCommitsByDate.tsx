import * as React from 'react';

import { IGroupedCommitsByDate } from 'features/versioning/commitsHistory/store/types';
import { IRepository } from 'core/shared/models/Versioning/Repository';

import Commit from './Commit/Commit';
import styles from './GroupedCommitsByDate.module.css';

interface ILocalProps {
  repositoryName: IRepository['name'];
  data: IGroupedCommitsByDate;
}

const GroupedCommitsByDate = ({ data, repositoryName }: ILocalProps) => {
  return (
    <div className={styles.root}>
      <div className={styles.date}>
        Commits on {data.dateCreated.toLocaleDateString()}
      </div>
      <div className={styles.commits}>
        {data.commits.map(commit => (
          <Commit
            data={commit}
            repositoryName={repositoryName}
            key={commit.sha}
          />
        ))}
      </div>
    </div>
  );
};

export default GroupedCommitsByDate;
