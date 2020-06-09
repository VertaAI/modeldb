import * as React from 'react';

import { INotebookCodeDiff } from 'core/shared/models/Versioning/Blob/CodeBlob';
import { MultipleBlobDataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';

import GitDiffView from '../GitDiffView/GitDiffView';
import styles from './NotebookDiffView.module.css';
import PathDatasetBlobDiff from './PathDatasetBlobDiff/PathDatasetBlobDiff';
import { IComparedCommitsInfo } from '../../../model';

interface ILocalProps {
  diff: INotebookCodeDiff;
  comparedCommitsInfo: IComparedCommitsInfo;
}

const NotebookDiffView = ({ diff, comparedCommitsInfo }: ILocalProps) => {
  const pathDatasetBlobDiff = diff.data.path;
  const gitBlobDiff = diff.data.git;
  return (
    <MultipleBlobDataBox title="Notebook">
      <>
        {gitBlobDiff && (
          <div className={styles.diff}>
            <GitDiffView
              diff={gitBlobDiff}
              comparedCommitsInfo={comparedCommitsInfo}
            />
          </div>
        )}
        {pathDatasetBlobDiff && (
          <div className={styles.diff}>
            <PathDatasetBlobDiff diff={pathDatasetBlobDiff} />
          </div>
        )}
      </>
    </MultipleBlobDataBox>
  );
};

export default NotebookDiffView;
