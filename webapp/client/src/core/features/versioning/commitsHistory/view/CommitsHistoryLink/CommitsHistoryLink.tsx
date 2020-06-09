import * as React from 'react';

import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import { CommitPointer } from 'core/shared/models/Versioning/RepositoryData';
import Button from 'core/shared/view/elements/Button/Button';
import routes from 'core/shared/routes';

interface ILocalProps {
  commitPointer: CommitPointer;
  location: CommitComponentLocation.CommitComponentLocation;
  repositoryName: IRepository['name'];
  children: (link: string | null) => any;
}

const CommitsHistoryLink = ({
  repositoryName,
  location,
  commitPointer,
  children,
}: ILocalProps) => {
  return commitPointer.type === 'branch'
    ? children(
        routes.repositoryCommitsHistory.getRedirectPathWithCurrentWorkspace({
          repositoryName,
          commitPointerValue: commitPointer.value,
          locationPathname: location as any,
        })
      )
    : children(null);
};

export default CommitsHistoryLink;
