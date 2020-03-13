import { ICodeVersion } from 'core/shared/models/CodeVersion';

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
        remoteRepoUrl: serverGitSnapshot.repo,
      },
    };
  }
};
