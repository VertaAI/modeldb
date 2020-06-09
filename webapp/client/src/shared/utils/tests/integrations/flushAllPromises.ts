export default function flushAllPromises() {
  return new Promise(resolve => setImmediate(resolve));
}
