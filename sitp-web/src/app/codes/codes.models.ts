export interface CreatedCode {
  id: number;
  code: string;
  courseId: number;
  courseTitle: string;
  status: 'ACTIVE' | 'REVOKED';
  redeemedAt?: string | null;
  claimantUsername?: string | null;
  createdAt?: string | null;
}

export interface RedeemConfirmation {
  courseId: number;
  courseTitle: string;
  accessUntil: string;
}
