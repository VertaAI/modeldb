import * as React from 'react';

import { DiffType } from 'shared/models/Versioning/Blob/Diff';
import { makeDockerImage } from 'shared/models/Versioning/Blob/EnvironmentBlob';
import { BlobDataBox } from 'shared/view/domain/Versioning/Blob/BlobBox/BlobBox';

import { IComparedCommitsInfo } from '../../../model';
import { makeHighlightCellBackground } from '../../shared/makeHighlightCellBackground';
import ComparePropertiesTable from '../../shared/ComparePropertiesTable/ComparePropertiesTable';
import { IDockerEnvironmentDiffViewModel } from '../utils';

const highlightCellBackground = makeHighlightCellBackground<
  IDockerEnvironmentDiffViewModel
>();

const DockerDiff = ({
  diff,
  comparedCommitsInfo,
  diffType,
}: {
  diffType: DiffType;
  diff: IDockerEnvironmentDiffViewModel;
  comparedCommitsInfo: IComparedCommitsInfo;
}) => {
  return (
    <BlobDataBox title="Docker details">
      <ComparePropertiesTable
        A={diff}
        B={diff}
        C={diffType === 'conflicted' ? diff : undefined}
        comparedCommitsInfo={comparedCommitsInfo}
        propDefinitions={[
          {
            title: 'Docker container',
            type: 'docker',
            getPropCellStyle: highlightCellBackground(
              ({ data, comparedCommitType }) =>
                Boolean(
                  data.data[comparedCommitType] &&
                    data.data[comparedCommitType]!.diffColor
                )
            ),
            render: ({ data, comparedCommitType: type }) => {
              const docker = data && data.data[type];
              return docker ? (
                <span>{makeDockerImage(docker.data)}</span>
              ) : null;
            },
          },
        ]}
      />
    </BlobDataBox>
  );
};

export default DockerDiff;
