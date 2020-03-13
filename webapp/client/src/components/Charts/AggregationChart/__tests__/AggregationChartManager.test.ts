import {
  makeShallowRenderer,
  findByDataTestAttribute,
} from 'core/shared/utils/tests/react/helpers';
import {
  flatData_allMetricHyp,
  flatData_someMetricMissing,
  flatData_someHypMissing,
  flatData_onlyHyperparams,
  flatData_onlyMetric,
} from '../../__tests__/mocks';
import {
  IAggregationChartProps,
  AggregationChartManager,
} from '../AggregationChartManager';
import {
  mockAggChartConfig,
  mockNoMetricAggChartConfig,
  mockNoHypAggChartConfig,
  mockHyperparamKeys,
  mockNoHyperparamKeys,
  mockMetricKeys,
  mockNoMetricKeys,
} from './mocks';

const makeShallowComponent = (props: Partial<IAggregationChartProps> = {}) => {
  const defaultProps: IAggregationChartProps = {
    aggregationChartConfig: {
      selectedMetric: '',
      selectedHyperparameter: '',
      selectedAggregationType: '',
      selectedChartType: '',
    },
    metricKeys: new Set(['']),
    hyperparameterKeys: new Set(['']),
    genericChartData: [],
    updateAggregationChartConfig: jest.fn(),
  };
  return {
    component: makeShallowRenderer<IAggregationChartProps>(
      AggregationChartManager,
      defaultProps
    )(props),
  };
};

