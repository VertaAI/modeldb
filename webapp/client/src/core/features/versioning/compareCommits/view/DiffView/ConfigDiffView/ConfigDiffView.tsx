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
} from 'core/shared/models/Versioning/Blob/Diff';
import HyperparameterItem from 'core/shared/view/domain/Versioning/Blob/ConfigBlob/HyperparameterItem/HyperparameterItem';
import HyperparameterSetItem from 'core/shared/view/domain/Versioning/Blob/ConfigBlob/HyperparameterSetItem/HyperparameterSetItem';
import { BlobDataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';

import { IComparedCommitsInfo, getCssDiffColor } from '../../model';
import sortArrayByAnotherArrayKeys from '../shared/sortArrayByAnotherArrayKeys/sortArrayByAnotherArrayKeys';
import makeComparePropertiesTable from '../shared/ComparePropertiesTable/ComparePropertiesTable';

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
          isHidden={Boolean(!A.hyperparameters && !B.hyperparameters)}
          render={({ data, anotherData, type }) => {
            return data && data.hyperparameters
              ? sortArrayByAnotherArrayKeys(
                  ({ data: { name } }) => name,
                  data.hyperparameters,
                  (anotherData && anotherData.hyperparameters) || []
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
        <tableComponents.PropDefinition
          title="Hyperparameters set"
          isHidden={Boolean(!A.hyperparameterSet && !B.hyperparameterSet)}
          render={({ data, anotherData, type }) => {
            return data && data.hyperparameterSet
              ? sortArrayByAnotherArrayKeys(
                  ({ data: { name } }) => name,
                  data.hyperparameterSet,
                  (anotherData && anotherData.hyperparameterSet) || []
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
      </tableComponents.Table>
    </BlobDataBox>
  );
};

const getHyperparameterDiffStyles = (
  diffType: DiffType,
  type: ComparedCommitType
): { rootStyles?: React.CSSProperties; valueStyles?: React.CSSProperties } => {
  const diffColor = getCssDiffColor(type);
  return diffType === 'modified'
    ? { valueStyles: { backgroundColor: diffColor } }
    : {
        rootStyles: { backgroundColor: diffColor },
      };
};

export default ConfigDiffView;
