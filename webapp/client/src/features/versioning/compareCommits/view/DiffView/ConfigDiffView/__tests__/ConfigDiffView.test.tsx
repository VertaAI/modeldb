import * as React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import ConfigDiffView from '../ConfigDiffView';
import {
  IConfigHyperparameter,
  IConfigBlob,
} from 'shared/models/Versioning/Blob/ConfigBlob';
import {
  ComparedCommitType,
  elementDiffMakers,
} from 'shared/models/Versioning/Blob/Diff';
import { findByDataTestAttribute } from 'shared/utils/tests/react/helpers';
import { DiffColor } from '../../../model';
import {
  getCommitColumns,
  getDiffColorFromBackgroundColor,
  getColumnDiffColor,
  getCommitColumnInfo,
} from '../../shared/ComparePropertiesTable/__tests__/helpers';

const comparedCommitsInfo: React.ComponentProps<
  typeof ConfigDiffView
>['comparedCommitsInfo'] = {
  commitA: {
    sha: 'commitASha',
  },
  commitB: {
    sha: 'commitBSHa',
  },
};

type Diff = React.ComponentProps<typeof ConfigDiffView>['diff'];

const makeComponent = ({ diff }: { diff: Diff }) => {
  return mount(
    <ConfigDiffView comparedCommitsInfo={comparedCommitsInfo} diff={diff} />
  );
};

const getDisplayedHyperparameters = (
  type: ComparedCommitType,
  component: ReactWrapper
) => {
  const commitColumns = getCommitColumns(type, component);

  return getCommitColumnInfo(
    type,
    'hyperparameter',
    column =>
      findByDataTestAttribute('hyperparameter', column).map(hyp => {
        return {
          name: findByDataTestAttribute('name', hyp).text(),
          value: findByDataTestAttribute('value', hyp).text(),
        };
      }),
    component
  );

  return commitColumns
    .map(column => {
      const hyperparameter = findByDataTestAttribute('hyperparameter', column);
      const diffInfo: IDiffInfo = {
        cell: getColumnDiffColor(column),
        fullItem:
          hyperparameter.length > 1
            ? getDiffColorFromBackgroundColor(
                hyperparameter.first().prop('style')
              )
            : undefined,
        value:
          hyperparameter.length > 1
            ? getDiffColorFromBackgroundColor(
                findByDataTestAttribute('value', hyperparameter)
                  .first()
                  .prop('style')
              )
            : undefined,
      };
      return {
        name:
          hyperparameter.length > 1
            ? findByDataTestAttribute('name', hyperparameter)
                .first()
                .text()
            : undefined,
        value:
          hyperparameter.length > 1
            ? findByDataTestAttribute('value', hyperparameter)
                .first()
                .text()
            : undefined,
        diffInfo,
      };
    })
    .filter(({ name, value }) => name && value);
};

interface IDiffInfo {
  cell: DiffColor | undefined;
  fullItem: DiffColor | undefined;
  value: DiffColor | undefined;
}

const hyperparametersToDisplayed = (
  diffColor: DiffColor,
  part: 'cell' | 'fullItem' | 'value',
  hyperparameters: IConfigHyperparameter[]
) => {
  return hyperparameters.map(h => {
    const diffInfo: IDiffInfo = {
      cell: part === 'cell' ? diffColor : undefined,
      fullItem: part === 'fullItem' ? diffColor : undefined,
      value: part === 'value' ? diffColor : undefined,
    };
    return {
      name: h.name,
      value: String(h.value.value),
      diffInfo,
    };
  });
};

const getPropertyRow = (
  name: keyof IConfigBlob['data'],
  component: ReactWrapper
) => {
  return component.find('tbody').at(name === 'hyperparameters' ? 0 : 1);
};

describe.skip('(compareCommits feature) ConfigDiffView', () => {
  describe('Hyperparameters', () => {
    it('should display hyperparameters with bDiff highlithing in the B column when diff status is added', () => {
      const hyperparameters: IConfigHyperparameter[] = [
        { name: 'hyp', value: { type: 'int', value: 123 } },
        { name: 'hyp2', value: { type: 'int', value: 12343 } },
      ];

      const addedDiff: Diff = {
        category: 'config',
        type: 'config',
        diffType: 'added',
        data: {
          hyperparameters: [
            elementDiffMakers.added(hyperparameters[0]),
            elementDiffMakers.added(hyperparameters[1]),
          ],
        },
        location: [],
      };
      const component = makeComponent({ diff: addedDiff });

      expect(getDisplayedHyperparameters('A', component)).toEqual({
        content: undefined,
        diffColor: undefined,
      });
      expect(getDisplayedHyperparameters('B', component)).toEqual(
        hyperparametersToDisplayed('bDiff', 'cell', hyperparameters)
      );
    });

    it('should display hyperparameters with red highlithing in the A column when diff status is deleted', () => {
      const hyperparameters: IConfigHyperparameter[] = [
        { name: 'hyp', value: { type: 'int', value: 123 } },
        { name: 'hyp2', value: { type: 'int', value: 12343 } },
      ];

      const deletedDiff: Diff = {
        category: 'config',
        type: 'config',
        diffType: 'deleted',
        data: {
          hyperparameters: [
            elementDiffMakers.deleted(hyperparameters[0]),
            elementDiffMakers.deleted(hyperparameters[1]),
          ],
        },
        location: [],
      };
      const component = makeComponent({ diff: deletedDiff });

      expect(getDisplayedHyperparameters('B', component)).toEqual(0);
      expect(getDisplayedHyperparameters('A', component)).toEqual(
        hyperparametersToDisplayed('aDiff', 'cell', hyperparameters)
      );
    });

    it('should display hyperparameters with red highlithing in the A column and hyperparameters with bDiff highlithing in the B column when diff status is modified', () => {
      const hyperparametersA: IConfigHyperparameter[] = [
        { name: 'hyp', value: { type: 'int', value: 123 } },
        { name: 'hyp2', value: { type: 'int', value: 12343 } },
      ];
      const hyperparametersB: IConfigHyperparameter[] = [
        { name: 'hyp2', value: { type: 'int', value: 134134134134 } },
        { name: 'hyp3', value: { type: 'int', value: 12343 } },
      ];
      const deletedDiff: Diff = {
        category: 'config',
        diffType: 'modified',
        type: 'config',
        data: {
          hyperparameters: [
            elementDiffMakers.deleted(hyperparametersA[0]),
            elementDiffMakers.added(hyperparametersB[1]),
            elementDiffMakers.modified(
              hyperparametersA[1],
              hyperparametersB[0]
            ),
          ],
        },
        location: [],
      };
      const component = makeComponent({ diff: deletedDiff });

      expect(getDisplayedHyperparameters('A', component)).toEqual(
        hyperparametersToDisplayed('aDiff', 'value', [
          hyperparametersA[1],
          hyperparametersA[0],
        ])
      );
      expect(getDisplayedHyperparameters('B', component)).toEqual(
        hyperparametersToDisplayed('bDiff', 'fullItem', [
          hyperparametersB[0],
          hyperparametersB[1],
        ])
      );
    });

    it('should not display hyperparameters row if there are not hyperparameters in commit A and commit B', () => {
      const component = makeComponent({
        diff: {
          category: 'config',
          type: 'config',
          diffType: 'added',
          data: {
            hyperparameters: [],
          },
          location: [],
        },
      });

      expect(getPropertyRow('hyperparameters', component).length).toEqual(1);
    });
  });
});
