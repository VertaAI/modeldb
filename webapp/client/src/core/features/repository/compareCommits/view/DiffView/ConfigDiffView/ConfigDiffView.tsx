import * as React from 'react';

import { shortenSHA } from 'core/shared/view/domain/Repository/ShortenedSHA/ShortenedSHA';
import { IConfigBlobDiff } from 'core/shared/models/Versioning/Blob/ConfigBlob';
import {
  DiffType,
  ComparedCommitType,
  getDiffBlobsData,
} from 'core/shared/models/Versioning/Blob/Diff';
import HyperparameterItem from 'core/shared/view/domain/Repository/Blob/ConfigBlob/HyperparameterItem/HyperparameterItem';
import HyperparameterSetItem from 'core/shared/view/domain/Repository/Blob/ConfigBlob/HyperparameterSetItem/HyperparameterSetItem';

import { IComparedCommitsInfo } from '../../types';
import { diffColors } from '../shared/styles';
import CompareTable from './CompareTable/CompareTable';
import sortArrayByAnotherArrayKeys from '../shared/sortArrayByAnotherArrayKeys/sortArrayByAnotherArrayKeys';

interface ILocalProps {
  diff: IConfigBlobDiff;
  comparedCommitsInfo: IComparedCommitsInfo;
}

const ConfigDiffView = ({ diff, comparedCommitsInfo }: ILocalProps) => {
  const { blobAData: blobA, blobBData: blobB } = getDiffBlobsData(diff);

  return (
    <div>
      <CompareTable
        blobA={blobA}
        blobB={blobB}
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
          isHidden={Boolean(
            blobA && blobB && !blobA.hyperparameters && !blobB.hyperparameters
          )}
          render={({ blobData, anotherBlobData, type }) => {
            return blobData && blobData.hyperparameters
              ? sortArrayByAnotherArrayKeys(
                  ({ name }) => name,
                  blobData.hyperparameters,
                  anotherBlobData ? anotherBlobData.hyperparameters || [] : []
                ).map(h => (
                  <HyperparameterItem
                    {...getHyperparameterDiffStyles(diff.diffType, type)}
                    hyperparameter={h}
                    key={h.name}
                  />
                ))
              : null;
          }}
        />
        <CompareTable.PropDefinition
          title="Hyperparameters set"
          isHidden={Boolean(
            blobA &&
              blobB &&
              !blobA.hyperparameterSet &&
              !blobB.hyperparameterSet
          )}
          render={({ blobData, anotherBlobData, type }) => {
            return blobData && blobData.hyperparameterSet
              ? sortArrayByAnotherArrayKeys(
                  ({ name }) => name,
                  blobData.hyperparameterSet,
                  anotherBlobData ? anotherBlobData.hyperparameterSet || [] : []
                ).map(h => (
                  <HyperparameterSetItem
                    {...getHyperparameterDiffStyles(diff.diffType, type)}
                    hyperparameterSetItem={h}
                    key={h.name}
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
