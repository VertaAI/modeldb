export function makeMockServiceMethod(path: string) {
  jest.mock(path);
  return { mockServiceMethod };
}

export function mockServiceMethod<
  S extends { prototype: any },
  M extends keyof S['prototype']
>(
  service: S,
  methodName: M,
  behaviour: jest.Mock<ReturnType<S['prototype'][M]>>
) {
  (service as any).prototype[methodName] = behaviour;
}
