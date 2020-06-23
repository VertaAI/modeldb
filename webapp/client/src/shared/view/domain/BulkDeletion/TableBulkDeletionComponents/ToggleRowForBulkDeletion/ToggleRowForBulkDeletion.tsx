import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import Checkbox from 'shared/view/elements/Checkbox/Checkbox';

import styles from './ToggleRowForBulkDeletion.module.css';

interface ILocalProps {
  id: string;
  isSelected: boolean;
  selectEntity(id: string): void;
  unselectEntity(id: string): void;
}

class ToggleRowForBulkDeletion extends React.PureComponent<ILocalProps> {
  public render() {
    const { isSelected } = this.props;
    return (
      <div
        className={cn(styles.root, {
          [styles.selected_row_view]: isSelected,
        })}
      >
        <Checkbox
          value={isSelected}
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
      this.props.selectEntity(this.props.id);
    } else {
      this.props.unselectEntity(this.props.id);
    }
  }
}

export default ToggleRowForBulkDeletion;
