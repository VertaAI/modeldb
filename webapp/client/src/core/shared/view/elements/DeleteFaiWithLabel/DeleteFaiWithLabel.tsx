import { bind } from 'decko';
import * as React from 'react';

import Confirm from '../Confirm/Confirm';
import ConfirmAction from '../ConfirmAction/ConfirmAction';
import FaiWithLabel, {
  IFaiWithLabelLocalProps,
} from '../FaiWithLabel/FaiWithLabel';

interface ILocalProps {
  confirmText: Exclude<React.ReactNode, null | undefined>;
  isDisabled?: boolean;
  theme: IFaiWithLabelLocalProps['theme'];
  dataTest?: string;
  onDelete(): void;
  onHover?(): void;
  onUnhover?(): void;
}

class DeleteFAIWithLabel extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      confirmText,
      theme,
      isDisabled,
      dataTest,
      onHover,
      onUnhover,
      onDelete,
    } = this.props;
    return (
      <ConfirmAction confirmText={confirmText}>
        {withConfirmAction => (
          <FaiWithLabel
            isDisabled={isDisabled}
            iconType="trash"
            theme={theme}
            label="delete"
            dataTest={dataTest}
            onClick={withConfirmAction(onDelete)}
            onHover={onHover}
            onUnhover={onUnhover}
          />
        )}
      </ConfirmAction>
    );
  }
}

export default DeleteFAIWithLabel;
