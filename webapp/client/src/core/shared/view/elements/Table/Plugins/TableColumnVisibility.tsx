import { TableColumnVisibility as TableColumnVisibilityBase } from '@devexpress/dx-react-grid';
import { bind } from 'decko';
import * as R from 'ramda';
import * as React from 'react';

import rootStyles from '../Templates/Root/Root.module.css';

interface ILocalProps {
  hiddenColumnNames: string[];
}

const EmptyMessageComponent = () => null;

class TableColumnVisibility extends React.PureComponent<ILocalProps> {
  public componentDidUpdate(prevProps: ILocalProps) {
    if (!R.equals(prevProps.hiddenColumnNames, this.props.hiddenColumnNames)) {
      const newVisibleColumnName = this.getNewVisibleColumnName(
        prevProps.hiddenColumnNames,
        this.props.hiddenColumnNames
      );
      if (newVisibleColumnName) {
        setTimeout(() => {
          const root = document.querySelector(`.${rootStyles.root}`);
          const targetColumn = document.querySelector(
            `td[data-name=${newVisibleColumnName}]`
          );
          if (root && targetColumn) {
            const prevScrollTop = root.scrollTop;
            targetColumn.scrollIntoView({ inline: 'end' });
            root.scrollTop = prevScrollTop;
          }
        }, 1);
      }
    }
  }

  public render() {
    const { hiddenColumnNames } = this.props;
    return (
      <TableColumnVisibilityBase
        emptyMessageComponent={EmptyMessageComponent}
        hiddenColumnNames={hiddenColumnNames}
      />
    );
  }

  @bind
  private getNewVisibleColumnName(
    prevHiddenColumnNames: string[],
    nextHiddenColumnNames: string[]
  ): string | undefined {
    const newVisibleColumnName = R.difference(
      prevHiddenColumnNames,
      nextHiddenColumnNames
    );
    return newVisibleColumnName ? newVisibleColumnName[0] : undefined;
  }
}

export default TableColumnVisibility;
