import * as R from 'ramda';
import moment from 'moment';

import { Milliseconds } from '../types';
import { combineValidators } from '../validators';

export const formatDurationMs = (value: Milliseconds): string => {
  return R.pipe(
    () => [],
    showIfNotZero(moment.duration(value).hours(), hours => `${hours}h`),
    showIfNotZero(moment.duration(value).minutes(), minutes => `${minutes}m`),
    showIfNotZero(moment.duration(value).seconds(), seconds => `${seconds}s`),
    showIfNotZero(
      moment.duration(value).milliseconds(),
      milliseconds => `${milliseconds}ms`
    ),
    R.join('')
  )();
};

export const formatDuration = (value: number): string => {
  return formatDurationMs(value * 1000);
};

export const parseDurationMs = (duration: string) => {
  const parse = ({
    ending,
    milliseconds,
  }: {
    ending: string;
    milliseconds: number;
  }) => (sanitazedDuration: string) => {
    const foundComponent = (sanitazedDuration.match(/(\d+[a-z]+)/g) || [])
      .map(strComponent => (strComponent.match(/(\d+)([a-z]+)/) || []).slice(1))
      .filter(component => component.length === 2)
      .find(component => component[1] === ending);
    return foundComponent ? parseInt(foundComponent[0]) * milliseconds : 0;
  };

  const durationComponentDefs: Record<
    string,
    { ending: string; milliseconds: number }
  > = {
    milliseconds: { milliseconds: 1, ending: 'ms' },
    seconds: { milliseconds: 1000, ending: 's' },
    minutes: { milliseconds: 60000, ending: 'm' },
    hours: { milliseconds: 3600000, ending: 'h' },
  };

  return Object.values(durationComponentDefs).reduce(
    (res, def) => res + parse(def)(duration.replace(/\s+/, '')),
    0
  );
};

export const parseDuration = (duration: string) => {
  const parse = ({ ending, seconds }: { ending: string; seconds: number }) => (
    sanitazedDuration: string
  ) => {
    const foundComponent = (sanitazedDuration.match(/(\d+[a-z]+)/g) || [])
      .map(strComponent => (strComponent.match(/(\d+)([a-z]+)/) || []).slice(1))
      .filter(component => component.length === 2)
      .find(component => component[1] === ending);
    return foundComponent ? parseFloat(foundComponent[0]) * seconds : 0;
  };

  const durationComponentDefs: Record<
    string,
    { ending: string; seconds: number }
  > = {
    milliseconds: { seconds: 0.001, ending: 'ms' },
    seconds: { seconds: 1, ending: 's' },
    minutes: { seconds: 60, ending: 'm' },
    hours: { seconds: 3600, ending: 'h' },
  };

  return Object.values(durationComponentDefs).reduce(
    (res, def) => res + parse(def)(duration.replace(/\s+/, '')),
    0
  );
};

export const validateDuration = (
  field: string,
  range: { min: Milliseconds; max: Milliseconds }
) =>
  combineValidators([
    validateDurationFormat(field),
    validateDurationInRange(field, range),
  ]);

const validateDurationFormat = (field: string) => (duration: string) => {
  return /^\d+ms$|^\d+s(\d+ms)?$|^\d+m(\d+s)?(\d+ms)?$|^\d+h(\d+m)?(\d+s)?(\d+ms)?$/.test(
    duration.replace(/\s/g, '')
  )
    ? undefined
    : `invalid ${field} format`;
};

const validateDurationInRange = (
  field: string,
  { min, max }: { min: Milliseconds; max: Milliseconds }
) => {
  return (duration: string) => {
    const durationInMs = moment
      .duration(`PT${duration.toUpperCase()}`)
      .asMilliseconds();
    return durationInMs > min && durationInMs < max
      ? undefined
      : `Allowed ${field} <1s and >24h`;
  };
};

const showIfNotZero = R.curry(
  (number: number, format: (number: number) => string, array: string[]) => {
    if (number !== 0) {
      return [...array, format(number)];
    }
    return array;
  }
);
