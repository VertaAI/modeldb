import { CommunicationActionsToObj } from 'core/shared/utils/redux/communication';
import { Project } from 'models/Project';
import { Store } from 'redux';
import { ILoadProjectActions } from 'store/projects';

const setProjectInStore = async (store: Store, project: Project) => {
  const action: CommunicationActionsToObj<
    ILoadProjectActions,
    any
  >['success'] = {
    payload: {
      project,
    },
    type: '@@projects/LOAD_PROJECT_SUCСESS',
  };
  await store.dispatch(action);
};

export default setProjectInStore;
