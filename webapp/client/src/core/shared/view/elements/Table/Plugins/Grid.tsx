import { Grid as GridBase, GridProps } from '@devexpress/dx-react-grid';
import * as React from 'react';
import { Omit } from 'react-redux';

import Root from '../Templates/Root/Root';

class Grid extends React.Component<Omit<GridProps, 'rootComponent'>> {
  public render() {
    const { children, ...restProps } = this.props;
    return (
      <GridBase rootComponent={Root} {...restProps}>
        {children}
      </GridBase>
    );
  }
}

export default Grid;
