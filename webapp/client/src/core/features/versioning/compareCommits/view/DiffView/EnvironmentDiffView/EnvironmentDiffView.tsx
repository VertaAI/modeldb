import * as React from 'react';

import KeyValuePairs from 'core/shared/view/elements/KeyValuePairs/KeyValuePairs';
import { shortenSHA } from 'core/shared/view/domain/Versioning/ShortenedSHA/ShortenedSHA';
import {
  DiffType,
  ComparedCommitType,
  getABData,
} from 'core/shared/models/Versioning/Blob/Diff';
import {
  IEnvironmentBlobDiff,
  makeDockerImage,
  safeMapPythonBlobDataDiff,
  checkCommonEnvironmentDataIsChanged,
} from 'core/shared/models/Versioning/Blob/EnvironmentBlob';
import PythonRequirementEnvironment from 'core/shared/view/domain/Versioning/Blob/EnvironmentBlob/PythonBlob/PythonRequirementEnvironment/PythonRequirementEnvironment';
import PythonVersion from 'core/shared/view/domain/Versioning/Blob/EnvironmentBlob/PythonBlob/PythonVersion/PythonVersion';

import { IComparedCommitsInfo, getCssDiffColor } from '../../model';
import sortArrayByAnotherArrayKeys from '../shared/sortArrayByAnotherArrayKeys/sortArrayByAnotherArrayKeys';
import { diffColors } from '../shared/styles';
import CompareTable from './CompareTable/CompareTable';
import styles from './EnvironmentDiffView.module.css';
import { MultipleBlobDataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';
import { getColumnComparedCommitsTitles } from '../shared/comparedCommitsNames';

interface ILocalProps {
  diff: IEnvironmentBlobDiff;
  comparedCommitsInfo: IComparedCommitsInfo;
}

const EnvironmentDiffView = ({ diff, comparedCommitsInfo }: ILocalProps) => {
  const { A: blobA, B: blobB } = getABData(diff.data);
  const blobType =
    (blobA && blobA.data && blobA.data.type) ||
    (blobB && blobB.data && blobB.data.type);

  return (
    <MultipleBlobDataBox title="Environment">
      {checkCommonEnvironmentDataIsChanged(blobA, blobB) && (
        <div className={styles.diff}>
          <EnvironmentCommonDetailsDiff
            diff={diff}
            comparedCommitsInfo={comparedCommitsInfo}
          />
        </div>
      )}
      {blobType === 'docker' && (
        <DockerDiff diff={diff} comparedCommitsInfo={comparedCommitsInfo} />
      )}
      {blobType === 'python' && (
        <PythonDiff diff={diff} comparedCommitsInfo={comparedCommitsInfo} />
      )}
    </MultipleBlobDataBox>
  );
};

const EnvironmentCommonDetailsDiff = ({
  diff,
  comparedCommitsInfo,
}: {
  diff: IEnvironmentBlobDiff;
  comparedCommitsInfo: IComparedCommitsInfo;
}) => {
  const { A: blobA, B: blobB } = getABData(diff.data);

  return (
    <CompareTable
      title="Common Details"
      A={blobA}
      B={blobB}
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
        title="Environment variables"
        isHidden={blobA && blobB && !blobA.variables && !blobB.variables}
        render={({ blobData, anotherBlobData, type }) => {
          const displayedVariables =
            blobData && blobData.variables
              ? getDiffViewModelForArrayItems(
                  diff.diffType,
                  ({ name }) => name,
                  blobData.variables,
                  anotherBlobData ? anotherBlobData.variables : []
                )
              : null;
          return displayedVariables ? (
            <KeyValuePairs
              data={displayedVariables.map(({ data: { name, value } }) => ({
                key: name,
                value,
              }))}
              getStyles={pair => {
                const res = displayedVariables.find(
                  ({ data }) => data.name === pair.key
                );
                return res
                  ? getDiffStylesForUniqItem(res.diffType, type)
                  : undefined;
              }}
            />
          ) : null;
        }}
      />
      <CompareTable.PropDefinition
        title="Command Line"
        isHidden={blobA && blobB && !blobA.commandLine && !blobB.commandLine}
        render={({ blobData, type }) => {
          return blobData && blobData.commandLine ? (
            <div className={styles.commandLine}>
              {blobData.commandLine.map((line, i) => (
                <div style={getCommonDiffStyles(type)} key={i}>
                  {line}
                </div>
              ))}
            </div>
          ) : null;
        }}
      />
    </CompareTable>
  );
};

const DockerDiff = ({
  diff,
  comparedCommitsInfo,
}: {
  diff: IEnvironmentBlobDiff;
  comparedCommitsInfo: IComparedCommitsInfo;
}) => {
  const { A: blobA, B: blobB } = getABData(diff.data);

  return (
    <CompareTable
      title="Docker details"
      A={blobA}
      B={blobB}
      columns={{
        property: {
          title: 'Properties',
          width: 190,
        },
        ...getColumnComparedCommitsTitles(comparedCommitsInfo),
      }}
    >
      <CompareTable.PropDefinition
        title="Docker container"
        render={({ blobData, type }) => {
          return blobData &&
            blobData.data &&
            blobData.data.type === 'docker' &&
            blobData.data.data.repository ? (
            <span style={getCommonDiffStyles(type)}>
              {makeDockerImage(blobData.data.data)}
            </span>
          ) : null;
        }}
      />
    </CompareTable>
  );
};

const PythonDiff = ({
  diff,
  comparedCommitsInfo,
}: {
  diff: IEnvironmentBlobDiff;
  comparedCommitsInfo: IComparedCommitsInfo;
}) => {
  const { A: blobA, B: blobB } = getABData(diff.data);

  return (
    <CompareTable
      title="Python Details"
      A={blobA}
      B={blobB}
      columns={{
        property: {
          title: 'Properties',
          width: 190,
        },
        ...getColumnComparedCommitsTitles(comparedCommitsInfo),
      }}
    >
      <CompareTable.PropDefinition
        title="Python"
        isHidden={
          !safeMapPythonBlobDataDiff(blobA, 'pythonVersion', r => true) &&
          !safeMapPythonBlobDataDiff(blobB, 'pythonVersion', r => true)
        }
        render={({ blobData, type }) => {
          return safeMapPythonBlobDataDiff(
            blobData,
            'pythonVersion',
            pythonVersion => (
              <PythonVersion
                pythonVersion={pythonVersion}
                rootStyles={getCommonDiffStyles(type)}
              />
            )
          );
        }}
      />
      <CompareTable.PropDefinition
        title="Requirements"
        isHidden={
          !safeMapPythonBlobDataDiff(blobA, 'requirements', r => true) &&
          !safeMapPythonBlobDataDiff(blobB, 'requirements', r => true)
        }
        render={({ blobData, anotherBlobData, type }) => {
          return safeMapPythonBlobDataDiff(
            blobData,
            'requirements',
            requirements =>
              getDiffViewModelForArrayItems(
                diff.diffType,
                ({ library }) => library,
                requirements,
                safeMapPythonBlobDataDiff(
                  anotherBlobData,
                  'requirements',
                  r => r
                ) || []
              ).map((r, i) => {
                return (
                  <PythonRequirementEnvironment
                    {...getDiffStylesForUniqItem(r.diffType, type)}
                    pythonRequirementEnvironment={r.data}
                    key={i}
                  />
                );
              })
          );
        }}
      />
      <CompareTable.PropDefinition
        title="Constraints"
        isHidden={
          !safeMapPythonBlobDataDiff(blobA, 'constraints', r => true) &&
          !safeMapPythonBlobDataDiff(blobB, 'constraints', r => true)
        }
        render={({ blobData, anotherBlobData, type }) => {
          return safeMapPythonBlobDataDiff(
            blobData,
            'constraints',
            constraints =>
              getDiffViewModelForArrayItems(
                diff.diffType,
                ({ library }) => library,
                constraints,
                safeMapPythonBlobDataDiff(
                  anotherBlobData,
                  'constraints',
                  r => r
                ) || []
              ).map((r, i) => (
                <PythonRequirementEnvironment
                  {...getDiffStylesForUniqItem(r.diffType, type)}
                  pythonRequirementEnvironment={r.data}
                  key={i}
                />
              ))
          );
        }}
      />
    </CompareTable>
  );
};

function getDiffViewModelForArrayItems<T>(
  diffType: DiffType,
  getItemKey: (item: T) => string,
  currentItems: T[],
  otherItems: T[] | undefined
) {
  const sorted = sortArrayByAnotherArrayKeys(
    getItemKey,
    currentItems,
    otherItems || []
  );
  return getDiffInfoForItems(diffType, getItemKey, sorted, otherItems);
}

function getDiffInfoForItems<T>(
  diffType: DiffType,
  getItemKey: (item: T) => string,
  currentItems: T[],
  otherItems: T[] | undefined
): Array<{ data: T; diffType: DiffType }> {
  return currentItems.map(item => {
    if (diffType === 'added' || diffType === 'deleted') {
      return { diffType: diffType, data: item };
    } else {
      return {
        diffType: (otherItems || []).find(
          anotherItem => getItemKey(anotherItem) === getItemKey(item)
        )
          ? ('modified' as const)
          : ('added' as const),
        data: item,
      };
    }
  });
}

const getDiffStylesForUniqItem = (
  diffType: DiffType,
  type: ComparedCommitType
) => {
  if (diffType === 'added' || diffType === 'deleted') {
    return {
      rootStyles: getCommonDiffStyles(type),
    };
  } else {
    return { valueStyles: getCommonDiffStyles(type) };
  }
};

const getCommonDiffStyles = (type: ComparedCommitType) => {
  return { backgroundColor: getCssDiffColor(type) };
};

export default EnvironmentDiffView;
