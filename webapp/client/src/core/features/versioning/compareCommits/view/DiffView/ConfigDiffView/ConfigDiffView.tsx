import * as React from 'react';

import {
  IConfigBlobDiff,
  IConfigHyperparameterDiff,
  IConfigHyperparameterSetItemDiff,
} from 'core/shared/models/Versioning/Blob/ConfigBlob';
import {
  DiffType,
  ComparedCommitType,
  getCommitDataFromNullableDiffs,
  DataWithDiffTypeFromDiffs,
  IArrayDiff,
} from 'core/shared/models/Versioning/Blob/Diff';
import HyperparameterItem from 'core/shared/view/domain/Versioning/Blob/ConfigBlob/HyperparameterItem/HyperparameterItem';
import HyperparameterSetItem from 'core/shared/view/domain/Versioning/Blob/ConfigBlob/HyperparameterSetItem/HyperparameterSetItem';
import { BlobDataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';

import { IComparedCommitsInfo, getCssDiffColorByCommitType } from '../../model';
import sortArrayByAnotherArrayKeys from '../shared/sortArrayByAnotherArrayKeys/sortArrayByAnotherArrayKeys';
import makeComparePropertiesTable, { makeHighlightCellBackground } from '../shared/ComparePropertiesTable/ComparePropertiesTable';

interface ILocalProps {
  diff: IConfigBlobDiff;
  comparedCommitsInfo: IComparedCommitsInfo;
}

type IRow = {
  hyperparameters?: Array<DataWithDiffTypeFromDiffs<IConfigHyperparameterDiff>>;
  hyperparameterSet?: Array<
    DataWithDiffTypeFromDiffs<IConfigHyperparameterSetItemDiff>
  >;
};

const tableComponents = makeComparePropertiesTable<IRow>();

const highlightCellBackground = makeHighlightCellBackground<IRow>();

const ConfigDiffView = ({ diff, comparedCommitsInfo }: ILocalProps) => {
  const A: IRow = {
    hyperparameters: getCommitDataFromNullableDiffs(
      'A',
      diff.data.hyperparameters
    ),
    hyperparameterSet: getCommitDataFromNullableDiffs(
      'A',
      diff.data.hyperparameterSet
    ),
  };
  const B: IRow = {
    hyperparameters: getCommitDataFromNullableDiffs(
      'B',
      diff.data.hyperparameters
    ),
    hyperparameterSet: getCommitDataFromNullableDiffs(
      'B',
      diff.data.hyperparameterSet
    ),
  };

  return (
    <BlobDataBox title="Config">
      <tableComponents.Table
        comparedCommitsInfo={comparedCommitsInfo}
        A={A}
        B={B}
      >
        <tableComponents.PropDefinition
          title="Hyperparameters"
          type="hyperparameters"
          isHidden={Boolean(!A.hyperparameters && !B.hyperparameters)}
          getCellStyle={highlightCellBackground(({ data }) => {
            if (data.hyperparameters && data.hyperparameters.length > 0) {
              return data.hyperparameters.every(({ diffType }) => diffType === 'added') || data.hyperparameters.every(({ diffType }) => diffType === 'deleted');
            }
            return false;
          })}
          render={({ data, anotherData, comparedCommitType: type }) => {
            return data && data.hyperparameters
              ? sortArrayByAnotherArrayKeys(
                  ({ data: { name } }) => name,
                  data.hyperparameters,
                  (anotherData && anotherData.hyperparameters) || []
                ).map(h => (
                  <HyperparameterItem
                    {...getHyperparameterDiffStyles(diff.diffType, h.diffType, type)}
                    hyperparameter={h.data}
                    key={h.data.name}
                  />
                ))
              : null;
          }}
        />
        <tableComponents.PropDefinition
          title="Hyperparameters set"
          type="hyperparametersSet"
          isHidden={Boolean(!A.hyperparameterSet && !B.hyperparameterSet)}
          getCellStyle={highlightCellBackground(({ data }) => {
            if (data.hyperparameterSet && data.hyperparameterSet.length > 0) {
              return data.hyperparameterSet.every(({ diffType }) => diffType === 'added') || data.hyperparameterSet.every(({ diffType }) => diffType === 'deleted');
            }
            return false;
          })}
          render={({ data, anotherData, comparedCommitType: type }) => {
            return data && data.hyperparameterSet
              ? sortArrayByAnotherArrayKeys(
                  ({ data: { name } }) => name,
                  data.hyperparameterSet,
                  (anotherData && anotherData.hyperparameterSet) || []
                ).map(h => (
                  <HyperparameterSetItem
                    {...getHyperparameterDiffStyles(diff.diffType, h.diffType, type)}
                    hyperparameterSetItem={h.data}
                    key={h.data.name}
                  />
                ))
              : null;
          }}
        />
      </tableComponents.Table>
    </BlobDataBox>
  );
};

const getHyperparameterDiffStyles = (
  diffType: DiffType,
  hyperparameterDiffType: DiffType,
  type: ComparedCommitType
): { rootStyles?: React.CSSProperties; valueStyles?: React.CSSProperties } => {
  if (diffType === 'added' || diffType === 'deleted') {
    return {};
  }
  const diffColor = getCssDiffColorByCommitType(type);
  return hyperparameterDiffType === 'modified'
    ? { valueStyles: { backgroundColor: diffColor } }
    : {
        rootStyles: { backgroundColor: diffColor },
      };
};

export default ConfigDiffView;
