import _ from 'lodash';
import React from 'react';
import ModelRecord from '../../../models/ModelRecord';
import BarChart from './BarChart';

import styles from './ModelExploration.module.css';

interface ILocalProps {
  expRuns: ModelRecord[];
}

interface ILocalState {
  aggType: string[];
  flatMetric: object[];
  computeXAxisFields: object[];
  selectedXAxis: string;
  selectedYAxis: string;
  selectedAggregate: string;
}

// requirements:
//      we need an aggregate switching mechanism
//      groupby involve a new chart type

export default class ModelExploration extends React.Component<ILocalProps, ILocalState> {
  public xAxisParams: Set<string> = new Set(); // computed fields from ModelRecord object
  public yAxisParams: Set<string> = new Set(); // metric fields only for Y axis
  public constructor(props: ILocalProps) {
    super(props);
    this.state = {
      aggType: ['average', 'sum', 'median', 'variance', 'stdev', 'count'],
      computeXAxisFields: this.computeXAxisFields(props.expRuns),
      flatMetric: this.computeFlatMetric(props.expRuns),
      selectedAggregate: 'average',
      selectedXAxis: 'n_valid', // initial val for testing
      selectedYAxis: 'val_acc' // initial val for testing
    };
  }

  public render() {
    const { expRuns } = this.props;
    // let constVal = 'averageReduceMetrics';
    // console.log(this.state.computeXAxisFields);
    // this.setState({
    //   computedData: this.reduceMetricsAvg(this.groupBy(this.state.computeXAxisFields, (field: any) => field[this.state.selectedXAxis]))
    // });
    // console.log(this.state);
    // this.state.computedData = this.reduceMetricsAvg(
    //   this.groupBy(this.state.computeXAxisFields, (field: any) => field[this.state.selectedXAxis])
    // );

    return expRuns ? (
      <div className={styles.summary_wrapper}>
        <h3>Explore Visualizations</h3>
        <p>
          Generate charts to visualize trends in data by selecting fields to plot as x and y values. Optionally pick fields to group by and
          specify what type of aggregation to use.
        </p>

        <div style={{ display: 'flex' }}>
          <div className={styles.chart_selector}>
            Y Axis: {'  '}
            <select name="selected-yaxis" value={this.state.selectedYAxis} onChange={this.setLocalYState} className={styles.dropdown}>
              {Array.from(this.yAxisParams).map((param: string, i: number) => {
                return (
                  <option key={i} value={param}>
                    {param}
                  </option>
                );
              })}
            </select>
          </div>

          <div className={styles.chart_selector}>
            X Axis: {'  '}
            <select name="selected-xaxis" value={this.state.selectedXAxis} onChange={this.setLocalXState} className={styles.dropdown}>
              {Array.from(this.xAxisParams).map((param: string, i: number) => {
                return (
                  <option key={i} value={param}>
                    {param}
                  </option>
                );
              })}
            </select>
          </div>

          <div className={styles.chart_selector}>
            Aggregate: {'  '}
            <select
              name="selected-aggregate"
              value={this.state.selectedAggregate}
              onChange={this.setLocalAggState}
              className={styles.dropdown}
            >
              {this.state.aggType.map((param: string, i: number) => {
                return (
                  <option key={i} value={param}>
                    {param}
                  </option>
                );
              })}
            </select>
          </div>
          {/* <div className={styles.compute_button}>
            <button>Compute Charts</button>
          </div> */}
        </div>
        <div>
          {console.log(this.state)}

          <BarChart
            xLabel={this.state.selectedXAxis}
            yLabel={this.state.selectedYAxis}
            data={this.returnAggResults(
              this.state.selectedAggregate,
              this.groupBy(this.state.computeXAxisFields, (field: any) => field[this.state.selectedXAxis])
            )}
          />
        </div>
      </div>
    ) : (
      ''
    );
  }

  public setLocalYState = (event: React.FormEvent<HTMLSelectElement>) => {
    const element = event.target as HTMLSelectElement;
    this.setState({ selectedYAxis: element.value });
  };

  public setLocalXState = (event: React.FormEvent<HTMLSelectElement>) => {
    const element = event.target as HTMLSelectElement;
    this.setState({ selectedXAxis: element.value });
  };

  public setLocalAggState = (event: React.FormEvent<HTMLSelectElement>) => {
    const element = event.target as HTMLSelectElement;
    this.setState({ selectedAggregate: element.value });
  };

  // Utilities
  public groupBy(list: object[], keyGetter: any) {
    const map = new Map();
    list.forEach((item: any) => {
      const key = keyGetter(item);
      const collection = map.get(key);
      if (!collection) {
        map.set(key, [item[this.state.selectedYAxis]]);
      } else {
        collection.push(item[this.state.selectedYAxis]);
      }
    });
    return map;
  }

  public average = (array: any) => array.reduce((a: number, b: number) => a + b) / array.length;
  public sum = (array: any) => array.reduce((a: number, b: number) => a + b);
  public median = (array: any) => {
    array.sort((a: number, b: number) => a - b);
    const lowMiddle = Math.floor((array.length - 1) / 2);
    const highMiddle = Math.ceil((array.length - 1) / 2);
    console.log((array[lowMiddle] + array[highMiddle]) / 2);
    return (array[lowMiddle] + array[highMiddle]) / 2;
  };
  public variance = (array: any) => array.reduce((a: any, b: any) => a * b);
  public stdev = (array: any) => array.reduce((a: any, b: any) => a * b);
  public count = (array: any) => array.reduce((a: any, b: any) => a * b);

