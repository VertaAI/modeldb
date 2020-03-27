import * as React from 'react';

import { shortenSHA } from 'core/shared/view/domain/Versioning/ShortenedSHA/ShortenedSHA';
import { IConfigBlobDiff } from 'core/shared/models/Versioning/Blob/ConfigBlob';
import {
  DiffType,
  ComparedCommitType,
  getCommitDataFromNullableDiffs,
} from 'core/shared/models/Versioning/Blob/Diff';
import HyperparameterItem from 'core/shared/view/domain/Versioning/Blob/ConfigBlob/HyperparameterItem/HyperparameterItem';
import HyperparameterSetItem from 'core/shared/view/domain/Versioning/Blob/ConfigBlob/HyperparameterSetItem/HyperparameterSetItem';

import { IComparedCommitsInfo } from '../../types';
import { diffColors } from '../shared/styles';
import CompareTable, { IRow } from './CompareTable/CompareTable';
import sortArrayByAnotherArrayKeys from '../shared/sortArrayByAnotherArrayKeys/sortArrayByAnotherArrayKeys';

interface ILocalProps {
  diff: IConfigBlobDiff;
  comparedCommitsInfo: IComparedCommitsInfo;
}

const ConfigDiffView = ({ diff, comparedCommitsInfo }: ILocalProps) => {
  const A: IRow = {
    hyperparameters: getCommitDataFromNullableDiffs('A', diff.data.hyperparameters),
    hyperparameterSet: getCommitDataFromNullableDiffs('A', diff.data.hyperparameterSet),
  };
  const B: IRow = {
    hyperparameters: getCommitDataFromNullableDiffs('B', diff.data.hyperparameters),
    hyperparameterSet: getCommitDataFromNullableDiffs('B', diff.data.hyperparameterSet),
  };

  return (
    <div>
      <CompareTable
        A={A}
        B={B}
        columns={{
          property: {
            title: 'Properties',
            width: 190,
          },
          A: {
            title: `From Commit SHA: ${shortenSHA(
              comparedCommitsInfo.commitA.sha
            )}`,
          },
          B: {
            title: `To Commit SHA: ${shortenSHA(
              comparedCommitsInfo.commitB.sha
            )}`,
          },
        }}
      >
        <CompareTable.PropDefinition
          title="Hyperparameters"
          isHidden={Boolean(!A.hyperparameters && !B.hyperparameters)}
          render={({ currentData, anotherData, type }) => {
            return currentData.hyperparameters
              ? sortArrayByAnotherArrayKeys(
                  ({ data: { name } }) => name,
                  currentData.hyperparameters,
                  anotherData.hyperparameters || [],
                ).map(h => (
                  <HyperparameterItem
                    {...getHyperparameterDiffStyles(h.diffType, type)}
                    hyperparameter={h.data}
                    key={h.data.name}
                  />
                ))
              : null;
          }}
        />
        <CompareTable.PropDefinition
          title="Hyperparameters set"
          isHidden={Boolean(!A.hyperparameterSet && !B.hyperparameterSet)}
          render={({ currentData, anotherData, type }) => {
            return currentData.hyperparameterSet
              ? sortArrayByAnotherArrayKeys(
                  ({ data: { name } }) => name,
                  currentData.hyperparameterSet,
                  anotherData.hyperparameterSet || []
                ).map(h => (
                  <HyperparameterSetItem
                    {...getHyperparameterDiffStyles(diff.diffType, type)}
                    hyperparameterSetItem={h.data}
                    key={h.data.name}
                  />
                ))
              : null;
          }}
        />
      </CompareTable>
    </div>
  );
};

const getHyperparameterDiffStyles = (
  diffType: DiffType,
  type: ComparedCommitType
): { rootStyles?: React.CSSProperties; valueStyles?: React.CSSProperties } => {
  return { rootStyles: getDiffStyles(diffType, type) };
};

const getDiffStyles = (diffType: DiffType, type: ComparedCommitType) => {
  if (diffType === 'deleted') {
    return { backgroundColor: diffColors.red };
  }
  if (diffType === 'added') {
    return { backgroundColor: diffColors.green };
  }
  return type === 'A'
    ? { backgroundColor: diffColors.red }
    : { backgroundColor: diffColors.green };
};

export default ConfigDiffView;
