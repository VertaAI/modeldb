import { convertServerCodeDiff } from '../index';

describe('convertServerCodeDiff', () => {
  describe('git', () => {
    it('should convert added diff', () => {
      const server = {
        location: ['repo_git'],
        status: 'ADDED',
        code: {
          git: {
            status: 'ADDED',
            B: {
              repo: 'repo',
              hash: '#hhh',
              branch: 'master',
              tag: 'tag',
              is_dirty: true,
            },
          },
        },
      };

      expect(convertServerCodeDiff(server as any)).toMatchSnapshot();
    });

    it('should convert deleted diff', () => {
      const server = {
        location: ['repo_git'],
        status: 'DELETED',
        code: {
          git: {
            status: 'DELETED',
            A: {
              repo: 'repo',
              hash: '#hhh',
              branch: 'master',
              tag: 'tag',
              is_dirty: true,
            },
          },
        },
      };

      expect(convertServerCodeDiff(server as any)).toMatchSnapshot();
    });

    it('should convert modified diff', () => {
      const server = {
        location: ['repo_gitadf'],
        status: 'MODIFIED',
        code: {
          git: {
            status: 'MODIFIED',
            A: {
              repo: 'repo',
              hash: '#hhh',
              branch: 'master',
              tag: 'tag',
              is_dirty: true,
            },
            B: {
              repo: 'repadfadfadfo',
              hash: '#hhh',
              branch: 'masteadfr',
              tag: 'tag',
              is_dirty: true,
            },
          },
        },
      };

      expect(convertServerCodeDiff(server as any)).toMatchSnapshot();
    });
  });
});