  // to be used to refactor the aggregate workflow
  // public reduceByAggType = (aggType: string, array: any[]) => {
  //   return
  // }

  public returnAggResults = (selected: string, arrayGpBy: any) => {
    switch (selected) {
      case 'average':
        return this.averageReduceMetrics(arrayGpBy);
      case 'sum':
        return this.sumReduceMetrics(arrayGpBy);
      case 'median':
        return this.medianReduceMetrics(arrayGpBy);
      case 'variance':
        return this.varianceReduceMetrics(arrayGpBy);
      case 'stdev':
        return this.stdevReduceMetrics(arrayGpBy);
      case 'count':
        return this.countReduceMetrics(arrayGpBy);
    }
  };

  public computeFlatMetric = (arr: ModelRecord[]) => {
    return arr.map(obj => {
      const metricField = _.pick(obj, 'startTime', 'metrics');
      const flatMetric: any = { date: metricField.startTime };
      metricField.metrics.forEach((kvPair: any) => {
        this.yAxisParams.add(kvPair.key);
        flatMetric[kvPair.key] = kvPair.value;
      });
      return flatMetric;
    });
  };

  public averageReduceMetrics = (mapObj: any) => {
    return [...mapObj].map(obj => {
      return { key: obj[0], value: this.average(obj[1]) };
    });
  };

  public sumReduceMetrics = (mapObj: any) => {
    return [...mapObj].map(obj => {
      return { key: obj[0], value: this.sum(obj[1]) };
    });
  };

  public medianReduceMetrics = (mapObj: any) => {
    return [...mapObj].map(obj => {
      return { key: obj[0], value: this.median(obj[1]) };
    });
  };

  public varianceReduceMetrics = (mapObj: any) => {
    return [...mapObj].map(obj => {
      return { key: obj[0], value: this.variance(obj[1]) };
    });
  };

  public stdevReduceMetrics = (mapObj: any) => {
    return [...mapObj].map(obj => {
      return { key: obj[0], value: this.stdev(obj[1]) };
    });
  };

  public countReduceMetrics = (mapObj: any) => {
    return [...mapObj].map(obj => {
      return { key: obj[0], value: this.count(obj[1]) };
    });
  };

  public reduceMetricForAgg = (groupedParams: any, selectedAgg: string) => {
    const reduceAverage = (array: any) => array.reduce((a: any, b: any) => a + b) / array.length;
    const reduceSum = (array: any) => array.reduce((a: any, b: any) => a + b);

    // const dogSwitch = (breed: any) =>
    //   ({
    //     border: 'Border Collies are good boys and girls.',
    //     pitbull: 'Pit Bulls are good boys and girls.',
    //     german: 'German Shepherds are good boys and girls.'
    //   }[breed]);
    // dogSwitch('border');

    // const returnReduceBySelection = (obj:any) => {
    //   switch (selectedAgg) {
    //     case 'average':
    // }
    // switch (selectedAgg) {
    //   case 'average':
    //     return [...groupedParams].map(obj => {
    //       return { key: obj[0], value: reduceAverage(obj[1]) };
    //     });
    //   case 'sum':
    //   return [...groupedParams].map(obj => {
    //     return { key: obj[0], value: reduceSum(obj[1]) };
    //   });
    // }
  };

  public computeXAxisFields = (expRuns: ModelRecord[]) => {
    this.xAxisParams.add('experimentId');
    this.xAxisParams.add('projectId');
    this.xAxisParams.add('id');
    this.xAxisParams.add('startTime');
    return expRuns.map(modeRecord => {
      const fields: any = {};
      if (modeRecord.metrics) {
        modeRecord.metrics.forEach((kvPair: any) => {
          this.xAxisParams.add(kvPair.key);
          fields[kvPair.key] = kvPair.value;
        });
      }
      if (modeRecord.hyperparameters) {
        modeRecord.hyperparameters.forEach((kvPair: any) => {
          this.xAxisParams.add(kvPair.key);
          fields[kvPair.key] = kvPair.value;
        });
      }
      if (modeRecord.datasets) {
        modeRecord.datasets.forEach((kvPair: any) => {
          this.xAxisParams.add(kvPair.key);
          fields[`dataset_${kvPair.key}`] = kvPair.path;
        });
      }
      if (modeRecord.artifacts) {
        modeRecord.artifacts.forEach((kvPair: any) => {
          this.xAxisParams.add(kvPair.key);
          fields[`artifact_${kvPair.key}`] = kvPair.path;
        });
      }
      fields.experiment_id = modeRecord.experimentId;
      fields.project_id = modeRecord.projectId;
      fields.exp_run_id = modeRecord.id;
      fields.start_time = modeRecord.startTime;

      if (modeRecord.codeVersion) {
        fields.code_version = modeRecord.codeVersion;
        this.xAxisParams.add('codeVersion');
      }
      if (modeRecord.owner) {
        fields.owner = modeRecord.owner;
        this.xAxisParams.add('owner');
      }
      if (modeRecord.tags) {
        modeRecord.tags.forEach((tag: string) => {
          fields[`tag_${tag}`] = tag;
        });
      }
      return fields;
    });
  };
}
