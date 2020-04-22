import { DeepPartial } from 'redux';

import flushAllPromises from 'core/shared/utils/tests/integrations/flushAllPromises';
import { PromiseValue } from 'core/shared/utils/types';
import { CurrentUser } from 'models/User';
import { IUserWorkspaces, IWorkspace } from 'models/Workspace';

import { currentUser } from '../mocks/models/users';
import { userWorkspacesWithCurrentUser } from '../mocks/models/workspace';
import makeMountComponentForIntegratingTest, {
  IOptions as IMakeMountComponentForIntegratingTestOptions,
} from './makeMountComponentForIntegratingTest';

interface IPredefinedData {
  currentUser: CurrentUser;
  userWorkspaces: IUserWorkspaces;
  currentWorkspace?: IWorkspace;
}

type IOptions = IMakeMountComponentForIntegratingTestOptions & {
  predefinedData?: Partial<IPredefinedData>;
};

export type IResult = PromiseValue<
  ReturnType<typeof makeMountComponentForIntegratingTest>
> & {
  predefinedData: IPredefinedData;
};

const makeMountComponentWithPredefinedData = async (
  options: IOptions
): Promise<IResult> => {
  const predefinedData: IPredefinedData = {
    currentUser:
      options.predefinedData && options.predefinedData.currentUser
        ? options.predefinedData.currentUser
        : currentUser,
    userWorkspaces:
      options.predefinedData && options.predefinedData.userWorkspaces
        ? options.predefinedData.userWorkspaces
        : userWorkspacesWithCurrentUser,
    currentWorkspace:
      options.predefinedData && options.predefinedData.currentWorkspace,
  };
  const makeMountComponentForIntegratingTestResult = await makeMountComponentForIntegratingTest(
    {
      Component: options.Component,
      settings: options.settings,
      apolloMockedProviderProps: options.apolloMockedProviderProps,
      updateStoreBeforeMount: async store => {
        await flushAllPromises();
        if (options.updateStoreBeforeMount) {
          await options.updateStoreBeforeMount(store);
        }
      },
      updateStoreAfterMount: options.updateStoreAfterMount,
    }
  );
  return { ...makeMountComponentForIntegratingTestResult, predefinedData };
};

export default makeMountComponentWithPredefinedData;
