import { ReactWrapper, ShallowWrapper } from 'enzyme';
import * as React from 'react';
import { action } from 'typesafe-actions';

import { IArtifact } from 'core/shared/models/Artifact';
import { IAttribute } from 'core/shared/models/Attribute';
import { ICodeVersion, IGitCodeVersion } from 'core/shared/models/CodeVersion';
import { IHyperparameter } from 'core/shared/models/HyperParameters';
import { IMetric } from 'core/shared/models/Metrics';
import cloneClassInstance from 'core/shared/utils/cloneClassInstance';
import flushAllPromises from 'core/shared/utils/tests/integrations/flushAllPromises';
import { findByDataTestAttribute } from 'core/shared/utils/tests/react/helpers';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import { ShortExperiment } from 'models/Experiment';
import ModelRecord from 'models/ModelRecord';
import { Project } from 'models/Project';
import ExperimentRunsDataService from 'services/experimentRuns/ExperimentRunsDataService';
import {
  IProjectsState,
  projectsReducer,
  loadProjectActionTypes,
} from 'store/projects';
import makeMountComponentWithPredefinedData from 'utils/tests/integrations/makeMountComponentWithPredefinedData';
import diffHighlightStyles from '../../shared/DiffHighlight/DiffHighlight.module.css';

import CompareModels, { ICompareModelsLocalProps } from '../CompareModels';
import CompareModelsTable from '../CompareModelsTable/CompareModelsTable';

jest.mock('services/experimentRuns/ExperimentRunsDataService');

const getProjectsState = (project: Project): IProjectsState => {
  return projectsReducer(
    undefined,
    action(loadProjectActionTypes.SUCCESS, { project })
  );
};

const mockProject: Project = new Project();
mockProject.id = 'project-id';
mockProject.name = 'project';

const renderComponent = async ({
  experimentRun1,
  experimentRun2,
}: {
  experimentRun1: ModelRecord;
  experimentRun2: ModelRecord;
}) => {
  const defaultProps: ICompareModelsLocalProps = {
    comparedModelIds: [experimentRun1.id, experimentRun2.id],
    projectId: mockProject.id,
  };

  (ExperimentRunsDataService as any).mockImplementation(() => ({
    loadModelRecord: jest.fn(id =>
      Promise.resolve({
        experimentRun:
          experimentRun1.id === id ? experimentRun1 : experimentRun2,
        comments: [],
      })
    ),
  }));

  const integratingTestData = await makeMountComponentWithPredefinedData({
    Component: () => <CompareModels {...defaultProps} />,
    settings: {
      initialState: {
        projects: getProjectsState(mockProject),
      },
    },
  });

  await flushAllPromises();
  await flushAllPromises();

  integratingTestData.component.update();

  return integratingTestData;
};

const makeMockExperimentRun = ({
  id,
  name,
  shortExperiment,
}: {
  id: string;
  name: string;
  shortExperiment: ShortExperiment;
}): ModelRecord => {
  const experimentRun = new ModelRecord();
  experimentRun.id = id;
  experimentRun.name = name;
  experimentRun.tags = [];
  experimentRun.artifacts = [];
  experimentRun.attributes = [];
  experimentRun.metrics = [];
  experimentRun.observations = [];
  experimentRun.hyperparameters = [];

  experimentRun.shortExperiment = shortExperiment;
  experimentRun.experimentId = shortExperiment.id;

  return experimentRun;
};
const updateExperimentRun = (
  {
    shortExperiment,
    hyperparameters,
    metrics,
    attributes,
    artifacts,
    codeVersion,
  }: {
    shortExperiment?: ShortExperiment;
    hyperparameters?: IHyperparameter[];
    metrics?: IMetric[];
    attributes?: IAttribute[];
    artifacts?: IArtifact[];
    codeVersion?: ICodeVersion;
  },
  experimentRun: ModelRecord
): ModelRecord => {
  const newExperimentRun = cloneClassInstance(experimentRun);
  if (shortExperiment) {
    newExperimentRun.shortExperiment = shortExperiment;
    newExperimentRun.id = shortExperiment.id;
  }
  if (hyperparameters) {
    newExperimentRun.hyperparameters = hyperparameters;
  }
  if (metrics) {
    newExperimentRun.metrics = metrics;
  }
  if (attributes) {
    newExperimentRun.attributes = attributes;
  }
  if (artifacts) {
    newExperimentRun.artifacts = artifacts;
  }
  if (codeVersion) {
    newExperimentRun.codeVersion = codeVersion;
  }
  return newExperimentRun;
};

