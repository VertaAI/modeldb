import * as React from 'react';

import {
  INotebookCodeDiff,
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
  const pathDatasetBlobDiff = diff.data.path;
  const gitBlobDiff = diff.data.git;
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
