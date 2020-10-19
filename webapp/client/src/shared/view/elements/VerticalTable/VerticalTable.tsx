import cn from 'classnames';
import React from 'react';

import styles from './VerticalTable.module.css';

interface ILocalProps<T> {
  columnsData: Array<IColumnData<T>>;
  propDefinitions: Array<IPropDefinition<T>>;
}

export interface IColumnData<T> {
  data: T;
  columnTitle?: string;
  dataName?: string | number;
}

export interface IPropDefinition<T> {
  title: string;
  render: (data: T) => React.ReactNode;
  getPropCellStyle?: (data: T) => object;
  type?: string;
  isHidden?: boolean;
  titleDataTest?: string;
  displayOnlyOne?: boolean;
}

const getCellsContainerStyle = (data: any[]) => {
  const columnWidth = `${100 / data.length}%`;
  return {
    display: 'grid',
    gridTemplateColumns: data.map(_ => columnWidth).join(' '),
    flexGrow: '2',
  };
};

function VerticalTable<T>({ columnsData, propDefinitions }: ILocalProps<T>) {
  const cellsContainerStyle = getCellsContainerStyle(columnsData);

  return (
    <div className={styles.root}>
      <TableHeader
        columnsData={columnsData}
        cellsContainerStyle={cellsContainerStyle}
      />
      {propDefinitions
        .filter(propDefinition => !propDefinition.isHidden)
        .map(propDefinition => (
          <Prop
            key={propDefinition.title}
            columnsData={columnsData}
            propDefinition={propDefinition}
            cellsContainerStyle={cellsContainerStyle}
          />
        ))}
    </div>
  );
}

function TableHeader<T>({
  columnsData,
  cellsContainerStyle,
}: {
  columnsData: Array<IColumnData<T>>;
  cellsContainerStyle: object;
}) {
  return (
    <div className={styles.header}>
      <div className={cn(styles.headerPropCell, styles.headerCell)}>
        Properties
      </div>
      <div className={styles.headerCells} style={cellsContainerStyle}>
        {columnsData.map(c => (
          <div
            className={styles.headerCell}
            data-test="header-cell"
            key={c.columnTitle}
          >
            {c.columnTitle}
          </div>
        ))}
      </div>
    </div>
  );
}

export function Prop<T>({
  propDefinition: {
    getPropCellStyle,
    type,
    title,
    render,
    titleDataTest,
    displayOnlyOne,
  },
  columnsData,
  cellsContainerStyle,
}: {
  propDefinition: IPropDefinition<T>;
  columnsData: Array<IColumnData<T>>;
  cellsContainerStyle: object;
}) {
  return (
    <div className={styles.prop} data-test={`prop-${type}`}>
      <div className={styles.propHeaderCell} data-test={titleDataTest}>
        {title}
      </div>
      {displayOnlyOne ? (
        <div className={styles.onlyOneProp}>
          <PropCell
            style={
              getPropCellStyle ? getPropCellStyle(columnsData[0].data) : {}
            }
            dataType={type}
            dataName={columnsData[0].dataName}
          >
            {render(columnsData[0].data)}
          </PropCell>
        </div>
      ) : (
        <div style={cellsContainerStyle}>
          {columnsData.map(column => (
            <PropCell
              key={column.columnTitle + title}
              style={getPropCellStyle ? getPropCellStyle(column.data) : {}}
              dataType={type}
              dataName={column.dataName}
            >
              {render(column.data)}
            </PropCell>
          ))}
        </div>
      )}
    </div>
  );
}

export function PropCell({
  style,
  children,
  dataType,
  dataName,
}: {
  style: object;
  children: React.ReactNode;
  dataType?: string;
  dataName?: string | number;
}) {
  return (
    <div
      style={style}
      className={styles.propCell}
      data-type={dataType}
      data-name={dataName}
    >
      {children}
    </div>
  );
}

export default VerticalTable;
