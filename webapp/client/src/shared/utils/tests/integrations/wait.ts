import { act } from 'react-dom/test-utils';

export default async function wait(ms = 0) {
  await act(async () => {
    return new Promise(resolve => {
      setImmediate(resolve);
    }) as any;
  });
}
