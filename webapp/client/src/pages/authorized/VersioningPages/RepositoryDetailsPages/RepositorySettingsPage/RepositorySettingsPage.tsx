import cn from 'classnames';
import * as React from 'react';
import { useHistory } from 'react-router';
import routes from 'core/shared/routes';
import { useSelector } from 'react-redux';

import { IRepository } from 'core/shared/models/Versioning/Repository';
import { PageCard, PageHeader } from 'core/shared/view/elements/PageComponents';
import { useDeleteRepository } from 'features/versioning/repositories';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';

import RepositoryDetailsPagesLayout from '../shared/RepositoryDetailsPagesLayout/RepositoryDetailsPagesLayout';
import styles from './RepositorySettingsPage.module.css';

interface ILocalProps {
  repository: IRepository;
}

type AllProps = ILocalProps;

const RepositorySettingsPage = (props: AllProps) => {
  const { repository } = props;

  const history = useHistory();
  const workspaceName = useSelector(selectCurrentWorkspaceName);
  const { deleteRepositoryButton, deletingRepository } = useDeleteRepository({
    repository,
    onDeleted: () => {
      history.push(routes.repositories.getRedirectPath({ workspaceName }));
    },
  });

  return (
    <RepositoryDetailsPagesLayout
      repository={repository}
      isDisabledTabs={deletingRepository.isRequesting}
    >
      <PageCard
        additionalClassname={cn(styles.root, {
          [styles.deleting]: deletingRepository.isRequesting,
        })}
      >
        <PageHeader
          title="Settings"
          withoutSeparator={true}
          rightContent={deleteRepositoryButton}
        />
      </PageCard>
    </RepositoryDetailsPagesLayout>
  );
};

export default RepositorySettingsPage;
