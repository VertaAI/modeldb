import { ShallowWrapper } from 'enzyme';

import {
  makeShallowRenderer,
  findByDataTestAttribute,
} from 'core/shared/utils/tests/react/helpers';
import { IChartProps, Charts } from '../Charts';
import {
  projectId,
  mockExperimentRuns,
  chartConfig_default,

  // some metric missing,
  mockExperimentRuns_someMetricMissing,
  // only hyperparam
  mockExperimentRuns_onlyHyperparam,
  // only metric
  mockExperimentRuns_onlyMetric,
  // some hyperparam missing
  mockExperimentRuns_someHypMissing,
} from './mocks';

import {
  computedModelRecords,
  computedMetricHypFields,
  computedMetricHypNonNumericFields,
} from './mocks/allRowsPresent';

import {
  computedModelRecords_someDataMissing,
  computedMetricHypFields_someDataMissing,
  computedNonNumericFields_someDataMissing,
} from './mocks/someDataMissing';

import {
  chartConfig_someHypMissing,
  computedModelRecords_someHypMissing,
  computedMetricHypFields_someHypMissing,
  computedNonNumericFields_someHypMissing,
} from './mocks/someHypMissing';

import { initialCommunication } from 'core/shared/utils/redux/communication';
import { makeRouterMockProps } from 'core/shared/utils/tests/react/routeComponentProps';
import routes from 'routes';
import { userWorkspacesWithCurrentUser } from 'utils/tests/mocks/models/workspace';
import {
  chartConfig_onlyMetric,
  computedModelRecords_onlyMetrics,
  computedMetricHypFields_onlyMetrics,
  computedNonNumericFields_onlyMetric,
} from './mocks/onlyMetricsPresent';

import {
  chartConfig_onlyHyp,
  computedModelRecords_onlyHyp,
  computedMetricHypFields_onlyHyp,
  computedNonNumericFields_onlyHyp,
} from './mocks/onlyHypPresent';

const makeShallowComponent = (props: Partial<IChartProps> = {}) => {
  const routerMockProps = makeRouterMockProps(
    { projectId },
    {
      history: {
        location: {
          pathname: routes.charts.getRedirectPath({
            projectId,
            workspaceName: userWorkspacesWithCurrentUser.user.name,
          }),
        },
      },
    }
  );
  const defaultProps: IChartProps = {
    ...routerMockProps,
    sequentialChartData: [],
    loadingSequentialChartData: initialCommunication,
    lazyChartData: [],
    loadingLazyChartData: initialCommunication,
    filters: [],
    projectId,
    onResetConfigurations: jest.fn(),
    resetExperimentRunsSettings: jest.fn(),
  };
  return {
    component: makeShallowRenderer<IChartProps>(Charts, defaultProps)(props),
  };
};

const findRecord = (
  recordClass: string,
  component: ShallowWrapper<any, any, any>
) => {
  return component.findWhere(
    n => n.props().additionalContainerClassName === recordClass
  );
};

