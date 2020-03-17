import React, { useState, useCallback, useEffect } from 'react';
import { connect } from 'react-redux';
import { useLocation } from 'react-router';
import { bindActionCreators, Dispatch } from 'redux';

import { selectors } from 'core/features/versioning/repositories';
import { RepositoryData } from 'core/features/versioning/repositoryData';
import PageCommunicationError from 'core/shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import { PageCard } from 'core/shared/view/elements/PageComponents';
import { AuthorizedLayout } from 'pages/authorized/shared/AuthorizedLayout';
import { IApplicationState } from 'store/store';

import RepositoryDetailsPagesLayout from '../shared/RepositoryDetailsPagesLayout/RepositoryDetailsPagesLayout';

interface ILocalProps {}

const mapStateToProps = (state: IApplicationState, props: any) => {
  const repository = selectors.selectRepositoryByName(
    state,
    props.match.params.repositoryName
  )!;
  return {
    repository,
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators({}, dispatch);
};

type AllProps = ILocalProps &
  ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps>;

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
        <RepositoryData
          onShowNotFoundError={onShowNotFoundError}
          repository={props.repository}
        />
      </PageCard>
    </RepositoryDetailsPagesLayout>
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(RepositoryPage);
