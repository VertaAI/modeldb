import * as R from 'ramda';
import groupArray from 'group-array';
import _ from 'lodash';
import { numberRounded } from 'core/shared/utils/formatters/number';

let aggregatedObject = [];
function getUniqueKey(obj, configArray) {
  return configArray
    .map(key => {
      return obj[0][key];
    })
    .join();
}

function computeAverageMetric(vals, metricArray, uKey) {
  return metricArray.map(key => {
    return {
      indexKey: '(' + uKey + ')',
      metricKey: key,
      value: numberRounded(_.meanBy(vals, key)),
    };
  });
}

function traversal(obj, hypArray, metricArray) {
  _.forIn(obj, function(val, key) {
    if (Array.isArray(val)) {
      let uKey = getUniqueKey(val, hypArray);
      aggregatedObject.push(computeAverageMetric(val, metricArray, uKey));
    } else {
      traversal(obj[key], hypArray, metricArray);
    }
  });
}

function prepareGroupedLineData(
  flatData,
  selectedHyperparamList,
  selectedMetricList
) {
  var promiseTraversal = new Promise(function(resolve, reject) {
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
      let groupResult = groupArray(R.flatten(aggregatedObject), 'metricKey');
      return Object.keys(groupResult).map(key => {
        return { lineIndex: key, values: groupResult[key] };
      });
    })
    .finally(function() {
      aggregatedObject = [];
    });
}

export default prepareGroupedLineData;
