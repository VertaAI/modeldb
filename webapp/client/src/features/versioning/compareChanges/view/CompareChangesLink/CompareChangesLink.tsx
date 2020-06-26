import { IRepository } from 'shared/models/Versioning/Repository';
import {
  CommitPointer,
  defaultBranch,
} from 'shared/models/Versioning/RepositoryData';
import routes from 'shared/routes';

interface ILocalProps {
  repositoryName: IRepository['name'];
  commitPointer: CommitPointer;
  children: (link: string | null) => any;
}

const CompareChangesLink = ({
  repositoryName,
  commitPointer,
  children,
}: ILocalProps) => {
  return commitPointer.type !== 'commitSha'
    ? children(
        routes.repositoryCompareChanges.getRedirectPathWithCurrentWorkspace({
          repositoryName,
          commitPointerAValue: defaultBranch,
          commitPointerBValue: commitPointer.value,
        })
      )
    : children(null);
};

export default CompareChangesLink;
