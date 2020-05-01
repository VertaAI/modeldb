import * as React from 'react';

import KeyValuePairs from 'core/shared/view/elements/KeyValuePairs/KeyValuePairs';
import { BlobDataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';

import { IComparedCommitsInfo } from '../../../model';
import {
  getArrayElemDiffStyles,
  IEnvironmentCommonDetailsDiffViewModel,
  needHighlightCellBackground,
} from '../utils';
import makeComparePropertiesTable, {
  makeHighlightCellBackground,
} from '../../shared/ComparePropertiesTable/ComparePropertiesTable';
import styles from './EnvironmentCommonDetailsDiff.module.css';

const tableComponents = makeComparePropertiesTable<
  IEnvironmentCommonDetailsDiffViewModel
>();

const highlightCellBackground = makeHighlightCellBackground<
  IEnvironmentCommonDetailsDiffViewModel
>();

const EnvironmentCommonDetailsDiff = ({
  diff,
  comparedCommitsInfo,
}: {
  diff: IEnvironmentCommonDetailsDiffViewModel;
  comparedCommitsInfo: IComparedCommitsInfo;
}) => {
  return (
    <BlobDataBox title="Common Details">
      <tableComponents.Table
        A={diff}
        B={diff}
        comparedCommitsInfo={comparedCommitsInfo}
      >
        <tableComponents.PropDefinition
          title="Environment variables"
          isHidden={diff.environmentVariables.isHidden}
          type="envVariables"
          getCellStyle={highlightCellBackground(({ data, comparedCommitType }) => {
            const environmentVariables =
              data && data.environmentVariables[comparedCommitType];
            return needHighlightCellBackground(environmentVariables);
          })}
          render={({ data, comparedCommitType }) => {
            const environmentVariables =
              data && data.environmentVariables[comparedCommitType];
            return environmentVariables ? (
              <KeyValuePairs
                data={environmentVariables.map(({ data: { name, value } }) => ({
                  key: name,
                  value,
                }))}
                getStyles={(_, i) =>
                  getArrayElemDiffStyles(environmentVariables[i])
                }
              />
            ) : null;
          }}
        />
        <tableComponents.PropDefinition
          title="Command Line"
          type="commandLine"
          getCellStyle={highlightCellBackground(({ data, comparedCommitType }) =>
            Boolean(data.commandLine[comparedCommitType])
          )}
          isHidden={diff.commandLine.isHidden}
          render={({ data, comparedCommitType }) => {
            const commandLine = data && data.commandLine[comparedCommitType];
            return commandLine ? (
              <div className={styles.commandLine}>
                {commandLine.data.map((line, i) => (
                  <div key={i}>{line}</div>
                ))}
              </div>
            ) : null;
          }}
        />
      </tableComponents.Table>
    </BlobDataBox>
  );
};

export default EnvironmentCommonDetailsDiff;
