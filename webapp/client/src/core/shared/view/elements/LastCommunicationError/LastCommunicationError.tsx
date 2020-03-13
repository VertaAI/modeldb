import * as R from 'ramda';
import * as React from 'react';

import { AppError } from 'core/shared/models/Error';
import { ICommunication } from 'core/shared/utils/redux/communication';

interface ILocalProps {
  communications: Array<ICommunication<any>>;
  children: (
    lastError: AppError,
    communication: ICommunication<any>
  ) => React.ReactNode;
}

interface ILocalState {
  lastError: AppError<any> | undefined;
  lastCommunication: ICommunication<any> | undefined;
}

class LastCommunicationError extends React.Component<ILocalProps, ILocalState> {
  public state: ILocalState = {
    lastError: undefined,
    lastCommunication: undefined,
  };

  public componentDidUpdate(prevProps: ILocalProps) {
    const prevAndCurrentCommunicationPairs = R.zip(
      prevProps.communications as any,
      this.props.communications as any
    ) as Array<[ICommunication, ICommunication]>;
    for (const [
      prevCommunication,
      currentCommunication,
    ] of prevAndCurrentCommunicationPairs) {
      if (!prevCommunication.error && currentCommunication.error) {
        this.setState({
          lastError: currentCommunication.error,
          lastCommunication: currentCommunication,
        });
      }
    }
  }

  public render() {
    const { children } = this.props;
    const { lastError, lastCommunication } = this.state;
    return lastError && lastCommunication
      ? children(lastError, lastCommunication)
      : null;
  }
}

export default LastCommunicationError;
