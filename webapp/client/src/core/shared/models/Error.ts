export type AppError<CustomErrorType extends string | undefined = undefined> =
  | CodeError
  | HttpError<CustomErrorType>;

export function isAppError(error: any): error is AppError<undefined> {
  return Boolean(
    'name' in error && (error.name === 'apiError' || error.name === 'codeError')
  );
}

export const createCodeErrorFromError = (error: Error): CodeError => {
  return error as any;
};

export const createCodeError = (message: string): CodeError =>
  new CodeError({ message });

export class CodeError extends Error {
  public name: 'codeError';

  public constructor({ message }: { message: string }) {
    super(message);
    this.name = 'codeError';
  }

  public toString() {
    return this.message;
  }
}

export function isHttpError(error: any): error is HttpError<undefined> {
  return Boolean('name' in error && error.name === 'apiError');
}

export class HttpError<
  CustomApiErrorType extends string | undefined
> extends Error {
  public name: 'apiError';
  public category: 'clientError' | 'serverError';
  public isMessageUserFriendly?: boolean;

  public type: CustomApiErrorType;

  public status: number;

  public constructor({
    type,
    status,
    message,
    isMessageUserFriendly = false,
  }: {
    status: number;
    type?: CustomApiErrorType;
    message?: string;
    isMessageUserFriendly?: boolean;
  }) {
    super(message);
    this.name = 'apiError';
    this.category =
      status >= 400 && status < 500 ? 'clientError' : 'serverError';
    this.type = (type || undefined) as any;
    this.status = status;
    this.isMessageUserFriendly = isMessageUserFriendly;
  }
}

export const makeHttpErrorWithUserFriendlyMessage = (opts: {
  status: number;
  message: string;
}) => {
  return new HttpError({
    status: opts.status,
    message: opts.message,
    type: undefined,
    isMessageUserFriendly: true,
  });
};

export type HttpErrorWithUserFriendlyMessage = HttpError<any> & {
  isMessageUserFriendly: true;
};

export const handleCustomErrorWithFallback = <T extends string, R>(
  error: AppError<T>,
  map: Record<T, (error: AppError<T>) => R>,
  fallback: (error: AppError | Error) => R
): R | string => {
  const appErrorsHandlers: {
    [ErrorName in AppError['name']]: (
      appError: Extract<AppError<T>, { name: ErrorName }>
    ) => string
  } = {
    apiError: apiError => {
      const handler = map[apiError.type];
      return handler ? (handler(apiError) as any) : fallback(error);
    },
    codeError: codeError => fallback(codeError) as any,
  };
  const handleAppError = appErrorsHandlers[error.name];
  return handleAppError ? handleAppError(error as any) : fallback(error);
};

export const handleHttpError = <R>(
  error: AppError<any>,
  f: (error: HttpError<undefined>) => R,
  fallback: (error: AppError | Error) => R
): R => {
  return isHttpError(error) ? f(error) : fallback(error);
};

export const handleUserFriendlyHttpError = <R>(
  error: AppError<any>,
  f: (error: HttpErrorWithUserFriendlyMessage) => R,
  fallback: (error: AppError<any> | Error) => R
): R => {
  return handleHttpError(
    error,
    httpError =>
      httpError.isMessageUserFriendly
        ? f(httpError as any)
        : fallback(httpError),
    fallback
  );
};