describe('(aggregation chart)', () => {
  describe('(data: all rows with metric and hyp)', () => {
    const props = {
      aggregationChartConfig: mockAggChartConfig,
      metricKeys: mockMetricKeys,
      hyperparameterKeys: mockHyperparamKeys,
      genericChartData: flatData_allMetricHyp,
    };
    const { component } = makeShallowComponent(props);

    it('should load  a valid aggregationChart component', () => {
      const wrapper = findByDataTestAttribute('aggregation-chart', component);
      expect(wrapper.length).toBe(1);
    });

    it('aggregationChart config loaded and its value as expected', () => {
      const classInstance = component.instance() as AggregationChartManager;
      const selectedMetric = classInstance.state.userSelectedMetricVal;
      expect(selectedMetric).toBe('val_acc');

      const selectedHyperparam =
        classInstance.state.userSelectedHyperparameterVal;
      expect(selectedHyperparam).toBe('C');

      const selectedAggregationType =
        classInstance.state.userSelectedAggregationType;
      expect(selectedAggregationType).toBe('average');

      const selectedChartType = classInstance.state.userSelectedChartType;
      expect(selectedChartType).toBe('bar-chart');
    });

    it('should render aggregationChart component that matches snapshot', () => {
      expect(
        findByDataTestAttribute('aggregation-chart', component)
      ).toMatchSnapshot();
    });
  });

  describe('(data: some metric missing)', () => {
    const props = {
      aggregationChartConfig: mockAggChartConfig,
      metricKeys: mockMetricKeys,
      hyperparameterKeys: mockHyperparamKeys,
      genericChartData: flatData_someMetricMissing,
    };
    const { component } = makeShallowComponent(props);

    it('should load  a valid aggregationChart component', () => {
      const wrapper = findByDataTestAttribute('aggregation-chart', component);
      expect(wrapper.length).toBe(1);
    });

    it('aggregationChart config loaded and its value as expected', () => {
      const classInstance = component.instance() as AggregationChartManager;
      const selectedMetric = classInstance.state.userSelectedMetricVal;
      expect(selectedMetric).toBe('val_acc');

      const selectedHyperparam =
        classInstance.state.userSelectedHyperparameterVal;
      expect(selectedHyperparam).toBe('C');

      const selectedAggregationType =
        classInstance.state.userSelectedAggregationType;
      expect(selectedAggregationType).toBe('average');

      const selectedChartType = classInstance.state.userSelectedChartType;
      expect(selectedChartType).toBe('bar-chart');
    });

    it('should render aggregationChart component that matches snapshot', () => {
      expect(
        findByDataTestAttribute('aggregation-chart', component)
      ).toMatchSnapshot();
    });
  });

  describe('(data: some hyp missing)', () => {
    const props = {
      aggregationChartConfig: mockAggChartConfig,
      metricKeys: mockMetricKeys,
      hyperparameterKeys: mockHyperparamKeys,
      genericChartData: flatData_someHypMissing,
    };
    const { component } = makeShallowComponent(props);

    it('should load  a valid aggregationChart component', () => {
      const wrapper = findByDataTestAttribute('aggregation-chart', component);
      expect(wrapper.length).toBe(1);
    });

    it('aggregationChart config loaded and its value as expected', () => {
      const classInstance = component.instance() as AggregationChartManager;
      const selectedMetric = classInstance.state.userSelectedMetricVal;
      expect(selectedMetric).toBe('val_acc');

      const selectedHyperparam =
        classInstance.state.userSelectedHyperparameterVal;
      expect(selectedHyperparam).toBe('C');

      const selectedAggregationType =
        classInstance.state.userSelectedAggregationType;
      expect(selectedAggregationType).toBe('average');

      const selectedChartType = classInstance.state.userSelectedChartType;
      expect(selectedChartType).toBe('bar-chart');
    });

    it('should render aggregationChart component that matches snapshot', () => {
      expect(
        findByDataTestAttribute('aggregation-chart', component)
      ).toMatchSnapshot();
    });
  });

  describe('(data: only hyperparams)', () => {
    const props = {
      aggregationChartConfig: mockNoMetricAggChartConfig,
      metricKeys: mockNoMetricKeys,
      hyperparameterKeys: mockHyperparamKeys,
      genericChartData: flatData_onlyHyperparams,
    };
    const { component } = makeShallowComponent(props);
    it('should load  a valid aggregationChart component', () => {
      const wrapper = findByDataTestAttribute('aggregation-chart', component);
      expect(wrapper.length).toBe(1);
    });

    it('aggregationChart config loaded and its value as expected', () => {
      const classInstance = component.instance() as AggregationChartManager;
      const selectedMetric = classInstance.state.userSelectedMetricVal;
      expect(selectedMetric).toBe('data not available');

      const selectedHyperparam =
        classInstance.state.userSelectedHyperparameterVal;
      expect(selectedHyperparam).toBe('C');

      const selectedAggregationType =
        classInstance.state.userSelectedAggregationType;
      expect(selectedAggregationType).toBe('average');

      const selectedChartType = classInstance.state.userSelectedChartType;
      expect(selectedChartType).toBe('bar-chart');
    });

    it('should render aggregationChart component that matches snapshot', () => {
      expect(
        findByDataTestAttribute('aggregation-chart', component)
      ).toMatchSnapshot();
    });
  });

  describe('(data: only hyperparams)', () => {
    const props = {
      aggregationChartConfig: mockNoHypAggChartConfig,
      metricKeys: mockMetricKeys,
      hyperparameterKeys: mockNoHyperparamKeys,
      genericChartData: flatData_onlyMetric,
    };
    const { component } = makeShallowComponent(props);

    it('should load  a valid aggregationChart component', () => {
      const wrapper = findByDataTestAttribute('aggregation-chart', component);
      expect(wrapper.length).toBe(1);
    });

    it('aggregationChart config loaded and its value as expected', () => {
      const classInstance = component.instance() as AggregationChartManager;
      const selectedMetric = classInstance.state.userSelectedMetricVal;
      expect(selectedMetric).toBe('val_acc');

      const selectedHyperparam =
        classInstance.state.userSelectedHyperparameterVal;
      expect(selectedHyperparam).toBe('data not available');

      const selectedAggregationType =
        classInstance.state.userSelectedAggregationType;
      expect(selectedAggregationType).toBe('average');

      const selectedChartType = classInstance.state.userSelectedChartType;
      expect(selectedChartType).toBe('bar-chart');
    });

    it('should render aggregationChart component that matches snapshot', () => {
      expect(
        findByDataTestAttribute('aggregation-chart', component)
      ).toMatchSnapshot();
    });
  });
});
