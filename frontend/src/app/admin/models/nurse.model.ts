export enum VerificationStatus {
  UNDER_REVIEW = 'UNDER_REVIEW',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
}

export interface NurseResponse {
  id: string;
  userId: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  nationalId: string;
  nationalIdFrontUrl: string;
  nationalIdBackUrl: string;
  licenseImageUrl: string;
  professionalCertificateUrl: string;
  specialization: string;
  yearsOfExperience: number;
  hourlyRate: number;
  bio: string;
  ratingAvg: number;
  totalReviews: number;
  isAvailable: boolean;
  verificationStatus: VerificationStatus;
  rejectionReason: string;
}

// ✅ Matches your Java record: public record FailedStep(String step, String reason) {}
export interface FailedStep {
  step: string;
  reason: string;
}

// ✅ Matches your Java DTO: requires overallReason AND a non-empty failedSteps list
export interface NurseRejectionRequest {
  overallReason: string;
  failedSteps: FailedStep[];
}
