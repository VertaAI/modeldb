import { configure } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';

configure({ adapter: new Adapter() });

// need for poper library
(global as any).document.createRange = () => ({
  setStart: () => {},
  setEnd: () => {},
  commonAncestorContainer: {
    nodeName: 'BODY',
    ownerDocument: document,
  },
});

const mockAxios = new MockAdapter(axios);
mockAxios.onAny().reply(200, {});
