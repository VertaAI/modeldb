export function makeMockServiceMethod(path: string) {
  jest.mock(path);
  return { mockServiceMethod };
}

export function makeMockedService<S extends { prototype: any }>({
  service,
}: {
  path: string;
  service: S;
}) {
  return {
    mockMethod: <M extends keyof S['prototype']>(
      name: M,
      behaviour: jest.Mock<ReturnType<S['prototype'][M]>>
    ) => {
      (service as any).prototype[name] = behaviour;
      return behaviour;
    },
  };
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
  return behaviour;
}
