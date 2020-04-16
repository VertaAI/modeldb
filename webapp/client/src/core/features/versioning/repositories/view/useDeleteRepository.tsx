import * as React from 'react';
import { useSelector, useDispatch } from 'react-redux';

import { actions, selectors } from 'core/features/versioning/repositories';
import {
  IRepository,
  isRepositoryDeletingAvailable,
} from 'core/shared/models/Versioning/Repository';
import { initialCommunication } from 'core/shared/utils/redux/communication';
import DeleteFAI from 'core/shared/view/elements/DeleteFAI/DeleteFAI';
import { toastCommunicationError } from 'core/shared/view/elements/Notification/Notification';
import { IApplicationState } from 'store/store';

const useDeleteRepository = (props: { repository: IRepository }) => {
  const { repository } = props;

  const dispatch = useDispatch();
  const deletingRepository = useSelector(
    (state: IApplicationState) =>
      selectors.selectCommunications(state).deletingRepositoryById[
        repository.id
      ] || initialCommunication
  );

  React.useEffect(() => {
    if (deletingRepository.error) {
      toastCommunicationError(deletingRepository.error);
    }
  }, [deletingRepository.error]);
  React.useEffect(() => {
    return () => {
      dispatch(actions.deleteRepository.reset({ id: repository.id }));
    };
  }, []);

  const deleteRepositoryButton = 
    <DeleteFAI
      confirmText="Are you sure?"
      onDelete={() => dispatch(actions.deleteRepository({ id: repository.id }))}
    />;

  return {
    deleteRepositoryButton,
    isDeletingRepository: deletingRepository.isRequesting,
  };
};

export default useDeleteRepository;
