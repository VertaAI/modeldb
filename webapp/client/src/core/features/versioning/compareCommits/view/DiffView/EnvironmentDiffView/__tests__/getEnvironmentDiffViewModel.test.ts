import { IEnvironmentBlobDiff } from 'core/shared/models/Versioning/Blob/EnvironmentBlob';
import {
  elementDiffMakers,
  makeArrayDiff,
} from 'core/shared/models/Versioning/Blob/Diff';

import { getEnvironmentDiffViewModel } from '../utils';

describe('(feature DiffView) getEnvironmentDiffViewModel', () => {
  describe('common details diff', () => {
    it('should display environment details diff info when it is modified', () => {
      const diff: IEnvironmentBlobDiff = {
        category: 'environment',
        type: 'environment',
        diffType: 'modified',
        location: [],
        data: {
          variables: undefined,
          commandLine: elementDiffMakers.added(['foor']),
        },
      };

      const viewModel = getEnvironmentDiffViewModel(diff);

      expect(viewModel.commonDetails.isHidden).toEqual(false);
    });

    it('should not display environment details diff info when it is not modified', () => {
      const diff: IEnvironmentBlobDiff = {
        category: 'environment',
        type: 'environment',
        diffType: 'modified',
        location: [],
        data: {
          variables: undefined,
          commandLine: undefined,
        },
      };

      const viewModel = getEnvironmentDiffViewModel(diff);

      expect(viewModel.commonDetails.isHidden).toEqual(true);
    });

    describe('command line', () => {
      it('should not display when is not modified', () => {
        const diff: IEnvironmentBlobDiff = {
          category: 'environment',
          type: 'environment',
          diffType: 'modified',
          location: [],
          data: {
            variables: undefined,
            commandLine: undefined,
          },
        };

        const viewModel = getEnvironmentDiffViewModel(diff);

        expect(viewModel.commonDetails.commandLine.isHidden).toEqual(true);
      });

      it('should display command line in B column with full bDiff highlighting when diff status is added', () => {
        const diff: IEnvironmentBlobDiff = {
          category: 'environment',
          type: 'environment',
          diffType: 'added',
          location: [],
          data: {
            commandLine: elementDiffMakers.added(['blob']),
          },
        };

        const viewModel = getEnvironmentDiffViewModel(diff);
        const expected: typeof viewModel.commonDetails.commandLine = {
          isHidden: false,
          A: undefined,
          B: {
            data: ['blob'],
            diffColor: 'bDiff',
            hightlightedPart: 'full',
          },
        };

        expect(viewModel.commonDetails.commandLine.isHidden).toEqual(
          expected.isHidden
        );
        expect(viewModel.commonDetails.commandLine.A).toEqual(expected.A);
        expect(viewModel.commonDetails.commandLine.B).toEqual(expected.B);
      });
    });

    describe('environment variables', () => {
      it('should not display when is not modified', () => {
        const diff: IEnvironmentBlobDiff = {
          category: 'environment',
          type: 'environment',
          diffType: 'modified',
          location: [],
          data: {
            variables: undefined,
          },
        };

        const viewModel = getEnvironmentDiffViewModel(diff);

        expect(viewModel.commonDetails.environmentVariables.isHidden).toEqual(
          true
        );
      });

      it('should display env variables in B column with full bDiff highlighting when diff status is added', () => {
        const diff: IEnvironmentBlobDiff = {
          category: 'environment',
          type: 'environment',
          diffType: 'added',
          location: [],
          data: {
            variables: makeArrayDiff([
              elementDiffMakers.added({ name: 'lang', value: 'en' }),
            ]),
          },
        };

        const viewModel = getEnvironmentDiffViewModel(diff);
        const expected: typeof viewModel.commonDetails.environmentVariables = {
          isHidden: false,
          A: undefined,
          B: [
            {
              data: { name: 'lang', value: 'en' },
              diffColor: 'bDiff',
              hightlightedPart: 'full',
            },
          ],
        };

        expect(viewModel.commonDetails.environmentVariables.isHidden).toEqual(
          expected.isHidden
        );
        expect(viewModel.commonDetails.environmentVariables.A).toEqual(
          expected.A
        );
        expect(viewModel.commonDetails.environmentVariables.B).toEqual(
          expected.B
        );
      });

      it('should display env variables in A column with full aDiff highlighting when diff status is deleted', () => {
        const diff: IEnvironmentBlobDiff = {
          category: 'environment',
          type: 'environment',
          diffType: 'deleted',
          location: [],
          data: {
            variables: makeArrayDiff([
              elementDiffMakers.deleted({ name: 'lang', value: 'en' }),
            ]),
          },
        };

        const viewModel = getEnvironmentDiffViewModel(diff);
        const expected: typeof viewModel.commonDetails.environmentVariables = {
          isHidden: false,
          A: [
            {
              data: { name: 'lang', value: 'en' },
              diffColor: 'aDiff',
              hightlightedPart: 'full',
            },
          ],
          B: undefined,
        };

        expect(viewModel.commonDetails.environmentVariables.isHidden).toEqual(
          expected.isHidden
        );
        expect(viewModel.commonDetails.environmentVariables.A).toEqual(
          expected.A
        );
        expect(viewModel.commonDetails.environmentVariables.B).toEqual(
          expected.B
        );
      });

      it('should highlight only value for modified env variables and should display modified env variables side by side', () => {
        const diff: IEnvironmentBlobDiff = {
          category: 'environment',
          type: 'environment',
          diffType: 'modified',
          location: [],
          data: {
            variables: makeArrayDiff([
              elementDiffMakers.added({ name: 'test', value: 'en' }),
              elementDiffMakers.modified(
                { name: 'lang', value: 'en' },
                { name: 'lang', value: 'ru' }
              ),
            ]),
          },
        };

        const viewModel = getEnvironmentDiffViewModel(diff);
        const expected: typeof viewModel.commonDetails.environmentVariables = {
          isHidden: false,
          A: [
            {
              data: { name: 'lang', value: 'en' },
              diffColor: 'aDiff',
              hightlightedPart: 'value',
            },
          ],
          B: [
            {
              data: { name: 'lang', value: 'ru' },
              diffColor: 'bDiff',
              hightlightedPart: 'value',
            },
            {
              data: { name: 'test', value: 'en' },
              diffColor: 'bDiff',
              hightlightedPart: 'full',
            },
          ],
        };

        expect(viewModel.commonDetails.environmentVariables.isHidden).toEqual(
          expected.isHidden
        );
        expect(viewModel.commonDetails.environmentVariables.A).toEqual(
          expected.A
        );
        expect(viewModel.commonDetails.environmentVariables.B).toEqual(
          expected.B
        );
      });
    });
  });
});
