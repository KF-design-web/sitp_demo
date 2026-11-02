export interface PaymentProofRow {
  id: number;
  uploaderId: number;
  uploaderName: string;
  uploaderUsername: string;
  courseId: number;
  courseTitle: string;
  paymentMethod: string;
  amount: number;
  fileUrl: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  createdAt: string;
  decidedAt: string | null;
}
