import { bind } from 'decko';

import { Diff } from 'core/shared/models/Repository/Blob/Diff';
import { IRepository } from 'core/shared/models/Repository/Repository';
import { SHA } from 'core/shared/models/Repository/RepositoryData';

import { BaseDataService } from '../../BaseDataService';
import { convertServerDiffsToClient } from '../../serverModel/CompareCommits/converters';

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
        },
      },
    });

    return convertServerDiffsToClient(response.data.diffs);
  }
}
