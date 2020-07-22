import * as React from 'react';

import {
  ICommunication,
  requestingCommunication,
  successfullCommunication,
  makeErrorCommunication,
} from 'shared/utils/redux/communication';
import { PromiseValue } from 'shared/utils/types';
import normalizeError from 'shared/utils/normalizeError';
import { AppError } from 'shared/models/Error';

export default function useRequest<T extends (...args: any[]) => Promise<any>>(
  api: T
): {
  communication: ICommunication<AppError>;
  data: PromiseValue<ReturnType<T>> | null;
  refetch: () => void;
} {
  const [communication, setCommunication] = React.useState<ICommunication>(
    requestingCommunication
  );
  const [data, setData] = React.useState<PromiseValue<ReturnType<T>> | null>(
    null
  );

  function callApi() {
    api()
      .then(result => {
        setCommunication(successfullCommunication);
        setData(result);
      })
      .catch(error =>
        setCommunication(makeErrorCommunication(normalizeError(error)))
      );
  }
  React.useEffect(() => {
    callApi();
  }, []);

  function refetch() {
    setCommunication(requestingCommunication);
    callApi();
  }

  return { communication, data, refetch };
}
