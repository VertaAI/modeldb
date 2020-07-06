import { CommunicationActionsToObj } from 'shared/utils/redux/communication';
import { Dataset } from 'shared/models/Dataset';
import { Store } from 'redux';
import { ILoadDatasetActions } from 'features/datasets/store';

const setDatasetInStore = async (store: Store, dataset: Dataset) => {
  const action: CommunicationActionsToObj<
    ILoadDatasetActions,
    any
  >['success'] = {
    payload: {
      dataset,
    },
    type: '@@datasets/LOAD_DATASET_SUCСESS',
  };
  await store.dispatch(action);
};

export default setDatasetInStore;
