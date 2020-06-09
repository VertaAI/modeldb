import React from 'react';

import { NetworkGraph } from 'core/features/versioning/networkGraph';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import { PageCard, PageHeader } from 'core/shared/view/elements/PageComponents';
import { IWorkspace } from 'core/shared/models/Workspace';

import RepositoryDetailsPagesLayout from '../shared/RepositoryDetailsPagesLayout/RepositoryDetailsPagesLayout';
import styles from './NetworkPage.module.css';

interface ILocalProps {
  repository: IRepository;
  workspaceName: IWorkspace['name'];
}

const NetworkPage: React.FC<ILocalProps> = ({ repository, workspaceName }) => {
  return (
    <RepositoryDetailsPagesLayout repository={repository}>
      <PageCard>
        <PageHeader title="Network graph" />
        <div className={styles.content}>
          <p>
            Timeline of the most recent commits to this repository and its
            network ordered by most recently pushed to.
          </p>
          <div className={styles.graph}>
            <NetworkGraph
              repositoryName={repository.name}
              workspaceName={workspaceName}
            />
          </div>
        </div>
      </PageCard>
    </RepositoryDetailsPagesLayout>
  );
};

export default NetworkPage;
