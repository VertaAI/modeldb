import * as React from 'react';

import { CommitsHistoryLink } from 'core/features/versioning/commitsHistory';
import { CompareChangesLink } from 'core/features/versioning/compareChanges';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import { IFullDataLocationComponents } from 'core/shared/models/Versioning/RepositoryData';

import BranchesAndTagsListContainer from './BranchesAndTagsListContainer/BranchesAndTagsListContainer';
import RepositoryBreadcrumbs from './RepositoryBreadcrumbs/RepositoryBreadcrumbs';
import styles from './Toolbar.module.css';

interface ILocalProps {
  fullDataLocationComponents: IFullDataLocationComponents;
  repository: IRepository;
}

const Toolbar: React.FC<ILocalProps> = ({
  repository,
  fullDataLocationComponents,
}) => {
  return (
    <div className={styles.root}>
      <BranchesAndTagsListContainer repository={repository} />
      <RepositoryBreadcrumbs
        repositoryName={repository.name}
        fullDataLocationComponents={fullDataLocationComponents}
      />
      <div className={styles.actions}>
        <div className={styles.action}>
          <CompareChangesLink
            commitPointer={fullDataLocationComponents.commitPointer}
            repositoryName={repository.name}
          />
        </div>
        <div className={styles.action}>
          <CommitsHistoryLink
            repositoryName={repository.name}
            commitPointer={fullDataLocationComponents.commitPointer}
            location={fullDataLocationComponents.location}
          />
        </div>
      </div>
    </div>
  );
};

export default Toolbar;
