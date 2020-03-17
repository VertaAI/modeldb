import * as React from 'react';

import { ICodeBlob } from 'core/shared/models/Repository/Blob/CodeBlob';
import matchBy from 'core/shared/utils/matchBy';

import styles from './CodeBlobView.module.css';
import GitBlobView from './GitBlobView/GitBlobView';
import NotebookBlobView from './NotebookBlobView/NotebookBlobView';

interface ILocalProps {
  blob: ICodeBlob;
}

const CodeBlobView = ({ blob }: ILocalProps) => {
  return (
    <div className={styles.root}>
      {matchBy(blob.data, 'type')({
        git: gitBlob => <GitBlobView blob={gitBlob} />,
        notebook: notebookBlob => <NotebookBlobView blob={notebookBlob} />,
      })}
    </div>
  );
};

export default CodeBlobView;
