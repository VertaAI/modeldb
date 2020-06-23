import { convertNetwork } from '../converters';
import { networkHistoryMock } from './__mocks__/networkHistoryMock';

describe('(networkGraph feature)', () => {
  describe('(networkGraph converters', () => {
    it('"convertNetworkHistory" should convert data correctly', () => {
      expect(convertNetwork(networkHistoryMock)).toMatchSnapshot();
    });
  });
});
