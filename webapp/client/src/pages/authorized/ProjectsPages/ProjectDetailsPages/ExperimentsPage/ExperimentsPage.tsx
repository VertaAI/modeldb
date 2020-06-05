import React from 'react';
import { useParams } from 'react-router';
import routes, { GetRouteParams } from 'routes';

import WithCurrentUserActionsAccesses from 'core/shared/view/domain/WithCurrentUserActionsAccesses/WithCurrentUserActionsAccesses';
import Button from 'core/shared/view/elements/Button/Button';

import ProjectsPagesLayout from '../../shared/ProjectsPagesLayout/ProjectsPagesLayout';
import ProjectPageTabs from '../shared/ProjectPageTabs/ProjectPageTabs';
import styles from './ExperimentsPage.module.css';
import ExperimentsList from 'features/experiments/view/ExperimentsList/ExperimentsList';

const ExperimentsPage = () => {
  const { projectId } = useParams<GetRouteParams<typeof routes.experiments>>();

  return (
    <ProjectsPagesLayout>
      <div className={styles.root}>
        <WithCurrentUserActionsAccesses
          entityType="project"
          entityId={projectId}
          actions={['update']}
        >
          {({ actionsAccesses }) => (
            <ProjectPageTabs
              projectId={projectId}
              rightContent={
                actionsAccesses.update ? (
                  <Button
                    to={routes.experimentCreation.getRedirectPathWithCurrentWorkspace(
                      { projectId }
                    )}
                  >
                    Create Experiment
                  </Button>
                ) : null
              }
            />
          )}
        </WithCurrentUserActionsAccesses>
        <ExperimentsList projectId={projectId} />
      </div>
    </ProjectsPagesLayout>
  );
};

export default ExperimentsPage;
