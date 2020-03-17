import * as React from 'react';

import { SHA } from 'core/shared/models/Versioning/RepositoryData';

const ShortenedSHA = ({ sha }: { sha: SHA }) => {
  return <span title={sha}>{sha.slice(0, 7)}</span>;
};

export const shortenSHA = (sha: SHA) => sha.slice(0, 7);

export default ShortenedSHA;
