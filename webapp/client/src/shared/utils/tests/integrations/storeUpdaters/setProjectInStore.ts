import { CommunicationActionsToObj } from 'shared/utils/redux/communication';
import { Project } from 'shared/models/Project';
import { Store } from 'redux';
import { ILoadProjectActions } from 'features/projects/store';

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
