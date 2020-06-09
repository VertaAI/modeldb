import * as React from 'react';

import { IGitCodeBlob } from 'shared/models/Versioning/Blob/CodeBlob';
import * as Github from 'shared/utils/github/github';
import matchBy from 'shared/utils/matchBy';
import ExternalLink from 'shared/view/elements/ExternalLink/ExternalLink';

interface ILocalProps {
  tag: string;
  remoteRepoUrl: IGitCodeBlob['data']['remoteRepoUrl'];
  rootStyles?: React.CSSProperties;
}

const TagProp = ({ remoteRepoUrl, tag, rootStyles }: ILocalProps) => {
  return matchBy(remoteRepoUrl, 'type')({
    github: ({ value }) => (
      <ExternalLink
        url={Github.makeTagUrl(value, tag)}
        text={tag}
        rootStyle={rootStyles}
      />
    ),
    unknown: () => <span style={rootStyles}>{tag}</span>,
  });
};

export default TagProp;
