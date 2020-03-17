import * as React from 'react';

import { INotebookCodeBlob } from 'core/shared/models/Versioning/Blob/CodeBlob';
import { PageHeader } from 'core/shared/view/elements/PageComponents';

import PathDatasetComponents from '../../shared/PathDatasetComponents/PathDatasetComponents';
import GitBlobView from '../GitBlobView/GitBlobView';
import styles from './NotebookBlobView.module.css';

interface ILocalProps {
  blob: INotebookCodeBlob;
}

const NotebookBlobView = ({ blob: { data } }: ILocalProps) => {
  return (
    <div className={styles.root}>
      {data.gitBlob && (
        <div className={styles.gitBlob}>
          <GitBlobView blob={data.gitBlob} />
        </div>
      )}
      {data.path && (
        <div className={styles.path}>
          <div className={styles.path__title}>
            <PageHeader title="Path" size="medium" />
          </div>
          <PathDatasetComponents data={data.path.components} />
        </div>
      )}
    </div>
  );
};

export default NotebookBlobView;
