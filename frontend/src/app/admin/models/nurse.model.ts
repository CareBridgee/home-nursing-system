export enum VerificationStatus {
  UNDER_REVIEW = 'UNDER_REVIEW',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
}

/**
 * ASSUMPTION: NurseResponse's exact fields weren't shown. Adjust to match
 * your real DTO — every field below is optional-safe in the template via
 * `?.` so removing/renaming one won't break the page, only the column
 * that reads it.
 */
export interface NurseResponse {
  id: string;
  fullName: string;
  email: string;
  phone?: string;
  licenseNumber?: string;
  yearsOfExperience?: number;
  verificationStatus: VerificationStatus;
  appliedAt?: string;
  rejectionReason?: string;
}

export interface NurseRejectionRequest {
  reason: string;
}
