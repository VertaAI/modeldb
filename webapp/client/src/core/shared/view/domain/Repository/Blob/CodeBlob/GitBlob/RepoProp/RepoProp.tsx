import * as React from 'react';

import { IGitCodeBlob } from 'core/shared/models/Versioning/Blob/CodeBlob';
import * as Github from 'core/shared/utils/github/github';
import matchBy from 'core/shared/utils/matchBy';
import ExternalLink from 'core/shared/view/elements/ExternalLink/ExternalLink';

interface ILocalProps {
  remoteRepoUrl: IGitCodeBlob['data']['remoteRepoUrl'];
  rootStyles?: React.CSSProperties;
}

const RepoProp = ({ remoteRepoUrl, rootStyles }: ILocalProps) => {
  return matchBy(remoteRepoUrl, 'type')({
    github: ({ value }) => (
      <ExternalLink
        url={Github.makeRepoUrl(value)}
        text={Github.makeRepoShortName(value)}
        rootStyle={rootStyles}
      />
    ),
    unknown: ({ value }) => <span style={rootStyles}>{value}</span>,
  });
};

export default RepoProp;
