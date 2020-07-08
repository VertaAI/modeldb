const cloneClassInstance = <T>(orig: T): T =>
  Object.assign(Object.create(Object.getPrototypeOf(orig)), orig);

export default cloneClassInstance;
