import { CommunicationActionsToObj } from 'core/shared/utils/redux/communication';
import ModelRecord from 'models/ModelRecord';
import { Store } from 'redux';
import { ILoadExperimentRunActions } from 'store/experimentRuns';

const setExperimentRunInStore = async (
  store: Store,
  experimentRun: ModelRecord
) => {
  const action: CommunicationActionsToObj<
    ILoadExperimentRunActions,
    any
  >['success'] = {
    payload: experimentRun,
    type: '@@experimentRuns/LOAD_EXPERIMENT_RUN_SUCСESS',
  };
  await store.dispatch(action);
};

export default setExperimentRunInStore;
