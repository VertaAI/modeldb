import { ICommunicationActionTypes } from './types';

export default function makeCommunicationActionTypes<
  R extends string,
  S extends string,
  F extends string
>({
  request,
  success,
  failure,
}: ICommunicationActionTypes<R, S, F>): ICommunicationActionTypes<R, S, F> {
  return { request, success, failure };
}
