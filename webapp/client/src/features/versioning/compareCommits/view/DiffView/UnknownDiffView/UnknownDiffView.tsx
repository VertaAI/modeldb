import * as React from 'react';

import { IUnknownBlobDiff } from 'shared/models/Versioning/Blob/Diff';
import matchBy from 'shared/utils/matchBy';
import { JsonView } from 'shared/view/elements/JsonView/JsonView';

import styles from './UnknownDiffView.module.css';

const UnknownDiffView = ({ diff }: { diff: IUnknownBlobDiff }) => {
  return (
    <div className={styles.diff}>
      <div className={styles.diffLeft}>
        {matchBy(diff, 'diffType')({
          added: d => null,
          deleted: d => <JsonView object={d.data.data} />,
          modified: d => <JsonView object={d.data.data} />,
          conflicted: d => <JsonView object={d.data.data} />,
        })}
      </div>
      <div className={styles.diffRight}>
        {matchBy(diff, 'diffType')({
          deleted: d => null,
          added: d => <JsonView object={d.data.data} />,
          modified: d => <JsonView object={d.data.data} />,
          conflicted: d => <JsonView object={d.data.data} />,
        })}
      </div>
    </div>
  );
};

export default UnknownDiffView;