describe.skip('(charts root component)', () => {
  describe('(data: all rows with metric and hyp)', () => {
    const { component } = makeShallowComponent({
      sequentialChartData: mockExperimentRuns,
    });
    const classInstance = component.instance() as Charts;
    const state = classInstance.state;

    it('should load charts with selected default chart configuration', () => {
      const wrapper = findByDataTestAttribute('charts-root', component);
      expect(wrapper.length).toBe(1);
      expect(
        findByDataTestAttribute('charts-root', component)
      ).toMatchSnapshot();
    });

    it('should load charts with selected chart configuration', () => {
      expect(state.initialChartConfig).toMatchObject(chartConfig_default);
    });

    it('should load charts with selected chart configuration', () => {
      expect(state.chartConfigurations).toMatchObject(chartConfig_default);
    });

    it('assert chartModelRecords to be accurately computed', () => {
      expect(state.chartModelRecords).toMatchObject(computedModelRecords);
    });

    it('should load charts with selected default chart configuration', () => {
      expect(state.chartMetricHypFields).toMatchObject(computedMetricHypFields);
    });

    it('should load charts with selected default chart configuration', () => {
      expect(state.chartMetricHypNonNumericFields).toMatchObject(
        computedMetricHypNonNumericFields
      );
    });
  });

  describe('(data: some metric missing)', () => {
    const { component } = makeShallowComponent({
      sequentialChartData: mockExperimentRuns_someMetricMissing,
    });
    const classInstance = component.instance() as Charts;
    const state = classInstance.state;

    it('should load charts with selected default chart configuration', () => {
      const wrapper = findByDataTestAttribute('charts-root', component);
      expect(wrapper.length).toBe(1);
      expect(
        findByDataTestAttribute('charts-root', component)
      ).toMatchSnapshot();
    });

    it('should load charts with selected chart configuration', () => {
      expect(state.initialChartConfig).toMatchObject(chartConfig_default);
    });

    it('should load charts with selected chart configuration', () => {
      expect(state.chartConfigurations).toMatchObject(chartConfig_default);
    });

    it('assert chartModelRecords to be accurately computed', () => {
      expect(state.chartModelRecords).toMatchObject(
        computedModelRecords_someDataMissing
      );
    });

    it('should load charts with selected default chart configuration', () => {
      expect(state.chartMetricHypFields).toMatchObject(
        computedMetricHypFields_someDataMissing
      );
    });

    it('should load charts with selected default chart configuration', () => {
      expect(state.chartMetricHypNonNumericFields).toMatchObject(
        computedNonNumericFields_someDataMissing
      );
    });
  });

  describe('(data: some hyp missing)', () => {
    const { component } = makeShallowComponent({
      sequentialChartData: mockExperimentRuns_someHypMissing,
    });
    const classInstance = component.instance() as Charts;
    const state = classInstance.state;

    it('should load charts with selected default chart configuration', () => {
      const wrapper = findByDataTestAttribute('charts-root', component);
      expect(wrapper.length).toBe(1);
      expect(
        findByDataTestAttribute('charts-root', component)
      ).toMatchSnapshot();
    });

    it('should load charts with selected initial configuration', () => {
      expect(state.initialChartConfig).toMatchObject(chartConfig_default);
    });

    it('should load charts with selected chart configuration', () => {
      expect(state.chartConfigurations).toMatchObject(
        chartConfig_someHypMissing
      );
    });

    it('assert chartModelRecords to be accurately computed', () => {
      expect(state.chartModelRecords).toMatchObject(
        computedModelRecords_someHypMissing
      );
    });

    it('should load charts with selected default chart configuration', () => {
      expect(state.chartMetricHypFields).toMatchObject(
        computedMetricHypFields_someHypMissing
      );
    });

    it('should load charts with selected default chart configuration', () => {
      expect(state.chartMetricHypNonNumericFields).toMatchObject(
        computedNonNumericFields_someHypMissing
      );
    });
  });

  describe('(data: only metric)', () => {
    const { component } = makeShallowComponent({
      sequentialChartData: mockExperimentRuns_onlyMetric,
    });
    const classInstance = component.instance() as Charts;
    const state = classInstance.state;

    it('should load charts with selected default chart configuration', () => {
      const wrapper = findByDataTestAttribute('charts-root', component);
      expect(wrapper.length).toBe(1);
      expect(
        findByDataTestAttribute('charts-root', component)
      ).toMatchSnapshot();
    });

    it('should load charts with selected initial configuration', () => {
      expect(state.initialChartConfig).toMatchObject(chartConfig_onlyMetric);
    });

    it('should load charts with selected chart configuration', () => {
      expect(state.chartConfigurations).toMatchObject(chartConfig_onlyMetric);
    });

    it('assert chartModelRecords to be accurately computed', () => {
      expect(state.chartModelRecords).toMatchObject(
        computedModelRecords_onlyMetrics
      );
    });

    it('should load charts with selected default chart configuration', () => {
      expect(state.chartMetricHypFields).toMatchObject(
        computedMetricHypFields_onlyMetrics
      );
    });

    it('should load charts with selected default chart configuration', () => {
      expect(state.chartMetricHypNonNumericFields).toMatchObject(
        computedNonNumericFields_onlyMetric
      );
    });
  });

  describe('(data: only hyperparam)', () => {
    const { component } = makeShallowComponent({
      sequentialChartData: mockExperimentRuns_onlyHyperparam,
    });
    const classInstance = component.instance() as Charts;
    const state = classInstance.state;

    it('should load charts with selected default chart configuration', () => {
      const wrapper = findByDataTestAttribute('charts-root', component);
      expect(wrapper.length).toBe(1);
      expect(
        findByDataTestAttribute('charts-root', component)
      ).toMatchSnapshot();
    });

    it('should load charts with selected initial configuration', () => {
      expect(state.initialChartConfig).toMatchObject(chartConfig_onlyHyp);
    });

    it('should load charts with selected chart configuration', () => {
      expect(state.chartConfigurations).toMatchObject(chartConfig_onlyHyp);
    });

    it('assert chartModelRecords to be accurately computed', () => {
      expect(state.chartModelRecords).toMatchObject(
        computedModelRecords_onlyHyp
      );
    });

    it('should load charts with selected default chart configuration', () => {
      expect(state.chartMetricHypFields).toMatchObject(
        computedMetricHypFields_onlyHyp
      );
    });

    it('should load charts with selected default chart configuration', () => {
      expect(state.chartMetricHypNonNumericFields).toMatchObject(
        computedNonNumericFields_onlyHyp
      );
    });
  });
});
