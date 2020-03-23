import * as React from 'react';

import { IUnknownBlobDiff } from 'core/shared/models/Versioning/Blob/Diff';
import matchBy from 'core/shared/utils/matchBy';
import { JsonView } from 'core/shared/view/elements/JsonView/JsonView';

import styles from './UnknownDiffView.module.css';

const UnknownDiffView = ({ diff }: { diff: IUnknownBlobDiff }) => {
  return (
    <div className={styles.diff}>
      <div className={styles.diffLeft}>
        {matchBy(diff, 'diffType')({
          added: d => null,
          deleted: d => <JsonView object={d.blob} />,
          updated: d => <JsonView object={d.blobA} />,
        })}
      </div>
      <div className={styles.diffRight}>
        {matchBy(diff, 'diffType')({
          deleted: d => null,
          added: d => <JsonView object={d.blob} />,
          updated: d => <JsonView object={d.blobB} />,
        })}
      </div>
    </div>
  );
};

export default UnknownDiffView;
