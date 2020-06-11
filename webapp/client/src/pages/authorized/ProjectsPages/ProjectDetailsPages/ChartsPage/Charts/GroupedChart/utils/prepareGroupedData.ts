import { numberRounded } from 'core/shared/utils/formatters/number';
import groupArray from 'group-array';
import _ from 'lodash';

interface IBarData {
  metricKey: string;
  value: string | number;
}

interface IAggregatedData {
  indexKey: string;
  values: IBarData[];
}

interface IGroupedData {
  [key: string]: any;
}

interface IFlatData {
  [key: string]: any;
}

let aggregatedObject: IAggregatedData[] = [];
function getUniqueKey(
  obj: any[] | Array<{ [x: string]: any }>,
  configArray: string[]
) {
  return configArray
    .map(key => {
      return obj[0][key];
    })
    .join();
}

function computeAverageMetric(
  vals: any[] | ArrayLike<unknown> | null | undefined,
  metricArray: string[],
  uKey: string
) {
  return metricArray.map(key => {
    const computed = numberRounded(_.meanBy(vals, key));
    return {
      valIndexKey: '(' + uKey + ')',
      metricKey: key,
      value: Number.isNaN(computed) ? 0 : computed,
    };
  });
}

function traversal(
  obj: IGroupedData,
  hypArray: string[],
  metricArray: string[]
) {
  _.forIn(obj, function(val, key) {
    if (Array.isArray(val)) {
      const uKey = getUniqueKey(val, hypArray);
      aggregatedObject.push({
        indexKey: '(' + uKey + ')',
        values: computeAverageMetric(val, metricArray, uKey),
      });
    } else {
      traversal(obj[key], hypArray, metricArray);
    }
  });
  return undefined;
}

function getGroupedChartData(
  flatData: IFlatData,
  selectedHyperparamList: string[],
  selectedMetricList: string[]
) {
  const promiseTraversal = new Promise(function(resolve, reject) {
    resolve(
      traversal(
        groupArray(flatData, selectedHyperparamList),
        selectedHyperparamList,
        selectedMetricList
      )
    );
  });

  return promiseTraversal
    .then(function() {
      return aggregatedObject;
    })
    .finally(function() {
      aggregatedObject = [];
    });
}

export default getGroupedChartData;
