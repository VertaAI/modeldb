import * as React from 'react';

import { AppError } from 'shared/models/Error';
import {
  ICommunication,
  initialCommunication,
  requestingCommunication,
  successfullCommunication,
} from 'shared/utils/redux/communication';
import { matchRemoteData } from 'shared/utils/redux/communication/remoteData';
import DefaultMatchRemoteData from './DefaultMatchRemoteData';

interface ILocalProps<
  Communication extends ICommunication<AppError<any>>,
  Data
> {
  communication: Communication;
  data: Data;
  children: (
    data: Exclude<Data, null | undefined>,
    reloadingCommunication: ICommunication
  ) => any;
}

const DefaultMatchRemoteDataWithReloading = <
  Communication extends ICommunication<AppError<any>>,
  Data
>({
  communication,
  data,
  children,
}: ILocalProps<Communication, Data>) => {
  const [
    isPreviousCommunicationSuccess,
    changePreviousCommunicationStatus,
  ] = React.useState(false);
  if (!isPreviousCommunicationSuccess && communication.isSuccess) {
    changePreviousCommunicationStatus(true);
  }

  if (isPreviousCommunicationSuccess && data) {
    return matchRemoteData(communication, data, {
      notAsked: () =>
        children(data as Exclude<Data, null | undefined>, initialCommunication),
      requesting: () =>
        children(
          data as Exclude<Data, null | undefined>,
          requestingCommunication
        ),
      errorOrNillData: () =>
        children(data as Exclude<Data, null | undefined>, communication),
      success: loadedData =>
        children(
          loadedData as Exclude<Data, null | undefined>,
          successfullCommunication
        ),
    });
  }

  return (
    <DefaultMatchRemoteData communication={communication} data={data}>
      {loadedData => children(loadedData, communication)}
    </DefaultMatchRemoteData>
  );
};

export function usePrevious<T>(value: T) {
  const ref = React.useRef<T>();
  React.useEffect(() => {
    ref.current = value;
  });
  return ref.current as T;
}

export default DefaultMatchRemoteDataWithReloading;
