import moment from 'moment';

import { formatDurationMs, parseDurationMs } from '..';

describe('(utils) duration', () => {
  describe('formatDuration', () => {
    it('should format milliseconds as duration', () => {
      expect(
        formatDurationMs(
          moment.duration({ milliseconds: 100 }).asMilliseconds()
        )
      ).toEqual('100ms');
      expect(
        formatDurationMs(moment.duration({ seconds: 1 }).asMilliseconds())
      ).toEqual('1s');
      expect(
        formatDurationMs(moment.duration({ minutes: 15 }).asMilliseconds())
      ).toEqual('15m');

      expect(
        formatDurationMs(
          moment
            .duration({
              hours: 2,
              minutes: 15,
              seconds: 5,
              milliseconds: 100,
            })
            .asMilliseconds()
        )
      ).toEqual('2h15m5s100ms');
    });
  });

  describe('parseDuration', () => {
    it('should parse duration as milliseconds', () => {
      expect(parseDurationMs('1ms')).toEqual(1);
      expect(parseDurationMs('1s')).toEqual(1000);
      expect(parseDurationMs('1s5ms')).toEqual(1005);
      expect(parseDurationMs('1m')).toEqual(60000);
      expect(parseDurationMs('1h15m5s300ms')).toEqual(4505300);
      expect(parseDurationMs('1h 1m 1s')).toEqual(3661000);
    });
  });
});
