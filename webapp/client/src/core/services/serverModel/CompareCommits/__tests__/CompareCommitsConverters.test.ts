import { convertServerDiffToClient } from '../converters';
import { convertServerCodeDiff } from '../convertServerCodeDiff';
import {
  mockedClientSuccessAddedDatasetDiff,
  mockedServerAddedDatasetDiff,
  mockedServerDeletedDatasetDiff,
  mockedServerUpdatedDatasetDiff,
  mockedClientSuccessDeletedDatasetDiff,
  mockedClientSuccessUpdatedDatasetDiff,
} from './mocks/mockCompareCommits';

describe('CompareCommits converters', () => {
  describe('(convertServerDiffToClient)', () => {
    describe('dataset blob diff', () => {
      it('should convert correct server response with status "added" to client data', () => {
        expect(convertServerDiffToClient(mockedServerAddedDatasetDiff)).toEqual(
          mockedClientSuccessAddedDatasetDiff
        );
      });

      it('should convert correct server response with status "deleted" to client data', () => {
        expect(
          convertServerDiffToClient(mockedServerDeletedDatasetDiff)
        ).toEqual(mockedClientSuccessDeletedDatasetDiff);
      });

      it('should convert correct server response with status "updated" to client data', () => {
        expect(
          convertServerDiffToClient(mockedServerUpdatedDatasetDiff)
        ).toEqual(mockedClientSuccessUpdatedDatasetDiff);
      });
    });

    describe('code blob diff', () => {
      it('should convert git code diff with deleted diff type', () => {
        const diff = {
          location: ['deleted'],
          status: 'DELETED',
          code: {
            git: {
              B: {
                repo: 'git@github.com:VertaAI/modeldb.git',
                hash: '#hhh',
                branch: 'development',
                tag: 'tag-2',
              },
            },
          },
        };

        expect(convertServerDiffToClient(diff)).toMatchSnapshot();
      });

      it('should convert git code diff with modified diff type', () => {
        const diff = {
          location: ['folder_asdfc', 'repo_blob'],
          status: 'MODIFIED',
          code: {
            git: {
              A: {
                repo: 'git@github.com:VertaAI/modeldb.git',
                hash: '#hhh',
                branch: 'master',
                tag: 'tag',
                is_dirty: true,
              },
              B: {
                repo: 'git@github.com:VertaAI/modeldb.git',
                hash: '#hhh',
                branch: 'development',
                tag: 'tag-2',
              },
            },
          },
        };

        expect(convertServerDiffToClient(diff)).toMatchSnapshot();
      });
    });
  });
});
