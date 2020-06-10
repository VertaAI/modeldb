import { IPagination } from 'core/shared/models/Pagination';
import {
  ICommunication,
  initialCommunication,
} from 'core/shared/utils/redux/communication';
import { IApplicationState } from 'store/store';

export const handleDeleteEntities = ({
  changePagination,
  currentPagination,
  loadEntities,
  prevPagination,
  dispatch,
}: {
  prevPagination: IPagination;
  currentPagination: IPagination;
  changePagination: (currentPage: number) => void;
  loadEntities: () => void;
  dispatch: any;
}) => {
  if (prevPagination.currentPage !== currentPagination.currentPage) {
    dispatch(changePagination(currentPagination.currentPage));
  }
  dispatch(loadEntities());
};

export const makeSelectDeletingEntity = ({
  selectBulkDeleting,
  selectEntityDeleting,
  selectEntityIdsForDeleting,
}: {
  selectBulkDeleting: (state: IApplicationState) => ICommunication;
  selectEntityDeleting: (
    state: IApplicationState,
    id: string
  ) => ICommunication | undefined;
  selectEntityIdsForDeleting: (state: IApplicationState) => string[];
}) => {
  return (state: IApplicationState, id: string) => {
    const bulkDeleting = selectBulkDeleting(state);
    if (bulkDeleting.isRequesting) {
      return selectEntityIdsForDeleting(state).includes(id)
        ? bulkDeleting
        : initialCommunication;
    }
    return selectEntityDeleting(state, id) || initialCommunication;
  };
};
