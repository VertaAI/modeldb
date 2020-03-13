import { Plugin, Getter } from '@devexpress/dx-react-core';
import * as React from 'react';

interface ILocalProps {
  getHeaderRow: (row: any, ref: any) => any;
  getBodyRow: (row: any, ref: any) => any;
}

type AllProps = ILocalProps;

export interface IRowSelectionPluginGetters {
  getHeaderRow: (row: any, ref: any) => any;
  getBodyRow: (row: any, ref: any) => any;
}

export default class RowSelection extends React.Component<AllProps> {
  public render() {
    const { getHeaderRow: headerRow, getBodyRow: bodyRow } = this.props;
    return (
      <Plugin name="RowSelection">
        <Getter name="getHeaderRow" value={headerRow} />
        <Getter name="getBodyRow" value={bodyRow} />
      </Plugin>
    );
  }
}
