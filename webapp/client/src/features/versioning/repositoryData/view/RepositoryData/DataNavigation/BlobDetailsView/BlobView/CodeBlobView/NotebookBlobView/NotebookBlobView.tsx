import * as React from 'react';

import { INotebookCodeBlob } from 'shared/models/Versioning/Blob/CodeBlob';

import PathDatasetComponents from '../../shared/PathDatasetComponents/PathDatasetComponents';
import GitBlobView from '../GitBlobView/GitBlobView';
import styles from './NotebookBlobView.module.css';
import {
  BlobDataBox,
  MultipleBlobDataBox,
} from 'shared/view/domain/Versioning/Blob/BlobBox/BlobBox';

interface ILocalProps {
  blob: INotebookCodeBlob;
}

const NotebookBlobView = ({ blob: { data } }: ILocalProps) => {
  return (
    <MultipleBlobDataBox title="Notebook">
      <>
        {data.gitBlob && (
          <div className={styles.gitBlob}>
            <GitBlobView blob={data.gitBlob} />
          </div>
        )}
        {data.path && (
          <div className={styles.path}>
            <BlobDataBox title="Path">
              <PathDatasetComponents data={[data.path]} />
            </BlobDataBox>
          </div>
        )}
      </>
    </MultipleBlobDataBox>
  );
};

export default NotebookBlobView;
