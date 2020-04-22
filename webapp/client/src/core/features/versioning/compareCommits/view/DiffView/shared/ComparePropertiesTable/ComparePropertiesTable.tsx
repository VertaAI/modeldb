import { DataTypeProvider, Column } from '@devexpress/dx-react-grid';
import Paper from '@material-ui/core/Paper';
import { bind } from 'decko';
import * as R from 'ramda';
import * as React from 'react';

import { ComparedCommitType } from 'core/shared/models/Versioning/Blob/Diff';
import {
  Table as TablePlugin,
  Grid,
  TableHeaderRow,
  TableWrapper,
} from 'core/shared/view/elements/Table/Plugins';

import {
  IComparedCommitsInfo,
  getCssDiffColorByCommitType,
} from '../../../model';
import { getColumnComparedCommitsTitles } from '../comparedCommitsNames';

import styles from './ComparePropertiesTable.module.css';
import withProps from 'core/shared/utils/react/withProps';
import { makeGenericCell } from 'core/shared/view/elements/Table/Templates/Cell/Cell';

interface ILocalProps<T> {
  comparedCommitsInfo: IComparedCommitsInfo;
  A?: T;
  B?: T;
  children: any;
}

const ColumnNames: { [K in ComparedCommitType]: K } & {
  properties: 'properties';
} = {
  A: 'A',
  B: 'B',
  properties: 'properties',
};

interface IState {
  columns: Column[];
  tableColumnExtensions: any[];
}

export interface IPropDefinition<T> {
  title: string;
  isHidden?: boolean;
  type: string;
  getCellStyle?: (
    settings: IPropDefinitionRenderProps<T>
  ) => React.CSSProperties | undefined;
  render(settings: IPropDefinitionRenderProps<T>): React.ReactNode;
}
export interface IPropDefinitionRenderProps<T> {
  comparedCommitType: ComparedCommitType;
  data?: T;
  anotherData?: T;
}

function PropDefinition<T>(props: IPropDefinition<T>) {
  return null;
}

class ComparePropertiesTable<T> extends React.Component<
  ILocalProps<T>,
  IState
> {
  public static PropDefinition = PropDefinition;

  constructor(props: ILocalProps<T>) {
    super(props);
    const columnsComparedCommitsTitles = getColumnComparedCommitsTitles(
      props.comparedCommitsInfo
    );
    this.state = {
      columns: [
        {
          name: ColumnNames.properties,
          title: 'Properties',
          getCellValue: R.identity,
        },
        {
          name: ColumnNames.A,
          title: columnsComparedCommitsTitles.A.title,
          getCellValue: R.identity,
        },
        {
          name: ColumnNames.B,
          title: columnsComparedCommitsTitles.B.title,
          getCellValue: R.identity,
        },
      ],
      tableColumnExtensions: [
        {
          columnName: ColumnNames.properties,
          width: 190,
        },
      ],
    };
  }

  public render() {
    const { columns, tableColumnExtensions } = this.state;
    const propDefinitions: IPropDefinition<
      T
    >[] = this.getPropDefinitions().filter(
      ({ isHidden = false }) => isHidden !== true
    );

    const DiffCell = withProps(makeGenericCell<IPropDefinition<any>, string>())(
      {
        getDataType: (_, row) => row.type,
        getStyle: (column: { name: string }, row: IPropDefinition<any>) => {
          switch (column.name) {
            case ColumnNames.properties: {
              return undefined;
            }
            case ColumnNames.A: {
              const renderProps: IPropDefinitionRenderProps<T> = {
                comparedCommitType: ColumnNames.A,
                data: this.props.A ? this.props.A : undefined,
                anotherData: this.props.B ? this.props.B : undefined,
              };
              const target = propDefinitions.find(
                ({ type }) => type === row.type
              );
              return target && row.getCellStyle
                ? row.getCellStyle(renderProps)
                : undefined;
            }
            case ColumnNames.B: {
              const renderProps: IPropDefinitionRenderProps<T> = {
                comparedCommitType: ColumnNames.B,
                data: this.props.B ? this.props.B : undefined,
                anotherData: this.props.A ? this.props.A : undefined,
              };
              const target = propDefinitions.find(
                ({ type }) => type === row.type
              );
              return target && row.getCellStyle
                ? row.getCellStyle(renderProps)
                : undefined;
            }
          }
        },
      }
    );

    return (
      <Paper>
        <TableWrapper isHeightByContent={true}>
          <Grid rows={propDefinitions} columns={columns}>
            <DataTypeProvider
              formatterComponent={this.ColumnFactory as any}
              for={columns.map(({ name }) => name)}
            />
            <TablePlugin
              columnExtensions={tableColumnExtensions}
              cellComponent={DiffCell}
            />
            <TableHeaderRow />
          </Grid>
        </TableWrapper>
      </Paper>
    );
  }

  @bind
  private getPropDefinitions(): IPropDefinition<T>[] {
    return React.Children.map(
      this.props.children,
      (child: React.ReactElement) => child.props
    );
  }

  @bind
  private ColumnFactory({
    column,
    row: propDefinition,
  }: {
    column: { name: string };
    row: IPropDefinition<T>;
  }) {
    switch (column.name) {
      case ColumnNames.properties: {
        return <span className={styles.title}>{propDefinition.title}</span>;
      }
      case ColumnNames.A: {
        const renderProps: IPropDefinitionRenderProps<T> = {
          comparedCommitType: ColumnNames.A,
          data: this.props.A ? this.props.A : undefined,
          anotherData: this.props.B ? this.props.B : undefined,
        };
        return propDefinition.render(renderProps);
      }
      case ColumnNames.B: {
        const renderProps: IPropDefinitionRenderProps<T> = {
          comparedCommitType: ColumnNames.B,
          data: this.props.B ? this.props.B : undefined,
          anotherData: this.props.A ? this.props.A : undefined,
        };
        return propDefinition.render(renderProps);
      }
    }
  }
}

export function makeHighlightCellBackground<T>() {
  function highlightCellBackground(
    pred: (settings: {
      data: T;
      comparedCommitType: ComparedCommitType;
    }) => boolean
  ) {
    return (settings: { data?: T; comparedCommitType: ComparedCommitType }) => {
      return settings.data &&
        pred({
          data: settings.data,
          comparedCommitType: settings.comparedCommitType,
        })
        ? {
            backgroundColor: getCssDiffColorByCommitType(
              settings.comparedCommitType
            ),
          }
        : undefined;
    };
  }
  return highlightCellBackground;
}

export default function makeComparePropertiesTable<T>(): {
  Table: (props: ILocalProps<T>) => JSX.Element;
  PropDefinition: (props: IPropDefinition<T>) => null;
} {
  return {
    Table: ComparePropertiesTable as any,
    PropDefinition: PropDefinition as any,
  };
}
