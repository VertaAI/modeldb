import { PromiseValue } from 'core/shared/utils/types';

import makeMountComponentForIntegratingTest, {
  IOptions as IMakeMountComponentForIntegratingTestOptions,
} from './makeMountComponentForIntegratingTest';

interface IPredefinedData {}

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
  const predefinedData: IPredefinedData = {};
  const makeMountComponentForIntegratingTestResult = await makeMountComponentForIntegratingTest(
    {
      Component: options.Component,
      settings: options.settings,
      updateStoreBeforeMount: async store => {
        if (options.updateStoreBeforeMount) {
          await options.updateStoreBeforeMount(store);
        }
      },
      updateStoreAfterMount: options.updateStoreAfterMount,
      apolloMockedProviderProps: options.apolloMockedProviderProps,
    }
  );
  return { ...makeMountComponentForIntegratingTestResult, predefinedData };
};

export default makeMountComponentWithPredefinedData;
