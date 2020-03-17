import cn from 'classnames';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import { actions, selectors } from 'core/features/repositories';
import { IRepository } from 'core/shared/models/Repository/Repository';
import { initialCommunication } from 'core/shared/utils/redux/communication';
import DeleteFAI from 'core/shared/view/elements/DeleteFAI/DeleteFAI';
import { toastCommunicationError } from 'core/shared/view/elements/Notification/Notification';
import { PageCard, PageHeader } from 'core/shared/view/elements/PageComponents';
import { IApplicationState } from 'store/store';

import RepositoryDetailsPagesLayout from '../shared/RepositoryDetailsPagesLayout/RepositoryDetailsPagesLayout';
import styles from './RepositorySettingsPage.module.css';

interface ILocalProps {
  repository: IRepository;
}

const mapStateToProps = (state: IApplicationState, localProps: ILocalProps) => {
  return {
    deletingRepository:
      selectors.selectCommunications(state).deletingRepositoryById[
        localProps.repository.id
      ] || initialCommunication,
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      deleteRepository: actions.deleteRepository,
      resetDeletingRepository: actions.deleteRepository.reset,
    },
    dispatch
  );
};

type AllProps = ILocalProps &
  ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps>;

const RepositorySettingsPage = (props: AllProps) => {
  const {
    repository,
    deletingRepository,
    deleteRepository,
    resetDeletingRepository,
  } = props;

  React.useEffect(() => {
    if (deletingRepository.error) {
      toastCommunicationError(deletingRepository.error as any);
    }
  }, [deletingRepository.error]);
  React.useEffect(() => {
    return () => {
      resetDeletingRepository({ id: repository.id });
    };
  }, []);

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
          rightContent={
            <DeleteFAI
              confirmText="Are you sure?"
              onDelete={() => deleteRepository({ id: repository.id })}
            />
          }
        />
      </PageCard>
    </RepositoryDetailsPagesLayout>
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(RepositorySettingsPage);
