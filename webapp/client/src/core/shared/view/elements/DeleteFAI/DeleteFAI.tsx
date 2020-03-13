import { bind } from 'decko';
import * as React from 'react';

import ConfirmAction from '../ConfirmAction/ConfirmAction';
import Fai, { IFaiLocalProps } from '../Fai/Fai';
import { Icon } from '../Icon/Icon';

interface ILocalProps {
  confirmText: Exclude<React.ReactNode, null | undefined>;
  faiDataTest?: string;
  isDisabled?: boolean;
  onDelete(): void;
}

class DeleteFAI extends React.PureComponent<ILocalProps> {
  public render() {
    const { faiDataTest, confirmText, isDisabled, onDelete } = this.props;
    return (
      <ConfirmAction confirmText={confirmText}>
        {withConfirmAction => (
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
