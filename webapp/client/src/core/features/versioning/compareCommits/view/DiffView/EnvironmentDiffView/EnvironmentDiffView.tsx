import * as React from 'react';

import { IEnvironmentBlobDiff } from 'core/shared/models/Versioning/Blob/EnvironmentBlob';
import { MultipleBlobDataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';
import matchBy from 'core/shared/utils/matchBy';

import { IComparedCommitsInfo } from '../../model';
import EnvironmentCommonDetailsDiff from './EnvironmentCommonDetailsDiff/EnvironmentCommonDetailsDiff';
import DockerDiff from './DockerDiff/DockerDiff';
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
        />
      )}
      {environmentDiffViewModel.data &&
        matchBy(environmentDiffViewModel.data, 'type')({
          python: pythonDiff => (
            <PythonDiff
              diff={pythonDiff}
              comparedCommitsInfo={comparedCommitsInfo}
            />
          ),
          docker: dockerDiff => (
            <DockerDiff
              diff={dockerDiff}
              comparedCommitsInfo={comparedCommitsInfo}
            />
          ),
        })}
    </MultipleBlobDataBox>
  );
};

export default EnvironmentDiffView;
