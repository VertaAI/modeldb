import * as React from 'react';

import { AppError } from 'shared/models/Error';
import { ICommunication } from 'shared/utils/redux/communication';
import { matchRemoteData } from 'shared/utils/redux/communication/remoteData';
import InlineCommunicationError from 'shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';
import Preloader from 'shared/view/elements/Preloader/Preloader';

interface ILocalProps<
  Communication extends ICommunication<AppError<any>>,
  Data
> {
  communication: Communication;
  data: Data;
  children: (success: Exclude<Data, null | undefined>) => any;
}

const DefaultMatchRemoteData = <
  Communication extends ICommunication<AppError<any>>,
  Data
>({
  communication,
  data,
  children,
}: ILocalProps<Communication, Data>) => {
  return matchRemoteData(communication, data, {
    notAsked: () => (
      <div
        style={{
          display: 'flex',
          justifyContent: 'center',
        }}
      >
        <Preloader variant="dots" />
      </div>
    ),
    requesting: () => (
      <div
        style={{
          display: 'flex',
          justifyContent: 'center',
        }}
      >
        <Preloader variant="dots" />
      </div>
    ),
    errorOrNillData: ({ error }) => (
      <div>
        <InlineCommunicationError error={error} />
      </div>
    ),
    success: loadedData =>
      children(loadedData as Exclude<Data, null | undefined>),
  });
};

export default DefaultMatchRemoteData;
