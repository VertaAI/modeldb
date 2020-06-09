import { bind } from 'decko';
import * as React from 'react';

import Checkbox from 'shared/view/elements/Checkbox/Checkbox';

interface ILocalProps {
  isSelected: boolean;
  selectAllEntities(): void;
  resetEntities(): void;
}

class ToggleAllRowsForBulkDeletion extends React.PureComponent<ILocalProps> {
  public render() {
    return (
      <div
        style={{
          backgroundColor: 'var(--main-color3)',
          borderRight: '1px solid #e2e2e2',
          textAlign: 'center',
          verticalAlign: 'center',
          zIndex: 3,
          paddingTop: '18px',
          width: '58px',
          height: '100%',
        }}
      >
        <Checkbox
          value={this.props.isSelected}
          isRounded={true}
          size="medium"
          theme="red"
          onChange={this.onChange}
        />
      </div>
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
