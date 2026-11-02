import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_URL } from '../core/api-url';
import { PaymentProofRow } from './payments.models';

@Injectable({ providedIn: 'root' })
export class PaymentsService {
  private http = inject(HttpClient);

  submit(courseId: number, amount: string, paymentMethod: string, receipt: File) {
    const form = new FormData();
    form.append('courseId', String(courseId));
    form.append('amount', amount);
    form.append('paymentMethod', paymentMethod);
    form.append('receipt', receipt);
    return this.http.post<PaymentProofRow>(`${API_URL}/payment-proofs`, form);
  }

  pending() {
    return this.http.get<PaymentProofRow[]>(`${API_URL}/payment-proofs/pending`);
  }

  receiptUrl(proofId: number): string {
    return `${API_URL}/payment-proofs/${proofId}/receipt`;
  }

  approve(id: number) {
    return this.http.post<PaymentProofRow>(
      `${API_URL}/payment-proofs/${id}/approve`,
      null,
    );
  }

  reject(id: number) {
    return this.http.post<PaymentProofRow>(
      `${API_URL}/payment-proofs/${id}/reject`,
      null,
    );
  }
}
