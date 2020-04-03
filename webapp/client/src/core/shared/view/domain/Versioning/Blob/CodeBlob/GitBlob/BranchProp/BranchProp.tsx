import * as React from 'react';

import { IGitCodeBlob } from 'core/shared/models/Versioning/Blob/CodeBlob';
import * as Github from 'core/shared/utils/github/github';
import matchBy from 'core/shared/utils/matchBy';
import ExternalLink from 'core/shared/view/elements/ExternalLink/ExternalLink';

interface ILocalProps {
  branch: string;
  remoteRepoUrl: IGitCodeBlob['data']['remoteRepoUrl'];
  rootStyles?: React.CSSProperties;
}

const BranchProp = ({ remoteRepoUrl, branch, rootStyles }: ILocalProps) => {
  return (
    <span data-test="git-branch" data-root-styles={rootStyles}>
      {matchBy(remoteRepoUrl, 'type')({
        github: ({ value }) => (
          <ExternalLink
            url={Github.makeBranchUrl(value, branch)}
            text={branch}
            rootStyle={rootStyles}
          />
        ),
        unknown: () => <span style={rootStyles}>{branch}</span>,
      })}
    </span>
  );
};

export default BranchProp;
