import * as React from 'react';

import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  CommitPointer,
  defaultBranch,
} from 'core/shared/models/Versioning/RepositoryData';
import Button from 'core/shared/view/elements/Button/Button';
import routes from 'routes';

interface ILocalProps {
  repositoryName: IRepository['name'];
  commitPointer: CommitPointer;
}

const CompareChangesLink = ({ repositoryName, commitPointer }: ILocalProps) => {
  return commitPointer.type !== 'commitSha' ? (
    <Button
      to={routes.repositoryCompareChanges.getRedirectPathWithCurrentWorkspace({
        repositoryName,
        commitPointerAValue: defaultBranch,
        commitPointerBValue: commitPointer.value,
      })}
      size="small"
    >
      Compare
    </Button>
  ) : null;
};

export default CompareChangesLink;
