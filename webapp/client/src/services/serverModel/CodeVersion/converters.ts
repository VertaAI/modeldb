import * as R from 'ramda';

import { ICodeVersion } from 'core/shared/models/CodeVersion';
import {
  ICodeVersionsFromBlob,
  BlobLocation,
} from 'core/shared/models/ModelRecord';
import { makeGithubRemoteRepoUrl } from 'core/shared/utils/github/github';

export const convertServerCodeVersion = (
  serverCodeVersion: any
): ICodeVersion | undefined => {
  if (!serverCodeVersion || Object.keys(serverCodeVersion).length === 0) {
    return undefined;
  }
  if ('code_archive' in serverCodeVersion) {
    return {
      type: 'artifact',
      data: {
        key: 'code',
        path: serverCodeVersion.code_archive.path,
        pathOnly: false,
        type: serverCodeVersion.code_archive.artifact_type,
      },
    };
  }
  {
    const serverGitSnapshot = serverCodeVersion.git_snapshot;
    return {
      type: 'git',
      data: {
        commitHash: serverGitSnapshot.hash,
        execPath: Array.isArray(serverGitSnapshot.filepaths)
          ? serverGitSnapshot.filepaths[0]
          : serverGitSnapshot.filepaths,
        isDirty: serverGitSnapshot.is_dirty,
        remoteRepoUrl: makeGithubRemoteRepoUrl(serverGitSnapshot.repo),
      },
    };
  }
};

export const convertServerCodeVersionsFromBlob = (
  serverCodeVersionsFromBlob: Record<string, any>
): ICodeVersionsFromBlob => {
  let res = R.toPairs(serverCodeVersionsFromBlob)
    .map(([location, codeVersion]) => [
      location,
      convertServerCodeVersion(codeVersion),
    ])
    .filter(([_, convertedCodeVersion]) =>
      Boolean(convertedCodeVersion)
    ) as Array<[BlobLocation, ICodeVersion]>;
  return R.fromPairs(res);
};
