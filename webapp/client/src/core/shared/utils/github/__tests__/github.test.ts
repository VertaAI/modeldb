import * as Github from '../github';

describe('(core utils) github', () => {
  describe('parseGithubRemoteRepoUrl', () => {
    describe('https? remote repo url', () => {
      it('should parse http github repo url', () => {
        const res = Github.parseGithubRemoteRepoUrl(
          'http://github.com/VertaAI/VertaWebApp'
        );

        const expected: ReturnType<typeof Github.parseGithubRemoteRepoUrl> = {
          type: 'success',
          data: {
            userName: 'VertaAI',
            repositoryInfo: {
              name: 'VertaWebApp',
              nameWithExtension: 'VertaWebApp',
            },
          },
        };
        expect(res).toEqual(expected);
      });

      it('should return empty strings for info when github url doesn`t point to repo', () => {
        const res = Github.parseGithubRemoteRepoUrl('http://github.com/VertaA');

        const expected: ReturnType<typeof Github.parseGithubRemoteRepoUrl> = {
          type: 'success',
          data: {
            userName: '',
            repositoryInfo: {
              name: '',
              nameWithExtension: '',
            },
          },
        };
        expect(res).toEqual(expected);
      });
    });
  });
});
