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

import { IComparedCommitsInfo } from '../../types';
import sortArrayByAnotherArrayKeys from '../shared/sortArrayByAnotherArrayKeys/sortArrayByAnotherArrayKeys';
import { diffColors } from '../shared/styles';
import CompareTable from './CompareTable/CompareTable';
import styles from './EnvironmentDiffView.module.css';

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
    <div className={styles.root}>
      {checkCommonEnvironmentDataIsChanged(blobA, blobB) && (
        <div className={styles.diff}>
          <EnvironmentCommonDetailsDiff
            diff={diff}
            comparedCommitsInfo={comparedCommitsInfo}
          />
        </div>
      )}
      {blobType === 'docker' && (
        <div className={styles.diff}>
          <DockerDiff diff={diff} comparedCommitsInfo={comparedCommitsInfo} />
        </div>
      )}
      {blobType === 'python' && (
        <div className={styles.diff}>
          <PythonDiff diff={diff} comparedCommitsInfo={comparedCommitsInfo} />
        </div>
      )}
    </div>
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
        title="Environment variables"
        isHidden={blobA && blobB && !blobA.variables && !blobB.variables}
        render={({ blobData, anotherBlobData, type }) => {
          const displayedVariables =
            blobData && blobData.variables
              ? sortArrayByAnotherArrayKeys(
                  ({ name }) => name,
                  blobData.variables,
                  anotherBlobData ? anotherBlobData.variables || [] : []
                )
              : null;
          return displayedVariables ? (
            <KeyValuePairs
              data={displayedVariables.map(({ name, value }) => ({
                key: name,
                value,
              }))}
              getRootStyles={() => getDiffStyles(diff.diffType, type)}
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
                <div style={getDiffStyles(diff.diffType, type)} key={i}>
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
        title="Docker container"
        render={({ blobData, type }) => {
          return blobData &&
            blobData.data &&
            blobData.data.type === 'docker' &&
            blobData.data.data.repository ? (
            <span style={getDiffStyles(diff.diffType, type)}>
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
                rootStyles={getDiffStyles(diff.diffType, type)}
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
              sortArrayByAnotherArrayKeys(
                ({ library }) => library,
                requirements,
                safeMapPythonBlobDataDiff(
                  anotherBlobData,
                  'requirements',
                  r => r
                ) || []
              ).map((r, i) => (
                <PythonRequirementEnvironment
                  pythonRequirementEnvironment={r}
                  rootStyles={getDiffStyles(diff.diffType, type)}
                  key={i}
                />
              ))
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
              sortArrayByAnotherArrayKeys(
                ({ library }) => library,
                constraints,
                safeMapPythonBlobDataDiff(
                  anotherBlobData,
                  'constraints',
                  r => r
                ) || []
              ).map((r, i) => (
                <PythonRequirementEnvironment
                  pythonRequirementEnvironment={r}
                  rootStyles={getDiffStyles(diff.diffType, type)}
                  key={i}
                />
              ))
          );
        }}
      />
    </CompareTable>
  );
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

export default EnvironmentDiffView;
