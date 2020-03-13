import { CommunicationActionsToObj } from 'core/shared/utils/redux/communication';
import { Dataset } from 'models/Dataset';
import { Store } from 'redux';
import { ILoadDatasetActions } from 'store/datasets';

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
