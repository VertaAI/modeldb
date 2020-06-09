import React from 'react';

import { Diff } from 'core/shared/models/Versioning/Blob/Diff';
import * as CommitComponentLocationHelpers from 'core/shared/models/Versioning/CommitComponentLocation';
import matchBy from 'core/shared/utils/matchBy';
import Breadcrumbs from 'core/shared/view/domain/Versioning/Breadcrumbs/Breadcrumbs';

import { IComparedCommitsInfo } from '../model';
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
          <Breadcrumbs
            breadcrumbItems={CommitComponentLocationHelpers.toArray(
              diff.location
            ).map(componentName => ({
              to: '',
              name: componentName,
              isDisabled: true,
            }))}
          />
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
