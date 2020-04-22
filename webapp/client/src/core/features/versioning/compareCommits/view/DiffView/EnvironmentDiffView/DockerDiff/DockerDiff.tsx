import * as React from 'react';

import { makeDockerImage } from 'core/shared/models/Versioning/Blob/EnvironmentBlob';
import { BlobDataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';

import { IComparedCommitsInfo } from '../../../model';
import { IDockerEnvironmentDiffViewModel } from '../utils';
import makeComparePropertiesTable, {
  makeHighlightCellBackground,
} from '../../shared/ComparePropertiesTable/ComparePropertiesTable';

const tableComponents = makeComparePropertiesTable<
  IDockerEnvironmentDiffViewModel
>();

const highlightCellBackground = makeHighlightCellBackground<
  IDockerEnvironmentDiffViewModel
>();

const DockerDiff = ({
  diff,
  comparedCommitsInfo,
}: {
  diff: IDockerEnvironmentDiffViewModel;
  comparedCommitsInfo: IComparedCommitsInfo;
}) => {
  return (
    <BlobDataBox title="Docker details">
      <tableComponents.Table
        A={diff}
        B={diff}
        comparedCommitsInfo={comparedCommitsInfo}
      >
        <tableComponents.PropDefinition
          title="Docker container"
          type="docker"
          getCellStyle={highlightCellBackground(
            ({ data, comparedCommitType }) =>
              Boolean(
                data.data[comparedCommitType] &&
                  data.data[comparedCommitType]!.diffColor
              )
          )}
          render={({ data, comparedCommitType: type }) => {
            const docker = data && data.data[type];
            return docker ? <span>{makeDockerImage(docker.data)}</span> : null;
          }}
        />
      </tableComponents.Table>
    </BlobDataBox>
  );
};

export default DockerDiff;
