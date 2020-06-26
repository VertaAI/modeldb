import * as CommitComponentLocation from 'shared/models/Versioning/CommitComponentLocation';
import { IRepository } from 'shared/models/Versioning/Repository';
import { CommitPointer } from 'shared/models/Versioning/RepositoryData';
import routes from 'shared/routes';

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
