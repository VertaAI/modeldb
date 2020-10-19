import * as React from 'react';

import ConfirmAction from '../ConfirmAction/ConfirmAction';
import Fai from '../Fai/Fai';
import { Icon } from '../Icon/Icon';

interface ILocalProps {
  title?: string;
  confirmText: Exclude<React.ReactNode, null | undefined>;
  faiDataTest?: string;
  isDisabled?: boolean;
  onDelete(): void;
}

class DeleteFAI extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      faiDataTest,
      confirmText,
      isDisabled,
      title,
      onDelete,
    } = this.props;
    return (
      <ConfirmAction
        title={title}
        confirmText={confirmText || 'Are you sure?'}
        cancelButtonText="Cancel"
        confirmButtonText="Delete"
      >
        {(withConfirmAction) => (
          <Fai
            theme="tertiary"
            isDisabled={isDisabled}
            variant="outlined"
            icon={<Icon type="trash" />}
            dataTest={faiDataTest}
            onClick={withConfirmAction(onDelete)}
          />
        )}
      </ConfirmAction>
    );
  }
}

export default DeleteFAI;
