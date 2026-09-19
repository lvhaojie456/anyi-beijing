// Shared API error type. Lives outside index.ts so feature modules (admin
// console, Live2D) can throw it without importing the whole app module.
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    readonly details?: unknown
  ) {
    super(code);
  }
}
