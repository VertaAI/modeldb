import { bind } from 'decko';
import React from 'react';

import Collapsable from 'core/shared/view/elements/Collapsable/Collapsable';
import ChartConfigDropdown, {
  IOption,
} from 'core/shared/view/elements/ChartConfigDropdown/ChartConfigDropdown';
import { Icon } from 'core/shared/view/elements/Icon/Icon';

import { IParallelChartSelection } from '../shared/types/chartConfiguration';
import { IGenericChartData, Category } from '../shared/types/chartDataTypes';
import ParallelCoordinates from './chart/ParallelCoordinateChart';
import styles from './ParallelChartManager.module.css';

interface ILocalProps {
  initialConfiguration?: IParallelChartSelection;
  metricKeysSet: Set<string>;
  data: IGenericChartData;
  updateParallelChartConfig(
    parallelChartSelection: IParallelChartSelection
  ): void;
}

interface ILocalState {
  panelConfig: IPanelObject;
  selectedPanelElements: string[];
  selectedCategory: Category;
}

interface IPanelObject {
  [key: string]: IPanelField;
}

interface IPanelField {
  key: string;
  selected: boolean;
  metricKey: boolean;
}

function getUniqueKeys(data: IGenericChartData) {
  return [
    ...new Set(
      Array.prototype.concat.apply(
        [],
        data.map((item: object) => Object.keys(item))
      )
    ),
  ].filter(key => key !== 'modelRecord');
}

export function getReducedData(
  data: IGenericChartData,
  initialSelectedPanelElements?: string[]
): IPanelObject {
  return getUniqueKeys(data).reduce(function(result, item, index, array) {
    result[item] = {
      key: item,
      selected: initialSelectedPanelElements
        ? initialSelectedPanelElements.includes(item)
        : getUniqueKeys(data)
            .slice(0, 6)
            .includes(item),
      metricKey: false,
    };
    return result;
  }, {});
}

function fetchAllSelectedKeys(panelObject: IPanelObject) {
  return Object.values(panelObject)
    .filter(obj => obj.selected)
    .map(obj => obj.key);
}

class ParallelChartManager extends React.Component<ILocalProps, ILocalState> {
  constructor(props: ILocalProps) {
    super(props);
    const panelFields = getReducedData(
      this.props.data,
      this.props.initialConfiguration
        ? this.props.initialConfiguration.selectedPanelElements
        : undefined
    );
    this.state = {
      panelConfig: panelFields,
      selectedPanelElements: fetchAllSelectedKeys(panelFields),
      selectedCategory: 'experimentId',
    };
  }

  public componentDidMount() {
    this.props.updateParallelChartConfig({
      selectedPanelElements: this.state.selectedPanelElements,
    });
  }

  public componentDidUpdate(prevProps: ILocalProps) {
    if (prevProps !== this.props) {
      this.setState({
        selectedPanelElements: fetchAllSelectedKeys(this.state.panelConfig),
      });
    }
  }

  public render() {
    const reducedArrayOfFeatures = this.state.panelConfig;
    if (this.props.metricKeysSet) {
      Object.values(this.state.panelConfig).map((obj: IPanelField) => {
        if (this.props.metricKeysSet.has(obj.key)) {
          obj.metricKey = true;
        }
        return '';
      });
    }
    return (
      <div className={styles.chart_section_wrapper} data-test="parallel-chart">
        <div className={styles.chart_header_container}>
          <div className={styles.chart_header}>
            Parallel Coordinates of Hyperparameters and Metrics
          </div>
        </div>
        <div>
          <ChartConfigDropdown
            label="Category :"
            value={this.state.selectedCategory}
            options={Object.values(Category)}
            onChange={this.handleCategoryChange}
          />
          <div className={styles.parallelControlPanel}>
            <Collapsable
              collapseLabel={' Control Panel'}
              collapsibleContainerID={'pcChartCollapsible'}
              keepOpen={true}
              children={
                <div>
                  <div className={styles.metric_key_block}>
                    Select Hyperparameters:
                    <div className={styles.panel_content}>
                      {Object.keys(reducedArrayOfFeatures).filter(
                        item => !reducedArrayOfFeatures[item].metricKey
                      ).length > 0 ? (
                        Object.keys(reducedArrayOfFeatures)
                          .filter(
                            item => !reducedArrayOfFeatures[item].metricKey
                          )
                          .map((item, i) => {
                            return (
                              <button
                                key={i}
                                className={
                                  reducedArrayOfFeatures[item].selected
                                    ? styles.panel_buttons
                                    : styles.panel_button_deselected
                                }
                                onClick={this.handlePanelButtonClick(item)}
                              >
                                <span>{item}</span>
                              </button>
                            );
                          })
                      ) : (
                        <span className={styles.empty_hyp_msg}>
                          no hyperparameters keys present
                        </span>
                      )}
                    </div>
                  </div>
                  <div className={styles.metric_key_block}>
                    Select Metrics:
                    <div className={styles.panel_content}>
                      {Object.keys(reducedArrayOfFeatures).filter(
                        item => reducedArrayOfFeatures[item].metricKey
                      ).length > 0 ? (
                        Object.keys(reducedArrayOfFeatures)
                          .filter(
                            item => reducedArrayOfFeatures[item].metricKey
                          )
                          .map((item, i) => {
                            return (
                              <button
                                key={i}
                                className={
                                  reducedArrayOfFeatures[item].selected
                                    ? styles.panel_buttons
                                    : styles.panel_button_deselected
                                }
                                onClick={this.handlePanelButtonClick(item)}
                              >
                                <span>{item}</span>
                              </button>
                            );
                          })
                      ) : (
                        <span className={styles.empty_hyp_msg}>
                          no hyperparameters keys present
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              }
            />
            <div>
              {this.state.selectedPanelElements.length >= 7 && (
                <div className={styles.parallelCountMeta}>
                  <div className={styles.pcSuggestionLogo}>
                    <Icon
                      className={styles.desc_action_icon}
                      type={'exclamation-triangle-lite'}
                    />
                  </div>
                  max no. of fields selected, consider deselecting some for
                  better visibility
                </div>
              )}
            </div>
          </div>
          <div>
            <ParallelCoordinates
              data={this.props.data}
              metricKeysSet={this.props.metricKeysSet}
              selectedPanelElements={this.state.selectedPanelElements}
              selectedCategory={this.state.selectedCategory}
            />
          </div>
        </div>
      </div>
    );
  }

  @bind
  public handleCategoryChange(option: IOption) {
    const selectedCategory = option.value as Category;
    this.setState({ selectedCategory });
  }

  public handlePanelButtonClick = (item: string) => (
    event: React.MouseEvent<HTMLButtonElement, MouseEvent>
  ) => {
    const reducedArrayOfFeatures = this.state.panelConfig;
    const metricKeysSet = this.props.metricKeysSet;
    const updatedPanelConfig = {
      ...reducedArrayOfFeatures,
      [item]: {
        key: item,
        selected: !reducedArrayOfFeatures[item].selected,
        metricKey: metricKeysSet.has(item),
      },
    };
    this.setState({
      panelConfig: updatedPanelConfig,
      selectedPanelElements: fetchAllSelectedKeys(updatedPanelConfig),
    });

    this.props.updateParallelChartConfig({
      selectedPanelElements: fetchAllSelectedKeys(updatedPanelConfig),
    });
    return;
  };
}

export type IParallelChartProps = ILocalProps;
export { ParallelChartManager };
export default ParallelChartManager;
