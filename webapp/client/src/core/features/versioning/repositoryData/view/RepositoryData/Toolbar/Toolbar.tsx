import * as React from 'react';

import { CommitsHistoryLink } from 'core/features/versioning/commitsHistory';
import { CompareChangesLink } from 'core/features/versioning/compareChanges';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import { IFullCommitComponentLocationComponents } from 'core/shared/models/Versioning/RepositoryData';
import Button from 'core/shared/view/elements/Button/Button';
import * as DataLocation from 'core/shared/models/Versioning/CommitComponentLocation';

import BranchesAndTagsListContainer from './BranchesAndTagsListContainer/BranchesAndTagsListContainer';
import RepositoryBreadcrumbs from './RepositoryBreadcrumbs/RepositoryBreadcrumbs';
import styles from './Toolbar.module.css';

interface ILocalProps {
  fullCommitComponentLocationComponents: IFullCommitComponentLocationComponents;
  repository: IRepository;
}

const Toolbar: React.FC<ILocalProps> = ({
  repository,
  fullCommitComponentLocationComponents,
}) => {
  return (
    <div className={styles.root}>
      <BranchesAndTagsListContainer repository={repository} />
      {!DataLocation.isRoot(fullCommitComponentLocationComponents.location) && (
        <div className={styles.breadcrumbs}>
          <RepositoryBreadcrumbs
            repositoryName={repository.name}
            fullCommitComponentLocationComponents={
              fullCommitComponentLocationComponents
            }
          />
        </div>
      )}
      <div className={styles.actions}>
        <CompareChangesLink
          commitPointer={fullCommitComponentLocationComponents.commitPointer}
          repositoryName={repository.name}
        >
          {link =>
            link && (
              <div className={styles.action}>
                <Button to={link} size="small" theme="secondary">
                  Compare
                </Button>
              </div>
            )
          }
        </CompareChangesLink>
        <div className={styles.action} />
        <CommitsHistoryLink
          repositoryName={repository.name}
          commitPointer={fullCommitComponentLocationComponents.commitPointer}
          location={fullCommitComponentLocationComponents.location}
        >
          {link =>
            link && (
              <div className={styles.action}>
                <Button to={link} size="small" theme="secondary">
                  History
                </Button>
              </div>
            )
          }
        </CommitsHistoryLink>
      </div>
    </div>
  );
};

export default Toolbar;
