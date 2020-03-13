import {
  AppError,
  createCodeErrorFromError,
  isAppError,
} from 'core/shared/models/Error';

export default function normalizeError(error: AppError | Error): AppError<any> {
  if (isAppError(error)) {
    return error;
  }
  return createCodeErrorFromError(error);
}
