import {
  makeShallowRenderer,
  findByDataTestAttribute,
} from 'core/shared/utils/tests/react/helpers';
import {
  parallelChartData_allMetricHyp,
  parallelChartData_someMetricMissing,
  parallelChartData_someHypMissing,
  parallelChartData_onlyMetric,
} from '../../__tests__/mocks';
import {
  IParallelChartProps,
  ParallelChartManager,
} from '../ParallelChartManager';
import {
  mockParallelChartSelection,
  mockOnlyMetricParallelChartSelection,
  mockMetricKeysSet,
} from './mocks';

const makeShallowComponent = (props: Partial<IParallelChartProps> = {}) => {
  const defaultProps: IParallelChartProps = {
    initialConfiguration: { selectedPanelElements: [] },
    metricKeysSet: new Set(),
    data: {},
    updateParallelChartConfig: jest.fn(),
  };
  return {
    component: makeShallowRenderer<IParallelChartProps>(
      ParallelChartManager,
      defaultProps
    )(props),
  };
};

describe('(parallel chart)', () => {
  describe('(data: all metric hyp)', () => {
    const props = {
      initialConfiguration: mockParallelChartSelection,
      metricKeysSet: mockMetricKeysSet,
      data: parallelChartData_allMetricHyp,
    };
    const { component } = makeShallowComponent(props);

    it('should load  a valid parallelChart component', () => {
      const wrapper = findByDataTestAttribute('parallel-chart', component);
      expect(wrapper.length).toBe(1);
    });

    it('should render parallelChart component that matches snapshot', () => {
      expect(
        findByDataTestAttribute('parallel-chart', component)
      ).toMatchSnapshot();
    });
  });

  describe('(data: some metric missing)', () => {
    const props = {
      initialConfiguration: mockParallelChartSelection,
      metricKeysSet: mockMetricKeysSet,
      data: parallelChartData_someMetricMissing,
    };
    const { component } = makeShallowComponent(props);

    it('should load  a valid parallelChart component', () => {
      const wrapper = findByDataTestAttribute('parallel-chart', component);
      expect(wrapper.length).toBe(1);
    });

    it('should render parallelChart component that matches snapshot', () => {
      expect(
        findByDataTestAttribute('parallel-chart', component)
      ).toMatchSnapshot();
    });
  });

  describe('(data: some hyp missing)', () => {
    const props = {
      initialConfiguration: mockParallelChartSelection,
      metricKeysSet: mockMetricKeysSet,
      data: parallelChartData_someHypMissing,
    };
    const { component } = makeShallowComponent(props);

    it('should load  a valid parallelChart component', () => {
      const wrapper = findByDataTestAttribute('parallel-chart', component);
      expect(wrapper.length).toBe(1);
    });

    it('should render parallelChart component that matches snapshot', () => {
      expect(
        findByDataTestAttribute('parallel-chart', component)
      ).toMatchSnapshot();
    });
  });

  describe('(data: only metric)', () => {
    const props = {
      initialConfiguration: mockOnlyMetricParallelChartSelection,
      metricKeysSet: mockMetricKeysSet,
      data: parallelChartData_onlyMetric,
    };
    const { component } = makeShallowComponent(props);
    it('should load  a valid parallelChart component', () => {
      const wrapper = findByDataTestAttribute('parallel-chart', component);
      expect(wrapper.length).toBe(1);
    });

    it('should render parallelChart component that matches snapshot', () => {
      expect(
        findByDataTestAttribute('parallel-chart', component)
      ).toMatchSnapshot();
    });
  });
});
