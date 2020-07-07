import matchBy from 'shared/utils/matchBy';

type PathPart<ParamName extends string, ParamValue> = Segment | Param;
type Segment = string;

type Param = SimpleParam<any, any> | OrParam<any, any> | ParamWithMod<any, any>;
type SimpleParam<Name extends string, Value> = {
  type: 'param';
  subtype: 'simple';
  name: Name;
};
type OrParam<Name extends string, Values extends string[]> = {
  type: 'param';
  subtype: 'or';
  name: Name;
  values: Values;
};
type ParamWithMod<
  NameAndValue extends Record<any, any>,
  Modifier extends ParamModifier
> = {
  type: 'param';
  subtype: 'withModifier';
  name: NameAndValue['name'];
  modifier: Modifier;
};
type ParamModifier = '+' | '*' | '?';

type ParamValue<T extends Param> = T extends SimpleParam<any, infer Value>
  ? Value
  : T extends OrParam<any, infer Value>
  ? Value[number]
  : T extends ParamWithMod<infer NameAndValue, any>
  ? NameAndValue
  : never;

type ParamsWithMod<T extends PathPart<any, any>[], Def> = Extract<
  T[number],
  { type: 'param'; subtype: 'withModifier' }
> extends never
  ? Def
  : Extract<
      T[number],
      { type: 'param'; subtype: 'withModifier' }
    > extends ParamWithMod<infer G, any>
  ? G
  : Def;

type ExtractParamsWithoutMod<T extends PathPart<any, any>[]> = Extract<
  T[number],
  { type: 'param'; subtype: 'simple' | 'or' }
>;

export interface IPath<Params, QueryParams> {
  value: string;
}

export type GetParams<T> = T extends IPath<infer Params, any> ? Params : never;
export type GetQueryParams<T> = T extends IPath<any, infer QueryParams>
  ? QueryParams
  : never;

export function makePath<
  T extends PathPart<any, any>[],
  ParamsWithoutMod = {
    [K in ExtractParamsWithoutMod<T>['name']]: ParamValue<
      Extract<ExtractParamsWithoutMod<T>, { name: K }>
    >
  }
>(...parts: T) {
  return function<QueryParams = undefined>(): IPath<
    ParamsWithoutMod & ParamsWithMod<T, ParamsWithoutMod>,
    Partial<QueryParams>
  > {
    const path = `/${parts
      .map(part => (typeof part === 'string' ? part : paramToPath(part)))
      .join('/')}`.replace(/\/+/g, '/');
    return { value: path };
  };
}
const paramToPath = (param: Param): string =>
  matchBy(param, 'subtype')({
    simple: simpleParam => `:${simpleParam.name}`,
    or: orParam => `:${orParam.name}(${orParam.values.join('|')})`,
    withModifier: paramWithMod =>
      `:${paramWithMod.name}${paramWithMod.modifier}`,
  });

export const queryParams = () => {};

export function param<Name extends string>(name: Name) {
  return <Value = string>(): SimpleParam<Name, Value> => ({
    type: 'param',
    name,
    subtype: 'simple',
  });
}

export function orParam<Name extends string>(name: Name) {
  return <Value extends string>(values: Value[]): OrParam<Name, Value[]> => ({
    type: 'param',
    name,
    subtype: 'or',
    values,
  });
}

export function paramWithMod<Name extends string>(
  name: Name,
  modifier: ParamModifier
) {
  return <
    NameAndValue extends Record<any, any>
  >(): keyof NameAndValue extends Name
    ? ParamWithMod<NameAndValue, ParamModifier>
    : { __compileError: 'wrong param name' } => {
    return {
      type: 'param',
      subtype: 'withModifier',
      name,
      modifier,
    } as keyof NameAndValue extends Name
      ? ParamWithMod<NameAndValue, ParamModifier>
      : { __compileError: 'wrong param name' };
  };
}
