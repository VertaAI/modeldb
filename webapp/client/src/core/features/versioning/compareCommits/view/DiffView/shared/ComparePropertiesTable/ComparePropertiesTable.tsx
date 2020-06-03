import { Paper } from '@material-ui/core';
import React from 'react';

import { ComparedCommitType } from 'core/shared/models/Versioning/Blob/Diff';
import VerticalTable, {
  IPropDefinition,
  IColumnData,
} from 'core/shared/view/elements/VerticalTable/VerticalTable';

import { IComparedCommitsInfo } from '../../../model';
import { getColumnComparedCommitsTitles } from '../comparedCommitsNames';

interface ILocalProps<T> {
  A?: T;
  B?: T;
  C?: T;
  comparedCommitsInfo: IComparedCommitsInfo;
  propDefinitions: Array<
    IPropDefinition<IPropDefinitionRenderProps<T | undefined>>
  >;
}

export interface IPropDefinitionRenderProps<T> {
  comparedCommitType: ComparedCommitType;
  data?: T;
  anotherData?: T;
}

function ComparePropertiesTable<T>({
  A,
  B,
  C,
  comparedCommitsInfo,
  propDefinitions,
}: ILocalProps<T>) {
  const columnsData = getColumnsData({
    A,
    B,
    C,
    comparedCommitsInfo,
  });

  return (
    <Paper>
      <VerticalTable
        columnsData={columnsData}
        propDefinitions={propDefinitions}
      />
    </Paper>
  );
}

function getColumnsData<T>({
  A,
  B,
  C,
  comparedCommitsInfo,
}: {
  A?: T;
  B?: T;
  C?: T;
  comparedCommitsInfo: IComparedCommitsInfo;
}): Array<IColumnData<IPropDefinitionRenderProps<T | undefined>>> {
  const columnTitles = getColumnComparedCommitsTitles(comparedCommitsInfo);
  const withCommonBase = Boolean(C);

  const columnDataA = getColumnData({
    columnTitle: columnTitles.A.title,
    data: A,
    anotherData: B,
    comparedCommitType: 'A',
  });

  const columnDataB = getColumnData({
    columnTitle: columnTitles.B.title,
    data: B,
    anotherData: B,
    comparedCommitType: 'B',
  });

  const columnDataC = getColumnData({
    columnTitle: columnTitles.C.title,
    data: C,
    anotherData: A,
    comparedCommitType: 'C',
  });

  if (withCommonBase) {
    return [columnDataA, columnDataC, columnDataC];
  }

  return [columnDataA, columnDataB];
}

function getColumnData<T>({
  data,
  columnTitle,
  anotherData,
  comparedCommitType,
}: {
  columnTitle: string;
  data: T;
  anotherData: T;
  comparedCommitType: ComparedCommitType;
}): IColumnData<IPropDefinitionRenderProps<T>> {
  return {
    columnTitle,
    data: getRenderProps({
      data,
      anotherData,
      comparedCommitType,
    }),
    dataName: comparedCommitType,
  };
}

function getRenderProps<T>({
  data,
  anotherData,
  comparedCommitType,
}: {
  data: T;
  anotherData: T;
  comparedCommitType: ComparedCommitType;
}): IPropDefinitionRenderProps<T> {
  return {
    data,
    anotherData,
    comparedCommitType,
  };
}

export default ComparePropertiesTable;
