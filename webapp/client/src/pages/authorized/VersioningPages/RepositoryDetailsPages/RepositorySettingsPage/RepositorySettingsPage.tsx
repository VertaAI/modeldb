import cn from 'classnames';
import * as React from 'react';

import { IRepository } from 'core/shared/models/Versioning/Repository';
import { PageCard, PageHeader } from 'core/shared/view/elements/PageComponents';
import { useDeleteRepository } from 'core/features/versioning/repositories';

import RepositoryDetailsPagesLayout from '../shared/RepositoryDetailsPagesLayout/RepositoryDetailsPagesLayout';
import styles from './RepositorySettingsPage.module.css';

interface ILocalProps {
  repository: IRepository;
}

type AllProps = ILocalProps;

const RepositorySettingsPage = (props: AllProps) => {
  const { repository } = props;

  const { deleteRepositoryButton, isDeletingRepository } = useDeleteRepository({
    repository,
  });

  return (
    <RepositoryDetailsPagesLayout
      repository={repository}
      isDisabledTabs={isDeletingRepository}
    >
      <PageCard
        additionalClassname={cn(styles.root, {
          [styles.deleting]: isDeletingRepository,
        })}
      >
        <PageHeader title="Settings" rightContent={deleteRepositoryButton} />
      </PageCard>
    </RepositoryDetailsPagesLayout>
  );
};

export default RepositorySettingsPage;
