import React from 'react';

import { getComparedCommitName } from 'features/versioning/compareCommits/view/DiffView/shared/comparedCommitsNames';
import { diffColors } from 'features/versioning/compareCommits/view/DiffView/shared/styles';
import { SHA } from 'core/shared/models/Versioning/RepositoryData';

import ShortenedSHA from '../../ShortenedSHA/ShortenedSHA';
import styles from './ComparedCommitsInfo.module.css';

interface ILocalProps {
  commitASha: SHA;
  commitBSha: SHA;
  baseCommitSha?: SHA;
}

const ComparedCommitsInfo: React.FC<ILocalProps> = ({
  commitASha,
  commitBSha,
  baseCommitSha,
}) => {
  return (
    <div className={styles.root}>
      <div className={styles.commitsInfo}>
        <div className={styles.commit}>
          {getComparedCommitName(
            (fromCommitSha, commitSha) => (
              <>
                <span className={styles.commitTitle}>{fromCommitSha} </span>
                <ShortenedSHA
                  sha={commitSha}
                  additionalClassName={styles.commitSha}
                />
              </>
            ),
            'A',
            commitASha
          )}
        </div>
        <div className={styles.commit}>
          {getComparedCommitName(
            (toCommitSha, commitSha) => (
              <>
                <span className={styles.commitTitle}>{toCommitSha} </span>
                <ShortenedSHA
                  sha={commitSha}
                  additionalClassName={styles.commitSha}
                />
              </>
            ),
            'B',
            commitBSha
          )}
        </div>

        {baseCommitSha ? (
          <div className={styles.commit}>
            <span className={styles.commitTitle}>Base commit:</span>
            <ShortenedSHA
              sha={baseCommitSha}
              additionalClassName={styles.commitSha}
            />
          </div>
        ) : null}
      </div>
      <div className={styles.diffColorKeys}>
        <div className={styles.diffColorKey}>
          <div className={styles.diffColorKey__name}>Before</div>
          <div
            className={styles.diffColorKey__value}
            style={{ backgroundColor: diffColors.aDiff }}
          />
        </div>
        <div className={styles.diffColorKey}>
          <div className={styles.diffColorKey__name}>After</div>
          <div
            className={styles.diffColorKey__value}
            style={{ backgroundColor: diffColors.bDiff }}
          />
        </div>
        {baseCommitSha ? (
          <div className={styles.diffColorKey}>
            <div className={styles.diffColorKey__name}>Base</div>
            <div
              className={styles.diffColorKey__value}
              style={{ backgroundColor: diffColors.cDiff }}
            />
          </div>
        ) : null}
      </div>
    </div>
  );
};

export default ComparedCommitsInfo;