const checkBgHighlighting = (
  modelType: 'first' | 'second',
  component: ReactWrapper<any, any> | ShallowWrapper<any, any>
) => {
  return component
    .getDOMNode()
    .classList.contains(
      diffHighlightStyles[
        `highlightBg_${modelType === 'first' ? 'entity1' : 'entity2'}`
      ]
    );
};

const checkBorderHighlighting = (
  modelType: 'first' | 'second',
  component: ReactWrapper<any, any> | ShallowWrapper<any, any>
) => {
  return component
    .getDOMNode()
    .classList.contains(
      diffHighlightStyles[
        `highlightBorder_${modelType === 'first' ? 'entity1' : 'entity2'}`
      ]
    );
};

const getDisplayedCodeVersion = (
  codeVersionType: ICodeVersion['type'],
  modelType: 'first' | 'second',
  component: ReactWrapper<any, any>
) => {
  if (codeVersionType === 'artifact') {
    const artifactsInfo = component
      .find('[data-test="artifact"]')
      .map(artifact => ({
        button: artifact,
        key: {
          data: artifact.find('[data-test="artifact-key"]').text(),
          elem: artifact.find('[data-test="artifact-key"]'),
        },
      }));
    return artifactsInfo.length === 1
      ? artifactsInfo[0]
      : artifactsInfo[modelType === 'first' ? 0 : 1];
  }
  {
    const gitCodeVersionsInfo = component
      .find('[data-test="git-code-version"]')
      .map(gitCodeVersionButton => ({
        button: gitCodeVersionButton,
      }));
    return gitCodeVersionsInfo.length === 1
      ? gitCodeVersionsInfo[0]
      : gitCodeVersionsInfo[modelType === 'first' ? 0 : 1];
  }
};

const openCompareCodeVersionsPopup = (
  codeVersionType: ICodeVersion['type'],
  modelType: 'first' | 'second',
  component: ReactWrapper<any, any>
) => {
  getDisplayedCodeVersion(
    codeVersionType,
    modelType,
    component
  ).button.simulate('click');
};

