import * as React from 'react';

import { CommitsHistoryLink } from 'features/versioning/commitsHistory';
import { CompareChangesLink } from 'features/versioning/compareChanges';
import { IRepository } from 'shared/models/Versioning/Repository';
import { IFullCommitComponentLocationComponents } from 'shared/models/Versioning/RepositoryData';
import Button from 'shared/view/elements/Button/Button';
import * as DataLocation from 'shared/models/Versioning/CommitComponentLocation';

import BranchesAndTagsListContainer from './BranchesAndTagsListContainer/BranchesAndTagsListContainer';
import RepositoryBreadcrumbs from './RepositoryBreadcrumbs/RepositoryBreadcrumbs';
import styles from './Toolbar.module.css';

type ILocalProps = {
  fullCommitComponentLocationComponents: IFullCommitComponentLocationComponents;
  repository: IRepository;
} & Omit<
  React.ComponentProps<typeof BranchesAndTagsListContainer>,
  'commitPointer'
>;

const Toolbar: React.FC<ILocalProps> = ({
  repository,
  fullCommitComponentLocationComponents,
  ...branchesAndTagsListProps
}) => {
  return (
    <div className={styles.root}>
      <BranchesAndTagsListContainer
        repository={repository}
        {...branchesAndTagsListProps}
        commitPointer={fullCommitComponentLocationComponents.commitPointer}
      />
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
