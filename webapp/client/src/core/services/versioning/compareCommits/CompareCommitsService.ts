import { bind } from 'decko';

import { Diff } from 'core/shared/models/Versioning/Blob/Diff';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  SHA,
  ICommit,
  Branch,
  CommitPointer,
} from 'core/shared/models/Versioning/RepositoryData';
import { convertServerCommitToClient } from 'core/services/serverModel/Versioning/RepositoryData/converters';

import { BaseDataService } from '../../BaseDataService';
import { convertServerDiffsToClient } from '../../serverModel/Versioning/CompareCommits/converters';

export default class CompareCommitsService extends BaseDataService {
  constructor() {
    super();
  }

  @bind
  public async loadCommitsDiff({
    commitASha,
    commitBSha,
    repositoryId,
  }: {
    commitASha: SHA;
    commitBSha: SHA;
    repositoryId: IRepository['id'];
  }): Promise<Diff[]> {
    const response = await this.get({
      url: `/v1/modeldb/versioning/repositories/${repositoryId}/diff`,
      config: {
        params: {
          commit_a: commitASha,
          commit_b: commitBSha,
          replace_a_with_common_ancestor: true,
        },
      },
    });

    return convertServerDiffsToClient(response.data.diffs);
  }

  @bind
  public async mergeCommits({
    base,
    commitASha,
    commitBSha,
    repositoryId,
  }: {
    base: CommitPointer;
    commitASha: SHA;
    commitBSha: SHA;
    repositoryId: IRepository['id'];
  }): Promise<ICommit> {
    const response = await this.post({
      url: `/v1/modeldb/versioning/repositories/${repositoryId}/merge`,
      data: {
        commit_sha_a: commitASha,
        commit_sha_b: commitBSha,
      },
    });
    const newCommit = convertServerCommitToClient(response.data.commit);

    if (base.type === 'branch') {
      await this.put({
        url: `/v1/modeldb/versioning/repositories/${repositoryId}/branches/${
          base.value
        }`,
        data: `"${newCommit.sha}"`,
        config: { headers: { 'Content-Type': 'raw' } },
      });
    }

    return newCommit;
  }
}
