import { bind } from 'decko';
import * as React from 'react';

import Checkbox from 'core/shared/view/elements/Checkbox/Checkbox';

interface ILocalProps {
  isSelected: boolean;
  selectAllEntities(): void;
  resetEntities(): void;
}

class ToggleAllRowsForBulkDeletion extends React.PureComponent<ILocalProps> {
  public render() {
    return (
      <td
        style={{
          backgroundColor: 'var(--main-color3)',
          borderRight: '1px solid #e2e2e2',
          textAlign: 'center',
          verticalAlign: 'center',
          position: 'sticky',
          left: '0',
          top: '0',
          zIndex: 3,
          paddingTop: '18px',
        }}
      >
        <Checkbox
          value={this.props.isSelected}
          isRounded={true}
          size="medium"
          theme="red"
          onChange={this.onChange}
        />
      </td>
    );
  }

  @bind
  private onChange(value: boolean) {
    if (value) {
      this.props.selectAllEntities();
    } else {
      this.props.resetEntities();
    }
  }
}

export default ToggleAllRowsForBulkDeletion;
