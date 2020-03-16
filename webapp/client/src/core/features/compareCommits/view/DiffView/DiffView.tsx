import React from 'react';

import { Diff } from 'core/shared/models/Repository/Blob/Diff';
import * as DataLocationHelpers from 'core/shared/models/Repository/DataLocation';
import matchBy from 'core/shared/utils/matchBy';

import { IComparedCommitsInfo } from '../types';
import CodeDiffView from './CodeDiffView/CodeDiffView';
import ConfigDiffView from './ConfigDiffView/ConfigDiffView';
import DatasetDiffView from './DatasetDiffView/DatasetDiffView';
import styles from './DiffView.module.css';
import EnvironmentDiffView from './EnvironmentDiffView/EnvironmentDiffView';
import UnknownDiffView from './UnknownDiffView/UnknownDiffView';

interface ILocalProps {
  diff: Diff;
  comparedCommitsInfo: IComparedCommitsInfo;
}

const DiffView: React.FC<ILocalProps> = ({ diff, comparedCommitsInfo }) => {
  return (
    <div className={styles.root}>
      <div className={styles.header}>
        <span className={styles.location}>
          {DataLocationHelpers.toPathname(diff.location)}
        </span>
      </div>
      {matchBy(diff, 'category')({
        dataset: d => <DatasetDiffView diff={d} />,
        code: d => (
          <CodeDiffView diff={d} comparedCommitsInfo={comparedCommitsInfo} />
        ),
        config: d => (
          <ConfigDiffView diff={d} comparedCommitsInfo={comparedCommitsInfo} />
        ),
        environment: d => (
          <EnvironmentDiffView
            diff={d}
            comparedCommitsInfo={comparedCommitsInfo}
          />
        ),
        unknown: d => <UnknownDiffView diff={d} />,
      })}
    </div>
  );
};

export default DiffView;
