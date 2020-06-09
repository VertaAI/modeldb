import * as React from 'react';

import { ICodeBlobDiff } from 'core/shared/models/Versioning/Blob/CodeBlob';
import matchBy from 'core/shared/utils/matchBy';

import { IComparedCommitsInfo } from '../../model';
import GitDiffView from './GitDiffView/GitDiffView';
import NotebookDiffView from './NotebookDiffView/NotebookDiffView';

interface ILocalProps {
  diff: ICodeBlobDiff;
  comparedCommitsInfo: IComparedCommitsInfo;
}

const CodeDiffView = ({ diff, comparedCommitsInfo }: ILocalProps) => {
  return matchBy(diff, 'type')({
    git: gitDiff => (
      <GitDiffView
        diff={gitDiff.data}
        comparedCommitsInfo={comparedCommitsInfo}
      />
    ),
    notebook: notebookDiff => (
      <NotebookDiffView
        diff={notebookDiff}
        comparedCommitsInfo={comparedCommitsInfo}
      />
    ),
  });
};

export default CodeDiffView;
