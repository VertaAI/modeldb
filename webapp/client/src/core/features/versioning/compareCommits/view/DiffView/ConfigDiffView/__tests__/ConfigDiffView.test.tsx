import * as React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import ConfigDiffView from '../ConfigDiffView';
import { IConfigHyperparameter, IConfigBlob } from 'core/shared/models/Versioning/Blob/ConfigBlob';
import { ComparedCommitType } from 'core/shared/models/Versioning/Blob/Diff';
import { findByDataTestAttribute } from 'core/shared/utils/tests/react/helpers';
import { diffColors } from '../../shared/styles';

const comparedCommitsInfo: React.ComponentProps<typeof ConfigDiffView>['comparedCommitsInfo'] = {
    commitA: {
        sha: 'commitASha'
    },
    commitB: {
        sha: 'commitBSHa',
    }
};

type Diff = React.ComponentProps<typeof ConfigDiffView>['diff'];

const makeComponent = ({ diff }: { diff: Diff }) => {
    return mount(<ConfigDiffView comparedCommitsInfo={comparedCommitsInfo} diff={diff} />);    
};

const getDisplayedHyperparameters = (type: ComparedCommitType, component: ReactWrapper) => {
    const commitColumn = component
        .find('tbody')
        .find('tr')
        .first()
        .find('td')
        .at(type === 'A' ? 1 : 2);
    return findByDataTestAttribute('hyperparameter', commitColumn)
        .map((h) => ({
            name: findByDataTestAttribute('name', h).text(),
            value: findByDataTestAttribute('value', h).text(),
            diffColor:
                h.prop('style').backgroundColor === diffColors.green ?
                'green' :
                h.prop('style').backgroundColor === diffColors.red ?
                'red' : undefined,
        }));
};

const hyperparametersToDisplayed = (diffColor: 'green' | 'red', hyperparameters: IConfigHyperparameter[]) => {
    return hyperparameters.map(h => ({
        name: h.name,
        value: String(h.value.value),
        diffColor: diffColor,
    }))
};

const getPropertyRow = (name: keyof IConfigBlob['data'], component: ReactWrapper) => {
    return component
        .find('tbody')
        .at(name === 'hyperparameters' ? 0 : 1);
};

describe('(compareCommits feature) ConfigDiffView', () => {
    describe('Hyperparameters', () => {
        it('should display hyperparameters with green highlithing in the B column when diff status is added', () => {
            const hyperparameters: IConfigHyperparameter[] = [
                { name: 'hyp', value: { type: 'int', value: 123 } },
                { name: 'hyp2', value: { type: 'int', value: 12343 } }
            ];

            const addedDiff: Diff = {
                category: 'config',
                type: 'config',
                diffType: 'added',
                data: {
                    hyperparameters: [
                        {
                            diffType: 'added',
                            B: hyperparameters[0],
                        },
                        {
                            diffType: 'added',
                            B: hyperparameters[1],
                        },
                    ],
                },
                location: [],
            };
            const component = makeComponent({ diff: addedDiff });

            expect(getDisplayedHyperparameters('A', component).length).toEqual(0);
            expect(getDisplayedHyperparameters('B', component))
                .toEqual(hyperparametersToDisplayed('green', hyperparameters));
        });

        it('should display hyperparameters with red highlithing in the A column when diff status is deleted', () => {
            const hyperparameters: IConfigHyperparameter[] = [
                { name: 'hyp', value: { type: 'int', value: 123 } },
                { name: 'hyp2', value: { type: 'int', value: 12343 } }
            ];

            const deletedDiff: Diff = {
                category: 'config',
                type: 'config',
                diffType: 'deleted',
                data: {
                    hyperparameters: [
                        {
                            diffType: 'deleted',
                            A: hyperparameters[0],
                        },
                        {
                            diffType: 'deleted',
                            A: hyperparameters[1],
                        },
                    ],
                },
                location: [],
            };
            const component = makeComponent({ diff: deletedDiff });

            expect(getDisplayedHyperparameters('B', component).length).toEqual(0);
            expect(getDisplayedHyperparameters('A', component))
                .toEqual(hyperparametersToDisplayed('red', hyperparameters));
        });

        it('should display hyperparameters with red highlithing in the A column and hyperparameters with green highlithing in the B column when diff status is modified', () => {
            const hyperparametersA: IConfigHyperparameter[] = [
                { name: 'hyp', value: { type: 'int', value: 123 } },
                { name: 'hyp2', value: { type: 'int', value: 12343 } }
            ];
            const hyperparametersB: IConfigHyperparameter[] = [
                { name: 'hyp2', value: { type: 'int', value: 134134134134 } },
                { name: 'hyp3', value: { type: 'int', value: 12343 } }
            ];
            const deletedDiff: Diff = {
                category: 'config',
                diffType: 'updated',
                type: 'config',
                data: {
                    hyperparameters: [
                        {
                            diffType: 'deleted',
                            A: hyperparametersA[0],
                        },
                        {
                            diffType: 'added',
                            B: hyperparametersB[1],
                        },
                        {
                            diffType: 'updated',
                            A: hyperparametersA[1],
                            B: hyperparametersB[0],
                        },
                    ],
                },
                location: [],
            };
            const component = makeComponent({ diff: deletedDiff });

            expect(getDisplayedHyperparameters('A', component))
                .toEqual(hyperparametersToDisplayed('red', [hyperparametersA[1], hyperparametersA[0]]));
            expect(getDisplayedHyperparameters('B', component))
                .toEqual(hyperparametersToDisplayed('green', [hyperparametersB[0], hyperparametersB[1]]));
        });

        it('should not display hyperparameters row if there are not hyperparameters in commit A and commit B', () => {
            const component = makeComponent({ diff: {
                category: 'config',
                type: 'config',
                diffType: 'added',
                data: {
                    hyperparameters: [],
                },
                location: [],
            } });

            expect(getPropertyRow('hyperparameters', component).length).toEqual(1);
        });
    });
});
