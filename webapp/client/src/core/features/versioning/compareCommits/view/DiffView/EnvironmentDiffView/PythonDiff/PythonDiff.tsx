import * as React from 'react';

import PythonVersion from 'core/shared/view/domain/Versioning/Blob/EnvironmentBlob/PythonBlob/PythonVersion/PythonVersion';
import PythonRequirementEnvironment from 'core/shared/view/domain/Versioning/Blob/EnvironmentBlob/PythonBlob/PythonRequirementEnvironment/PythonRequirementEnvironment';
import { BlobDataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';

import {
  IPythonEnvironmentDiffViewModel,
  getArrayElemDiffStyles,
  needHighlightCellBackground,
} from '../utils';
import { IComparedCommitsInfo, getCssDiffColor } from '../../../model';
import makeComparePropertiesTable, {
  makeHighlightCellBackground,
} from '../../shared/ComparePropertiesTable/ComparePropertiesTable';

const tableComponents = makeComparePropertiesTable<
  IPythonEnvironmentDiffViewModel
>();
const highlightCellBackground = makeHighlightCellBackground<
  IPythonEnvironmentDiffViewModel
>();

const PythonDiff = ({
  diff,
  comparedCommitsInfo,
}: {
  diff: IPythonEnvironmentDiffViewModel;
  comparedCommitsInfo: IComparedCommitsInfo;
}) => {
  return (
    <BlobDataBox title="Python Details">
      <tableComponents.Table
        A={diff}
        B={diff}
        comparedCommitsInfo={comparedCommitsInfo}
      >
        <tableComponents.PropDefinition
          title="Python"
          type="python"
          isHidden={diff.data.pythonVersion.isHidden}
          getCellStyle={highlightCellBackground(({ data, comparedCommitType }) =>
            Boolean(
              data.data.pythonVersion[comparedCommitType] &&
                data.data.pythonVersion[comparedCommitType]!.hightlightedPart === 'full'
            )
          )}
          render={({ data, comparedCommitType }) => {
            const pythonVersion = data && data.data.pythonVersion[comparedCommitType];
            return pythonVersion ? (
              <PythonVersion pythonVersion={pythonVersion.data} />
            ) : null;
          }}
        />
        <tableComponents.PropDefinition
          title="Requirements"
          type="requirements"
          isHidden={diff.data.requirements.isHidden}
          getCellStyle={highlightCellBackground(({ data, comparedCommitType }) => {
            return needHighlightCellBackground(data && data.data.requirements[comparedCommitType]);
          })}
          render={({ data, comparedCommitType }) => {
            const requirements = data && data.data.requirements[comparedCommitType];
            return requirements
              ? requirements.map((x, i) => (
                  <PythonRequirementEnvironment
                    {...getArrayElemDiffStyles(x)}
                    pythonRequirementEnvironment={x.data}
                    key={i}
                  />
                ))
              : null;
          }}
        />
        <tableComponents.PropDefinition
          title="Constraints"
          type="constraints"
          isHidden={diff.data.constraints.isHidden}
          getCellStyle={highlightCellBackground(({ data, comparedCommitType }) => {
            return needHighlightCellBackground(data && data.data.constraints[comparedCommitType]);
          })}
          render={({ data, comparedCommitType }) => {
            const constraints = data && data.data.constraints[comparedCommitType];
            return constraints
              ? constraints.map((x, i) => (
                  <PythonRequirementEnvironment
                    {...getArrayElemDiffStyles(x)}
                    pythonRequirementEnvironment={x.data}
                    key={i}
                  />
                ))
              : null;
          }}
        />
      </tableComponents.Table>
    </BlobDataBox>
  );
};

export default PythonDiff;
