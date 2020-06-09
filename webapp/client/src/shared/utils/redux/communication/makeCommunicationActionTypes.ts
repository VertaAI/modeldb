import { ICommunicationActionTypes } from './types';

export default function makeCommunicationActionTypes<
  R extends string,
  S extends string,
  F extends string
>({
  REQUEST: request,
  SUCCESS: success,
  FAILURE: failure,
}: ICommunicationActionTypes<R, S, F>): ICommunicationActionTypes<R, S, F> {
  return { REQUEST: request, SUCCESS: success, FAILURE: failure };
}