describe('components', () => {
  describe('CompareModels', () => {
    const experimentRun1 = makeMockExperimentRun({
      id: 'exprRun-id-1',
      name: 'exprRun-name-1',
      shortExperiment: {
        id: 'exprRun-1-experiment-id',
        name: 'experiment',
      },
    });

    const experimentRun2 = makeMockExperimentRun({
      id: 'exprRun-id-2',
      name: 'exprRun-name-2',
      shortExperiment: {
        id: 'exprRun-2-experiment-id',
        name: 'experiment',
      },
    });

    it('should display table with comparing experiment runs info if experiment runs are loaded', async () => {
      const { component } = await renderComponent({
        experimentRun1,
        experimentRun2,
      });

      expect(component.find(Preloader).length).toBe(0);
      expect(component.find(CompareModelsTable).length).toBe(1);
    });

    describe('property "id"', () => {
      it('should render comparing row for property', async () => {
        const { component } = await renderComponent({
          experimentRun1,
          experimentRun2,
        });

        expect(
          findByDataTestAttribute('property-name-id', component).length
        ).toBe(1);
        expect(
          findByDataTestAttribute('property-name-id', component).text()
        ).toBe('ID');
      });

      it('should render correct value for the first experiment run', async () => {
        const { component } = await renderComponent({
          experimentRun1,
          experimentRun2,
        });

        expect(
          findByDataTestAttribute('property-value-id', component)
            .at(0)
            .text()
        ).toBe(experimentRun1.id);
      });

      it('should render correct value for the second experiment run', async () => {
        const { component } = await renderComponent({
          experimentRun1,
          experimentRun2,
        });

        expect(
          findByDataTestAttribute('property-value-id', component)
            .at(1)
            .text()
        ).toBe(experimentRun2.id);
      });

      it('should highlight background ids values', async () => {
        const { component } = await renderComponent({
          experimentRun1,
          experimentRun2,
        });
        const firstIdElem = findByDataTestAttribute(
          'property-value-id',
          component
        ).at(0);
        const secondIdElem = findByDataTestAttribute(
          'property-value-id',
          component
        ).at(1);

        expect(checkBgHighlighting('first', firstIdElem)).toBe(true);
        expect(checkBgHighlighting('second', secondIdElem)).toBe(true);
      });
    });

    describe('property "experiment"', () => {
      it('should render comparing row for property', async () => {
        const { component } = await renderComponent({
          experimentRun1,
          experimentRun2,
        });

        expect(
          findByDataTestAttribute('property-name-experimentId', component)
            .length
        ).toBe(1);
        expect(
          findByDataTestAttribute(
            'property-name-experimentId',
            component
          ).text()
        ).toBe('Experiment');
      });

      it('should render correct value for the first experiment run', async () => {
        const { component } = await renderComponent({
          experimentRun1,
          experimentRun2,
        });

        expect(
          findByDataTestAttribute('property-value-experimentId', component)
            .at(0)
            .text()
        ).toBe(experimentRun1.shortExperiment.name);
      });

      it('should render correct value for the second experiment run', async () => {
        const { component } = await renderComponent({
          experimentRun1,
          experimentRun2,
        });

        expect(
          findByDataTestAttribute('property-value-experimentId', component)
            .at(1)
            .text()
        ).toBe(experimentRun2.shortExperiment.name);
      });

      it('should highlight background experiments values if they are not equal', async () => {
        const { component } = await renderComponent({
          experimentRun1: updateExperimentRun(
            {
              shortExperiment: { name: 'experiment-1', id: 'experiment-1-id' },
            },
            experimentRun1
          ),
          experimentRun2: updateExperimentRun(
            {
              shortExperiment: { name: 'experiment-2', id: 'experiment-2-id' },
            },
            experimentRun2
          ),
        });
        const firstIdElem = findByDataTestAttribute(
          'property-value-experimentId',
          component
        ).at(0);
        const secondIdElem = findByDataTestAttribute(
          'property-value-experimentId',
          component
        ).at(1);

        expect(checkBgHighlighting('first', firstIdElem)).toBe(true);
        expect(checkBgHighlighting('second', secondIdElem)).toBe(true);
      });

      it('should not highlight background owners values if they are equal', async () => {
        const { component } = await renderComponent({
          experimentRun1: updateExperimentRun(
            { shortExperiment: { name: 'experiment', id: 'experiment-id' } },
            experimentRun1
          ),
          experimentRun2: updateExperimentRun(
            { shortExperiment: { name: 'experiment', id: 'experiment-id' } },
            experimentRun2
          ),
        });
        const firstIdElem = findByDataTestAttribute(
          'property-value-experimentId',
          component
        ).at(0);
        const secondIdElem = findByDataTestAttribute(
          'property-value-experimentId',
          component
        ).at(1);

        expect(checkBgHighlighting('first', firstIdElem)).toBe(false);
        expect(checkBgHighlighting('second', secondIdElem)).toBe(false);
      });
    });

    describe('property "project"', () => {
      it('should render comparing row for property', async () => {
        const { component } = await renderComponent({
          experimentRun1,
          experimentRun2,
        });

        expect(
          findByDataTestAttribute('property-name-projectId', component).length
        ).toBe(1);
        expect(
          findByDataTestAttribute('property-name-projectId', component).text()
        ).toBe('Project');
      });

      it('should render correct value for the first experiment run', async () => {
        const { component } = await renderComponent({
          experimentRun1,
          experimentRun2,
        });

        expect(
          findByDataTestAttribute('property-value-projectId', component)
            .at(0)
            .text()
        ).toBe(mockProject.name);
      });

      it('should render correct value for the second experiment run', async () => {
        const { component } = await renderComponent({
          experimentRun1,
          experimentRun2,
        });

        expect(
          findByDataTestAttribute('property-value-projectId', component)
            .at(1)
            .text()
        ).toBe(mockProject.name);
      });
    });

    describe('property "Hyperparameters"', () => {
      const getDisplayedHyperparameters = (
        modelType: 'first' | 'second',
        component: ReactWrapper<any, any>
      ) => {
        return component
          .find('[data-test="hyperparameters"]')
          .at(modelType === 'first' ? 0 : 1)
          .find('[data-test="hyperparameter"]')
          .map(hyperparameterElem => ({
            key: {
              data: hyperparameterElem
                .find('[data-test="hyperparameter-key"]')
                .text(),
              elem: hyperparameterElem.find('[data-test="hyperparameter-key"]'),
            },
            value: {
              data: hyperparameterElem
                .find('[data-test="hyperparameter-value"]')
                .text(),
              elem: hyperparameterElem.find(
                '[data-test="hyperparameter-value"]'
              ),
            },
          }));
      };

      it('should render comparing row for property', async () => {
        const { component } = await renderComponent({
          experimentRun1,
          experimentRun2,
        });

        expect(
          findByDataTestAttribute('property-name-hyperparameters', component)
            .length
        ).toBe(1);
        expect(
          findByDataTestAttribute(
            'property-name-hyperparameters',
            component
          ).text()
        ).toBe('Hyperparameters');
      });

      it('should render correct value for the first experiment run', async () => {
        const experimentRun1Hyperparameters = [
          { key: 'C', value: 0.000001 },
          { key: 'solver', value: 'lbfgs' },
          { key: 'max_iter', value: 15 },
        ];
        const { component } = await renderComponent({
          experimentRun1: updateExperimentRun(
            { hyperparameters: experimentRun1Hyperparameters },
            experimentRun1
          ),
          experimentRun2: updateExperimentRun(
            { hyperparameters: [] },
            experimentRun2
          ),
        });

        const displayedHyperparameters = getDisplayedHyperparameters(
          'first',
          component
        );
        expect(displayedHyperparameters.length).toBe(
          experimentRun1Hyperparameters.length
        );
        experimentRun1Hyperparameters.forEach(({ key, value }, i) => {
          expect(displayedHyperparameters[i].key.data).toEqual(key);
          expect(displayedHyperparameters[i].value.data).toEqual(String(value));
        });
      });

      it('should render correct value for the second experiment run', async () => {
        const experimentRun2Hyperparameters = [
          { key: 'C', value: 0.000001 },
          { key: 'solver', value: 'lbfgs' },
          { key: 'max_iter', value: 15 },
        ];
        const { component } = await renderComponent({
          experimentRun1: updateExperimentRun(
            { hyperparameters: [] },
            experimentRun1
          ),
          experimentRun2: updateExperimentRun(
            { hyperparameters: experimentRun2Hyperparameters },
            experimentRun2
          ),
        });

        const displayedHyperparameters = getDisplayedHyperparameters(
          'second',
          component
        );
        expect(displayedHyperparameters.length).toBe(
          experimentRun2Hyperparameters.length
        );
        experimentRun2Hyperparameters.forEach(({ key, value }, i) => {
          expect(displayedHyperparameters[i].key.data).toEqual(key);
          expect(displayedHyperparameters[i].value.data).toEqual(String(value));
        });
      });

      it('should highlight background of hyperparameter value if hyperparameters with the same key have not the same value or if hyperparameter with key is not existed', async () => {
        const experimentRun1Hyperparameters = [
          { key: 'C', value: 0.000001 },
          { key: 'solver', value: 'lbfgs' },
          { key: 'max_iter', value: 15 },
          { key: 'val_acc', value: 125 },
        ];
        const experimentRun2Hyperparameters = [
          { key: 'C', value: 0.2 },
          { key: 'solver', value: 'lbfgs' },
          { key: 'max_iter', value: 28 },
        ];
        const expectedResult = [
          { key: 'C', isDiff: true },
          { key: 'solver', isDiff: false },
          { key: 'max_iter', isDiff: true },
          { key: 'val_acc', isDiff: true },
        ];
        const { component } = await renderComponent({
          experimentRun1: updateExperimentRun(
            { hyperparameters: experimentRun1Hyperparameters },
            experimentRun1
          ),
          experimentRun2: updateExperimentRun(
            { hyperparameters: experimentRun2Hyperparameters },
            experimentRun2
          ),
        });

        const displayedHyperparameters1 = getDisplayedHyperparameters(
          'first',
          component
        );
        const displayedHyperparameters2 = getDisplayedHyperparameters(
          'second',
          component
        );
        expectedResult.forEach(({ key, isDiff }) => {
          const targetDisplayedHyperparameter1 = displayedHyperparameters1.find(
            h => h.key.data === key
          );
          const targetDisplayedHyperparameter2 = displayedHyperparameters2.find(
            h => h.key.data === key
          );
          if (targetDisplayedHyperparameter1) {
            expect(
              checkBgHighlighting(
                'first',
                targetDisplayedHyperparameter1.value.elem
              )
            ).toBe(isDiff);
          }
          if (targetDisplayedHyperparameter2) {
            expect(
              checkBgHighlighting(
                'second',
                targetDisplayedHyperparameter2.value.elem
              )
            ).toBe(isDiff);
          }
        });
      });
    });

    describe('property "Metrics"', () => {
      const getDisplayedMetrics = (
        modelType: 'first' | 'second',
        component: ReactWrapper<any, any>
      ) => {
        return component
          .find('[data-test="metrics"]')
          .at(modelType === 'first' ? 0 : 1)
          .find('[data-test="metric"]')
          .map(metricElem => ({
            key: {
              data: metricElem.find('[data-test="metric-key"]').text(),
              elem: metricElem.find('[data-test="metric-key"]'),
            },
            value: {
              data: metricElem.find('[data-test="metric-value"]').text(),
              elem: metricElem.find('[data-test="metric-value"]'),
            },
          }));
      };

      it('should render comparing row for property', async () => {
        const { component } = await renderComponent({
          experimentRun1,
          experimentRun2,
        });

        expect(
          findByDataTestAttribute('property-name-metrics', component).length
        ).toBe(1);
        expect(
          findByDataTestAttribute('property-name-metrics', component).text()
        ).toBe('Metrics');
      });

      it('should render correct value for the first experiment run', async () => {
        const experimentRun1Metrics = [{ key: 'val_acc', value: 0.7852 }];
        const { component } = await renderComponent({
          experimentRun1: updateExperimentRun(
            { metrics: experimentRun1Metrics },
            experimentRun1
          ),
          experimentRun2: updateExperimentRun({ metrics: [] }, experimentRun2),
        });

        const displayedMetrics = getDisplayedMetrics('first', component);
        expect(displayedMetrics.length).toBe(experimentRun1Metrics.length);
        experimentRun1Metrics.forEach(({ key, value }, i) => {
          expect(displayedMetrics[i].key.data).toEqual(key);
          expect(displayedMetrics[i].value.data).toEqual(String(value));
        });
      });

      it('should render correct value for the second experiment run', async () => {
        const experimentRun2Metrics = [{ key: 'val_acc', value: 0.7852 }];
        const { component } = await renderComponent({
          experimentRun1: updateExperimentRun({ metrics: [] }, experimentRun1),
          experimentRun2: updateExperimentRun(
            { metrics: experimentRun2Metrics },
            experimentRun2
          ),
        });

        const displayedMetrics = getDisplayedMetrics('second', component);
        expect(displayedMetrics.length).toBe(experimentRun2Metrics.length);
        experimentRun2Metrics.forEach(({ key, value }, i) => {
          expect(displayedMetrics[i].key.data).toEqual(key);
          expect(displayedMetrics[i].value.data).toEqual(String(value));
        });
      });

      it('should highlight background of metric value if metrics with the same key have not the same value or if metric with key is not existed', async () => {
        const experimentRun1Metrics = [{ key: 'val_acc', value: 0.000001 }];
        const experimentRun2Metrics = [{ key: 'val_acc', value: 0.2 }];
        const expectedResult = [{ key: 'val_acc', isDiff: true }];
        const { component } = await renderComponent({
          experimentRun1: updateExperimentRun(
            { metrics: experimentRun1Metrics },
            experimentRun1
          ),
          experimentRun2: updateExperimentRun(
            { metrics: experimentRun2Metrics },
            experimentRun2
          ),
        });

        const displayedMetrics1 = getDisplayedMetrics('first', component);
        const displayedMetrics2 = getDisplayedMetrics('second', component);
        expectedResult.forEach(({ key, isDiff }) => {
          const targetDisplayedMetric1 = displayedMetrics1.find(
            h => h.key.data === key
          );
          const targetDisplayedMetric2 = displayedMetrics2.find(
            h => h.key.data === key
          );
          if (targetDisplayedMetric1) {
            expect(
              checkBgHighlighting('first', targetDisplayedMetric1.value.elem)
            ).toBe(isDiff);
          }
          if (targetDisplayedMetric2) {
            expect(
              checkBgHighlighting('second', targetDisplayedMetric2.value.elem)
            ).toBe(isDiff);
          }
        });
      });
    });

    describe('property "Attributes"', () => {
      const getDisplayedAttributes = (
        modelType: 'first' | 'second',
        component: ReactWrapper<any, any>
      ) => {
        return component
          .find('[data-test="attributes"]')
          .at(modelType === 'first' ? 0 : 1)
          .find('[data-test="attribute"]')
          .map(attributeElem => ({
            button: attributeElem,
            key: {
              data: attributeElem.find('[data-test="attribute-key"]').text(),
              elem: attributeElem.find('[data-test="attribute-key"]'),
            },
          }));
      };

      it('should render comparing row for property', async () => {
        const { component } = await renderComponent({
          experimentRun1,
          experimentRun2,
        });

        expect(
          findByDataTestAttribute('property-name-attributes', component).length
        ).toBe(1);
        expect(
          findByDataTestAttribute('property-name-attributes', component).text()
        ).toBe('Attributes');
      });

      it('should highlight border of attribute button if attributes with the same key have not the same value or if attribute with key is not existed', async () => {
        const experimentRun1Attributes = [
          { key: 'attribute-1', value: 0.000001 },
          { key: 'attribute-2', value: 'lbfgs' },
          { key: 'attribute-3', value: 15 },
        ];
        const experimentRun2Attributes = [
          { key: 'attribute-1', value: 0.2 },
          { key: 'attribute-2', value: 'lbfgs' },
        ];
        const expectedResult = [
          { key: 'attribute-1', isDiff: true },
          { key: 'attribute-2', isDiff: false },
          { key: 'attribute-3', isDiff: true },
        ];
        const { component } = await renderComponent({
          experimentRun1: updateExperimentRun(
            { attributes: experimentRun1Attributes },
            experimentRun1
          ),
          experimentRun2: updateExperimentRun(
            { attributes: experimentRun2Attributes },
            experimentRun2
          ),
        });

        const displayedAttributes1 = getDisplayedAttributes('first', component);
        const displayedAttributes2 = getDisplayedAttributes(
          'second',
          component
        );
        expectedResult.forEach(({ key, isDiff }) => {
          const targetDisplayedAttributes1 = displayedAttributes1.find(
            h => h.key.data === key
          );
          const targetDisplayedAttributes2 = displayedAttributes2.find(
            h => h.key.data === key
          );
          if (targetDisplayedAttributes1) {
            expect(
              checkBorderHighlighting(
                'first',
                targetDisplayedAttributes1.button
              )
            ).toBe(isDiff);
          }
          if (targetDisplayedAttributes2) {
            expect(
              checkBorderHighlighting(
                'second',
                targetDisplayedAttributes2.button
              )
            ).toBe(isDiff);
          }
        });
      });
    });

    describe('property "Artifacts"', () => {
      const getDisplayedArtifacts = (
        modelType: 'first' | 'second',
        component: ReactWrapper<any, any>
      ) => {
        return component
          .find('[data-test="artifacts"]')
          .at(modelType === 'first' ? 0 : 1)
          .find('[data-test="artifact"]')
          .map(artifact => ({
            button: artifact,
            key: {
              data: artifact.find('[data-test="artifact-key"]').text(),
              elem: artifact.find('[data-test="artifact-key"]'),
            },
          }));
      };

      it('should render comparing row for property', async () => {
        const { component } = await renderComponent({
          experimentRun1,
          experimentRun2,
        });

        expect(
          findByDataTestAttribute('property-name-artifacts', component).length
        ).toBe(1);
        expect(
          findByDataTestAttribute('property-name-artifacts', component).text()
        ).toBe('Artifacts');
      });

      it('should highlight border of artifact button if artifacts with the same keys have not the same type or path or linkedArtifactId or if artifact with key is not existed', async () => {
        const testCases: Array<{
          artifact1: IArtifact;
          artifact2?: IArtifact;
          isDiff: boolean;
        }> = [
          {
            artifact1: {
              key: 'artifact-1',
              type: 'BINARY',
              path: 'artifact-1-path',
              pathOnly: false,
            },
            artifact2: {
              key: 'artifact-1',
              type: 'BINARY',
              path: 'artifact-1-path',
              pathOnly: false,
            },
            isDiff: false,
          },
          {
            artifact1: {
              key: 'artifact-1',
              type: 'CODE',
              path: 'artifact-1-path',
              pathOnly: false,
            },
            artifact2: {
              key: 'artifact-1',
              type: 'BINARY',
              path: 'artifact-1-path',
              pathOnly: false,
            },
            isDiff: true,
          },
          {
            artifact1: {
              key: 'artifact-1',
              type: 'BINARY',
              path: 'artifact-1-path',
              pathOnly: false,
            },
            artifact2: {
              key: 'artifact-1',
              type: 'BINARY',
              path: 'artifact-2-path',
              pathOnly: false,
            },
            isDiff: true,
          },
          {
            artifact1: {
              key: 'artifact-1',
              type: 'BINARY',
              path: 'artifact-1-path',
              pathOnly: false,
            },
            artifact2: undefined,
            isDiff: true,
          },
        ];

        for (const testCase of testCases) {
          const { component } = await renderComponent({
            experimentRun1: updateExperimentRun(
              { artifacts: [testCase.artifact1] },
              experimentRun1
            ),
            experimentRun2: updateExperimentRun(
              { artifacts: testCase.artifact2 ? [testCase.artifact2] : [] },
              experimentRun2
            ),
          });

          const displayedArtifact1 = getDisplayedArtifacts(
            'first',
            component
          )[0];
          const displayedArtifact2 = getDisplayedArtifacts(
            'second',
            component
          )[0];
          expect(
            checkBorderHighlighting('first', displayedArtifact1.button)
          ).toBe(testCase.isDiff);
          if (displayedArtifact2) {
            expect(
              checkBorderHighlighting('second', displayedArtifact2.button)
            ).toBe(testCase.isDiff);
          }
        }
      });
    });

    describe('property "Code version"', () => {
      it('should render comparing row for property', async () => {
        const { component } = await renderComponent({
          experimentRun1,
          experimentRun2,
        });

        expect(
          findByDataTestAttribute('property-name-codeVersion', component).length
        ).toBe(1);
        expect(
          findByDataTestAttribute('property-name-codeVersion', component).text()
        ).toBe('Code Version');
      });

      it('should highlight border of code versions buttons if code versions have different types', async () => {
        const { component } = await renderComponent({
          experimentRun1: updateExperimentRun(
            {
              codeVersion: {
                type: 'git',
                data: {},
              },
            },
            experimentRun1
          ),
          experimentRun2: updateExperimentRun(
            {
              codeVersion: {
                type: 'artifact',
                data: {
                  key: 'artifact-code-version',
                  path: 'artifact-path',
                  pathOnly: false,
                  type: 'CODE',
                },
              },
            },
            experimentRun2
          ),
        });

        expect(
          checkBorderHighlighting(
            'first',
            getDisplayedCodeVersion('git', 'first', component).button
          )
        ).toBe(true);
        expect(
          checkBorderHighlighting(
            'second',
            getDisplayedCodeVersion('artifact', 'second', component).button
          )
        ).toBe(true);
      });

      describe('when code versions type are "git"', () => {
        it('should higlight border of git buttons if exec path or remote repo url or commit hash is different', async () => {
          const testCases: Array<{
            codeVersion1: IGitCodeVersion;
            codeVersion2: IGitCodeVersion;
            isDifferent: boolean;
          }> = [
            {
              codeVersion1: {
                type: 'git',
                data: {
                  commitHash: 'commitHash',
                  execPath: 'exectPath',
                  remoteRepoUrl: 'git@github.com:VertaAI/modeldb-client.git',
                },
              },
              codeVersion2: {
                type: 'git',
                data: {
                  commitHash: 'commitHash',
                  execPath: 'exectPath',
                  remoteRepoUrl: 'git@github.com:VertaAI/modeldb-client.git',
                },
              },
              isDifferent: false,
            },
            {
              codeVersion1: {
                type: 'git',
                data: {
                  commitHash: 'commitHash-1',
                  execPath: 'exectPath',
                  remoteRepoUrl: 'git@github.com:VertaAI/modeldb-client.git',
                },
              },
              codeVersion2: {
                type: 'git',
                data: {
                  commitHash: 'commitHash-2',
                  execPath: 'exectPath',
                  remoteRepoUrl: 'git@github.com:VertaAI/modeldb-client.git',
                },
              },
              isDifferent: true,
            },
            {
              codeVersion1: {
                type: 'git',
                data: {
                  commitHash: 'commitHash',
                  execPath: 'exectPath-1',
                  remoteRepoUrl: 'git@github.com:VertaAI/modeldb-client.git',
                },
              },
              codeVersion2: {
                type: 'git',
                data: {
                  commitHash: 'commitHash',
                  execPath: 'exectPath-2',
                  remoteRepoUrl: 'git@github.com:VertaAI/modeldb-client.git',
                },
              },
              isDifferent: true,
            },
          ];

          for (const testCase of testCases) {
            const { component } = await renderComponent({
              experimentRun1: updateExperimentRun(
                { codeVersion: testCase.codeVersion1 },
                experimentRun1
              ),
              experimentRun2: updateExperimentRun(
                { codeVersion: testCase.codeVersion2 },
                experimentRun2
              ),
            });
            (['first', 'second'] as const).forEach(modelType => {
              const codeVersionInfo = getDisplayedCodeVersion(
                'git',
                modelType,
                component
              );
              if (codeVersionInfo) {
                expect(
                  checkBorderHighlighting(modelType, codeVersionInfo.button)
                ).toBe(testCase.isDifferent);
              }
            });
          }
        });

        it('should display "Compare Code" button if remote repo urls are not equals and commit hashs are not existed', async () => {
          const testCases = [
            {
              gitCodeVersion1Data: {
                remoteRepoUrl: 'git@github.com:VertaAI/modeldb-client.git',
                commitHash: '0db2e34e00f34fca818d49f682fbcecfc20514a6',
              },
              gitCodeVersion2Data: {
                remoteRepoUrl:
                  'git@github.com:NotVertaAI/not-modeldb-client.git',
                commitHash: '0db2e34e00f34fca818d49f682fbcecfc20514a6',
              },
            },
            {
              gitCodeVersion1Data: {
                remoteRepoUrl: 'git@github.com:VertaAI/modeldb-client.git',
                commitHash: '0db2e34e00f34fca818d49f682fbcecfc20514a6',
              },
              gitCodeVersion2Data: {
                remoteRepoUrl: 'git@github.com:VertaAI/modeldb-client.git',
              },
            },
          ];
          for (const testCase of testCases) {
            const { component } = await renderComponent({
              experimentRun1: updateExperimentRun(
                {
                  codeVersion: {
                    type: 'git',
                    data: testCase.gitCodeVersion1Data,
                  },
                },
                experimentRun1
              ),
              experimentRun2: updateExperimentRun(
                {
                  codeVersion: {
                    type: 'git',
                    data: testCase.gitCodeVersion2Data,
                  },
                },
                experimentRun2
              ),
            });

            openCompareCodeVersionsPopup('git', 'first', component);

            expect(
              findByDataTestAttribute('compare-code-versions-button', component)
                .length
            ).toBe(0);
          }
        });

        it('should display "Compare Code" button which redirect on git compare page in the compare code versions popup if remote repo urls are equals and commit hashs are existed', async () => {
          const { component } = await renderComponent({
            experimentRun1: updateExperimentRun(
              {
                codeVersion: {
                  type: 'git',
                  data: {
                    remoteRepoUrl: 'git@github.com:VertaAI/modeldb-client.git',
                    commitHash: '0db2e34e00f34fca818d49f682fbcecfc20514a6',
                  },
                },
              },
              experimentRun1
            ),
            experimentRun2: updateExperimentRun(
              {
                codeVersion: {
                  type: 'git',
                  data: {
                    remoteRepoUrl: 'git@github.com:VertaAI/modeldb-client.git',
                    commitHash: 'afd13413fadff34fca818d49f682fbcecfc20514a6',
                  },
                },
              },
              experimentRun2
            ),
          });

          openCompareCodeVersionsPopup('git', 'first', component);

          expect(
            findByDataTestAttribute('compare-code-versions-button', component)
              .length
          ).not.toBe(0);
          expect(
            findByDataTestAttribute('compare-code-versions-button', component)
              .first()
              .text()
          ).toBe('Compare Code');

          const windowOpenSpy = jest.fn();
          window.open = windowOpenSpy;

          findByDataTestAttribute('compare-code-versions-button', component)
            .first()
            .simulate('click');

          const expectedUrl = `https://github.com/VertaAI/modeldb-client/compare/0db2e3..afd134`;
          expect(windowOpenSpy).toBeCalledWith(expectedUrl, '_blank');
        });
      });
    });
  });
});
