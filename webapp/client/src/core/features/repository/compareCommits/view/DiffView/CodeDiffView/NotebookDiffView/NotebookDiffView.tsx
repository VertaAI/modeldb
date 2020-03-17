import * as React from 'react';

import {
  INotebookCodeDiff,
  getPathDatasetBlobDiffFromNotebook,
  getGitCodeDiffFromNotebook,
} from 'core/shared/models/Versioning/Blob/CodeBlob';

import GitDiffView from '../GitDiffView/GitDiffView';
import styles from './NotebookDiffView.module.css';
import PathDatasetBlobDiff from './PathDatasetBlobDiff/PathDatasetBlobDiff';
import { IComparedCommitsInfo } from '../../../types';

interface ILocalProps {
  diff: INotebookCodeDiff;
  comparedCommitsInfo: IComparedCommitsInfo;
}

const NotebookDiffView = ({ diff, comparedCommitsInfo }: ILocalProps) => {
  const pathDatasetBlobDiff = getPathDatasetBlobDiffFromNotebook(diff);
  const gitBlobDiff = getGitCodeDiffFromNotebook(diff);
  return (
    <div className={styles.root}>
      {pathDatasetBlobDiff && (
        <div className={styles.diff}>
          <PathDatasetBlobDiff diff={pathDatasetBlobDiff} />
        </div>
      )}
      {gitBlobDiff && (
        <div className={styles.diff}>
          <GitDiffView
            diff={gitBlobDiff}
            comparedCommitsInfo={comparedCommitsInfo}
          />
        </div>
      )}
    </div>
  );
};

export default NotebookDiffView;
