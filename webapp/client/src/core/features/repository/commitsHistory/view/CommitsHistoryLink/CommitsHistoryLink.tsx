import * as React from 'react';

import * as DataLocation from 'core/shared/models/Repository/DataLocation';
import { IRepository } from 'core/shared/models/Repository/Repository';
import { CommitPointer } from 'core/shared/models/Repository/RepositoryData';
import Button from 'core/shared/view/elements/Button/Button';
import routes from 'routes';

interface ILocalProps {
  commitPointer: CommitPointer;
  location: DataLocation.DataLocation;
  repositoryName: IRepository['name'];
}

const CommitsHistoryLink = ({
  repositoryName,
  location,
  commitPointer,
}: ILocalProps) => {
  return commitPointer.type === 'branch' ? (
    <Button
      size="small"
      to={routes.repositoryCommitsHistory.getRedirectPathWithCurrentWorkspace({
        repositoryName,
        commitPointerValue: commitPointer.value,
        locationPathname: location as any,
      })}
    >
      History
    </Button>
  ) : null;
};

export default CommitsHistoryLink;
