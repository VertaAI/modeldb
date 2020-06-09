import React, { useState, useCallback, useEffect } from 'react';
import { useLocation } from 'react-router';

import { RepositoryData } from 'features/versioning/repositoryData';
import PageCommunicationError from 'core/shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import { PageCard, PageHeader } from 'core/shared/view/elements/PageComponents';
import { AuthorizedLayout } from 'pages/authorized/shared/AuthorizedLayout';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import { RepositoryNavigation } from 'features/versioning/repositoryNavigation';

import RepositoryDetailsPagesLayout from '../shared/RepositoryDetailsPagesLayout/RepositoryDetailsPagesLayout';

interface ILocalProps {
  repository: IRepository;
}

type AllProps = ILocalProps;

const RepositoryPage = (props: AllProps) => {
  const [entityError, changeEntityError] = useState(null);

  const location = useLocation();

  useEffect(() => {
    if (entityError) {
      changeEntityError(null);
    }
  }, [location.pathname]);

  const onShowNotFoundError = useCallback(
    (error: any) => {
      changeEntityError(error);
    },
    [changeEntityError, changeEntityError]
  );

  return entityError ? (
    <AuthorizedLayout>
      <PageCommunicationError error={entityError!} />
    </AuthorizedLayout>
  ) : (
    <RepositoryDetailsPagesLayout repository={props.repository}>
      <PageCard>
        <PageHeader
          title={props.repository.name}
          size="medium"
          withoutSeparator={true}
          rightContent={<RepositoryNavigation />}
        />
        <RepositoryData
          onShowNotFoundError={onShowNotFoundError}
          repository={props.repository}
        />
      </PageCard>
    </RepositoryDetailsPagesLayout>
  );
};

export default RepositoryPage;
