import * as React from 'react';

import PythonVersion from 'shared/view/domain/Versioning/Blob/EnvironmentBlob/PythonBlob/PythonVersion/PythonVersion';
import PythonRequirementEnvironment from 'shared/view/domain/Versioning/Blob/EnvironmentBlob/PythonBlob/PythonRequirementEnvironment/PythonRequirementEnvironment';
import { BlobDataBox } from 'shared/view/domain/Versioning/Blob/BlobBox/BlobBox';

import {
  IPythonEnvironmentDiffViewModel,
  getArrayElemDiffStyles,
  needHighlightCellBackground,
} from '../utils';
import { IComparedCommitsInfo } from '../../../model';
import { makeHighlightCellBackground } from '../../shared/makeHighlightCellBackground';
import { DiffType } from 'shared/models/Versioning/Blob/Diff';
import ComparePropertiesTable from '../../shared/ComparePropertiesTable/ComparePropertiesTable';

const highlightCellBackground = makeHighlightCellBackground<
  IPythonEnvironmentDiffViewModel
>();

const PythonDiff = ({
  diff,
  comparedCommitsInfo,
  diffType,
}: {
  diffType: DiffType;
  diff: IPythonEnvironmentDiffViewModel;
  comparedCommitsInfo: IComparedCommitsInfo;
}) => {
  return (
    <BlobDataBox title="Python Details">
      <ComparePropertiesTable
        A={diff}
        B={diff}
        C={diffType === 'conflicted' ? diff : undefined}
        comparedCommitsInfo={comparedCommitsInfo}
        propDefinitions={[
          {
            title: 'Python',
            type: 'python',
            isHidden: diff.data.pythonVersion.isHidden,
            getPropCellStyle: highlightCellBackground(
              ({ data, comparedCommitType }) =>
                Boolean(
                  data.data.pythonVersion[comparedCommitType] &&
                    data.data.pythonVersion[comparedCommitType]!
                      .hightlightedPart === 'full'
                )
            ),
            render: ({ data, comparedCommitType }) => {
              const pythonVersion =
                data && data.data.pythonVersion[comparedCommitType];
              return pythonVersion ? (
                <PythonVersion pythonVersion={pythonVersion.data} />
              ) : null;
            },
          },
          {
            title: 'Requirements',
            type: 'requirements',
            isHidden: diff.data.requirements.isHidden,
            getPropCellStyle: highlightCellBackground(
              ({ data, comparedCommitType }) => {
                return needHighlightCellBackground(
                  data && data.data.requirements[comparedCommitType]
                );
              }
            ),
            render: ({ data, comparedCommitType }) => {
              const requirements =
                data && data.data.requirements[comparedCommitType];
              return requirements
                ? requirements.map((x, i) => (
                    <PythonRequirementEnvironment
                      {...getArrayElemDiffStyles(x)}
                      pythonRequirementEnvironment={x.data}
                      key={i}
                    />
                  ))
                : null;
            },
          },
          {
            title: 'Constraints',
            type: 'constraints',
            isHidden: diff.data.constraints.isHidden,
            getPropCellStyle: highlightCellBackground(
              ({ data, comparedCommitType }) => {
                return needHighlightCellBackground(
                  data && data.data.constraints[comparedCommitType]
                );
              }
            ),
            render: ({ data, comparedCommitType }) => {
              const constraints =
                data && data.data.constraints[comparedCommitType];
              return constraints
                ? constraints.map((x, i) => (
                    <PythonRequirementEnvironment
                      {...getArrayElemDiffStyles(x)}
                      pythonRequirementEnvironment={x.data}
                      key={i}
                    />
                  ))
                : null;
            },
          },
        ]}
      />
    </BlobDataBox>
  );
};

export default PythonDiff;
