import { makeGithubRemoteRepoUrl } from '../github';

const repositoryFullNameVerta = 'VertaAI/VertaWebApp';
describe('makeGithubRemoteRepoUrl util', () => {
  describe('ssh', () => {
    it('should correct parse git info from github ssh url', () => {
      expect(
        makeGithubRemoteRepoUrl('git@github.com:VertaAI/VertaWebApp.git').value
      ).toEqual({
        baseUrl: 'github.com',
        repositoryFullName: repositoryFullNameVerta,
      });
    });

    it('should correct parse git info from gitlab ssh url', () => {
      expect(
        makeGithubRemoteRepoUrl('git@gitlab.com:username/repo-name.git').value
      ).toEqual({
        baseUrl: 'gitlab.com',
        repositoryFullName: 'username/repo-name',
      });
    });

    it('should correct parse git info from github enterprise ssh url', () => {
      expect(
        makeGithubRemoteRepoUrl('git@github.verta.com:VertaAI/VertaWebApp.git')
          .value
      ).toEqual({
        baseUrl: 'github.verta.com',
        repositoryFullName: repositoryFullNameVerta,
      });
    });
  });

  describe('https', () => {
    it('should correct parse git info from github https url', () => {
      expect(
        makeGithubRemoteRepoUrl('https://github.com/VertaAI/VertaWebApp.git')
          .value
      ).toEqual({
        baseUrl: 'github.com',
        repositoryFullName: repositoryFullNameVerta,
      });
    });

    it('should correct parse git info from gitlab https url', () => {
      expect(
        makeGithubRemoteRepoUrl('https://gitlab.com/username/repo-name.git')
          .value
      ).toEqual({
        baseUrl: 'gitlab.com',
        repositoryFullName: 'username/repo-name',
      });
    });

    it('should correct parse git info from github enterprise https url', () => {
      expect(
        makeGithubRemoteRepoUrl(
          'https://github.verta.com/VertaAI/VertaWebApp.git'
        ).value
      ).toEqual({
        baseUrl: 'github.verta.com',
        repositoryFullName: repositoryFullNameVerta,
      });
    });
  });

  describe('other cases', () => {
    it('should correct parse git info from file url', () => {
      expect(makeGithubRemoteRepoUrl('filename/something.git').value).toEqual({
        baseUrl: undefined,
        repositoryFullName: 'filename/something',
      });
    });

    it('should correct parse git info from file url', () => {
      expect(makeGithubRemoteRepoUrl('something').value).toEqual({
        baseUrl: undefined,
        repositoryFullName: 'something',
      });
    });

    it('should return unknown type when unparseable url have been passed', () => {
      expect(makeGithubRemoteRepoUrl('').type).toEqual('unknown');
    });
  });
});
