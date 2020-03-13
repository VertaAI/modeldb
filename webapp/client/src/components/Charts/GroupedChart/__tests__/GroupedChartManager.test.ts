import {
  makeShallowRenderer,
  findByDataTestAttribute,
} from 'core/shared/utils/tests/react/helpers';
import {
  groupedChartData_allMetricHyp,
  groupedChartData_someMetricMissing,
  groupedChartData_someHypMissing,
  groupedChartData_onlyMetric,
  groupedChartData_onlyHyperparams,
} from '../../__tests__/mocks';
import {
  IGroupedChartProps,
  GroupedChartManager,
} from '../GroupedChartManager';
import { mockGroupedChartConfig } from './mocks';

const makeShallowComponent = (props: Partial<IGroupedChartProps> = {}) => {
  const defaultProps: IGroupedChartProps = {
    initialConfiguration: {},
    metricKeysSet: new Set(),
    hyperparamKeysSet: new Set(),
    data: {},
    updateGroupedChartConfig: jest.fn(),
  };
  return {
    component: makeShallowRenderer<IGroupedChartProps>(
      GroupedChartManager,
      defaultProps
    )(props),
  };
};

describe('(grouped chart)', () => {
  describe('(data: all metric hyp)', () => {
    const props = {
      initialConfiguration: mockGroupedChartConfig,
      data: groupedChartData_allMetricHyp,
    };
    const { component } = makeShallowComponent(props);

    it('should load  a valid groupedChart component', () => {
      const wrapper = findByDataTestAttribute('grouped-chart', component);
      expect(wrapper.length).toBe(1);
    });

    it('should render groupedChart component that matches snapshot', () => {
      expect(
        findByDataTestAttribute('grouped-chart', component)
      ).toMatchSnapshot();
    });
  });

  describe('(data: some metric missing)', () => {
    const props = {
      initialConfiguration: mockGroupedChartConfig,
      data: groupedChartData_someMetricMissing,
    };
    const { component } = makeShallowComponent(props);

    it('should load  a valid groupedChart component', () => {
      const wrapper = findByDataTestAttribute('grouped-chart', component);
      expect(wrapper.length).toBe(1);
    });

    it('should render groupedChart component that matches snapshot', () => {
      expect(
        findByDataTestAttribute('grouped-chart', component)
      ).toMatchSnapshot();
    });
  });

  describe('(data: some hyp missing)', () => {
    const props = {
      initialConfiguration: mockGroupedChartConfig,
      data: groupedChartData_someHypMissing,
    };
    const { component } = makeShallowComponent(props);

    it('should load  a valid groupedChart component', () => {
      const wrapper = findByDataTestAttribute('grouped-chart', component);
      expect(wrapper.length).toBe(1);
    });

    it('should render groupedChart component that matches snapshot', () => {
      expect(
        findByDataTestAttribute('grouped-chart', component)
      ).toMatchSnapshot();
    });
  });
});
