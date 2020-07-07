import * as React from 'react';

import { ICodeBlob } from 'shared/models/Versioning/Blob/CodeBlob';
import matchBy from 'shared/utils/matchBy';

import GitBlobView from './GitBlobView/GitBlobView';
import NotebookBlobView from './NotebookBlobView/NotebookBlobView';

interface ILocalProps {
  blob: ICodeBlob;
}

const CodeBlobView = ({ blob }: ILocalProps) => {
  return matchBy(blob.data, 'type')({
    git: gitBlob => <GitBlobView blob={gitBlob} />,
    notebook: notebookBlob => <NotebookBlobView blob={notebookBlob} />,
  });
};

export default CodeBlobView;
