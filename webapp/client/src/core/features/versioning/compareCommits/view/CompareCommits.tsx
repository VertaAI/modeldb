import React from 'react';

import ShortenedSHA from 'core/shared/view/domain/Versioning/ShortenedSHA/ShortenedSHA';
import * as CommitComponentLocationHelpers from 'core/shared/models/Versioning/CommitComponentLocation';
import { SHA } from 'core/shared/models/Versioning/RepositoryData';
import Placeholder from 'core/shared/view/elements/Placeholder/Placeholder';
import { Diff } from 'core/shared/models/Versioning/Blob/Diff';

import styles from './CompareCommits.module.css';
import DiffView from './DiffView/DiffView';
import { getComparedCommitName } from './DiffView/shared/comparedCommitsNames';
import { diffColors } from './DiffView/shared/styles';

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
                    <div className={styles.header}>
                        <div className={styles.commitsInfo}>
                            <div className={styles.commit}>
                                {getComparedCommitName(
                                    (fromCommitSha, commitSha) => (
                                        <>
                                            <span className={styles.commitTitle}>
                                                {fromCommitSha}{' '}
                                            </span>
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
                        </div>
                        <div className={styles.diffColorKeys}>
                            <div className={styles.diffColorKey}>
                                <div className={styles.diffColorKey__name}>Before</div>
                                <div
                                    className={styles.diffColorKey__value}
                                    style={{ backgroundColor: diffColors.red }}
                                />
                            </div>
                            <div className={styles.diffColorKey}>
                                <div className={styles.diffColorKey__name}>After</div>
                                <div
                                    className={styles.diffColorKey__value}
                                    style={{ backgroundColor: diffColors.green }}
                                />
                            </div>
                        </div>
                    </div>
                    <div className={styles.diff}>
                        {diffs.length > 0 ? (
                            diffs.map(d => (
                                <DiffView
                                    key={CommitComponentLocationHelpers.toPathname(
                                        d.location
                                    )}
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
