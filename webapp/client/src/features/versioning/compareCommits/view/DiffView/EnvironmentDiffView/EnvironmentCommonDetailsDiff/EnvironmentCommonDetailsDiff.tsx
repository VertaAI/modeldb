import * as React from 'react';

import KeyValuePairs from 'shared/view/elements/KeyValuePairs/KeyValuePairs';
import { BlobDataBox } from 'shared/view/domain/Versioning/Blob/BlobBox/BlobBox';

import { IComparedCommitsInfo } from '../../../model';
import {
  getArrayElemDiffStyles,
  IEnvironmentCommonDetailsDiffViewModel,
  needHighlightCellBackground,
} from '../utils';
import { makeHighlightCellBackground } from '../../shared/makeHighlightCellBackground';
import styles from './EnvironmentCommonDetailsDiff.module.css';
import { DiffType } from 'shared/models/Versioning/Blob/Diff';
import ComparePropertiesTable from '../../shared/ComparePropertiesTable/ComparePropertiesTable';

const highlightCellBackground = makeHighlightCellBackground<
  IEnvironmentCommonDetailsDiffViewModel
>();

const EnvironmentCommonDetailsDiff = ({
  diff,
  comparedCommitsInfo,
  diffType,
}: {
  diff: IEnvironmentCommonDetailsDiffViewModel;
  comparedCommitsInfo: IComparedCommitsInfo;
  diffType: DiffType;
}) => {
  return (
    <BlobDataBox title="Common Details">
      <ComparePropertiesTable
        A={diff}
        B={diff}
        C={diffType === 'conflicted' ? diff : undefined}
        comparedCommitsInfo={comparedCommitsInfo}
        propDefinitions={[
          {
            title: 'Environment variables',
            isHidden: diff.environmentVariables.isHidden,
            type: 'envVariables',
            getPropCellStyle: highlightCellBackground(
              ({ data, comparedCommitType }) => {
                const environmentVariables =
                  data && data.environmentVariables[comparedCommitType];
                return needHighlightCellBackground(environmentVariables);
              }
            ),
            render: ({ data, comparedCommitType }) => {
              const environmentVariables =
                data && data.environmentVariables[comparedCommitType];
              return environmentVariables ? (
                <KeyValuePairs
                  data={environmentVariables.map(
                    ({ data: { name, value } }) => ({
                      key: name,
                      value,
                    })
                  )}
                  getStyles={(_, i) =>
                    getArrayElemDiffStyles(environmentVariables[i])
                  }
                />
              ) : null;
            },
          },
          {
            title: 'Command Line',
            type: 'commandLine',
            getPropCellStyle: highlightCellBackground(
              ({ data, comparedCommitType }) =>
                Boolean(data.commandLine[comparedCommitType])
            ),
            isHidden: diff.commandLine.isHidden,
            render: ({ data, comparedCommitType }) => {
              const commandLine = data && data.commandLine[comparedCommitType];
              return commandLine ? (
                <div className={styles.commandLine}>
                  {commandLine.data.map((line, i) => (
                    <div key={i}>{line}</div>
                  ))}
                </div>
              ) : null;
            },
          },
        ]}
      />
    </BlobDataBox>
  );
};

export default EnvironmentCommonDetailsDiff;
