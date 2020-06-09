import * as React from 'react';

import { IEnvironmentBlobDiff } from 'core/shared/models/Versioning/Blob/EnvironmentBlob';
import matchBy from 'core/shared/utils/matchBy';
import { MultipleBlobDataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';

import { IComparedCommitsInfo } from '../../model';
import DockerDiff from './DockerDiff/DockerDiff';
import EnvironmentCommonDetailsDiff from './EnvironmentCommonDetailsDiff/EnvironmentCommonDetailsDiff';
import PythonDiff from './PythonDiff/PythonDiff';
import { getEnvironmentDiffViewModel } from './utils';

interface ILocalProps {
  diff: IEnvironmentBlobDiff;
  comparedCommitsInfo: IComparedCommitsInfo;
}

const EnvironmentDiffView = ({ diff, comparedCommitsInfo }: ILocalProps) => {
  const environmentDiffViewModel = getEnvironmentDiffViewModel(diff);

  return (
    <MultipleBlobDataBox title="Environment">
      {!environmentDiffViewModel.commonDetails.isHidden && (
        <EnvironmentCommonDetailsDiff
          diff={environmentDiffViewModel.commonDetails}
          comparedCommitsInfo={comparedCommitsInfo}
          diffType={diff.diffType}
        />
      )}
      {environmentDiffViewModel.data &&
        matchBy(environmentDiffViewModel.data, 'type')({
          python: pythonDiff => (
            <PythonDiff
              diff={pythonDiff}
              diffType={diff.diffType}
              comparedCommitsInfo={comparedCommitsInfo}
            />
          ),
          docker: dockerDiff => (
            <DockerDiff
              diff={dockerDiff}
              diffType={diff.diffType}
              comparedCommitsInfo={comparedCommitsInfo}
            />
          ),
        })}
    </MultipleBlobDataBox>
  );
};

export default EnvironmentDiffView;
