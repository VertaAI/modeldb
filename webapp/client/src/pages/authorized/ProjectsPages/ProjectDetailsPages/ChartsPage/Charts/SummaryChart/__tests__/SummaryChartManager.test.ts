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
  ISummaryChartProps,
  SummaryChartManager,
} from '../SummaryChartManager';
import {
  mockSummaryChartConfig,
  mockNoDataConfig,
  mockMetricKeys,
  mockNoMetricKeys,
} from './mocks';

const makeShallowComponent = (props: Partial<ISummaryChartProps> = {}) => {
  const defaultProps: ISummaryChartProps = {
    summaryChartConfig: {},
    metricKeys: new Set(),
    genericChartData: {},
    updateSummaryChartConfig: jest.fn(),
  };
  return {
    component: makeShallowRenderer<ISummaryChartProps>(
      SummaryChartManager,
      defaultProps
    )(props),
  };
};

describe('(symmary chart)', () => {
  describe('(data: all_metric_hyp)', () => {
    const props = {
      summaryChartConfig: mockSummaryChartConfig,
      metricKeys: mockMetricKeys,
      genericChartData: flatData_allMetricHyp,
    };
    const { component } = makeShallowComponent(props);
    it('should load a valid symmaryChart component', () => {
      const wrapper = findByDataTestAttribute('summary-chart', component);
      expect(wrapper.length).toBe(1);
    });

    it('symmaryChart config loaded and its value as expected', () => {
      const classInstance = component.instance() as SummaryChartManager;
      const selectedMetric = classInstance.state.userSelectedMetricVal;
      expect(selectedMetric).toBe('val_acc');
    });

    it('should render summaryChart component that matches snapshot', () => {
      expect(
        findByDataTestAttribute('summary-chart', component)
      ).toMatchSnapshot();
    });
  });

  describe('(data: some metric missing)', () => {
    const props = {
      summaryChartConfig: mockSummaryChartConfig,
      metricKeys: mockMetricKeys,
      genericChartData: flatData_someMetricMissing,
    };
    const { component } = makeShallowComponent(props);

    it('should load a valid symmaryChart component', () => {
      const wrapper = findByDataTestAttribute('summary-chart', component);
      expect(wrapper.length).toBe(1);
    });

    it('symmaryChart config loaded and its value as expected', () => {
      const classInstance = component.instance() as SummaryChartManager;
      const selectedMetric = classInstance.state.userSelectedMetricVal;
      expect(selectedMetric).toBe('val_acc');
    });

    it('should render summaryChart component that matches snapshot', () => {
      expect(
        findByDataTestAttribute('summary-chart', component)
      ).toMatchSnapshot();
    });
  });

  describe('(data: some hyp missing)', () => {
    const props = {
      summaryChartConfig: mockSummaryChartConfig,
      metricKeys: mockMetricKeys,
      genericChartData: flatData_someHypMissing,
    };
    const { component } = makeShallowComponent(props);

    it('should load a valid symmaryChart component', () => {
      const wrapper = findByDataTestAttribute('summary-chart', component);
      expect(wrapper.length).toBe(1);
    });

    it('symmaryChart config loaded and its value as expected', () => {
      const classInstance = component.instance() as SummaryChartManager;
      const selectedMetric = classInstance.state.userSelectedMetricVal;
      expect(selectedMetric).toBe('val_acc');
    });

    it('should render summaryChart component that matches snapshot', () => {
      expect(
        findByDataTestAttribute('summary-chart', component)
      ).toMatchSnapshot();
    });
  });

  describe('(data: only hyp)', () => {
    const props = {
      summaryChartConfig: mockNoDataConfig,
      metricKeys: mockNoMetricKeys,
      genericChartData: flatData_onlyHyperparams,
    };
    const { component } = makeShallowComponent(props);

    it('should load a valid symmaryChart component', () => {
      const wrapper = findByDataTestAttribute('summary-chart', component);
      expect(wrapper.length).toBe(1);
    });

    it('symmaryChart config loaded and its value as expected', () => {
      const classInstance = component.instance() as SummaryChartManager;
      const selectedMetric = classInstance.state.userSelectedMetricVal;
      expect(selectedMetric).toBe('data not available');
    });

    it('should render summaryChart component that matches snapshot', () => {
      expect(
        findByDataTestAttribute('summary-chart', component)
      ).toMatchSnapshot();
    });
  });

  describe('(data: only metric)', () => {
    const props = {
      summaryChartConfig: mockSummaryChartConfig,
      metricKeys: mockMetricKeys,
      genericChartData: flatData_onlyMetric,
    };
    const { component } = makeShallowComponent(props);

    it('should load a valid symmaryChart component', () => {
      const wrapper = findByDataTestAttribute('summary-chart', component);
      expect(wrapper.length).toBe(1);
    });

    it('symmaryChart config loaded and its value as expected', () => {
      const classInstance = component.instance() as SummaryChartManager;
      const selectedMetric = classInstance.state.userSelectedMetricVal;
      expect(selectedMetric).toBe('val_acc');
    });

    it('should render summaryChart component that matches snapshot', () => {
      expect(
        findByDataTestAttribute('summary-chart', component)
      ).toMatchSnapshot();
    });
  });
});
