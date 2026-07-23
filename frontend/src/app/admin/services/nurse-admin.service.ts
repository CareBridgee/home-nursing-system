import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { forkJoin, map, Observable } from 'rxjs';
import { NurseRejectionRequest, NurseResponse, VerificationStatus } from '../models/nurse.model';

const BASE_PATH = 'http://localhost:8080/api/v1/admin/nurses';

@Injectable({ providedIn: 'root' })
export class NurseAdminService {
  constructor(private http: HttpClient) {}

  listByStatus(status: VerificationStatus): Observable<NurseResponse[]> {
    const params = new HttpParams().set('status', status);
    return this.http.get<NurseResponse[]>(BASE_PATH, { params });
  }

  listAll(): Observable<NurseResponse[]> {
    return forkJoin([
      this.listByStatus(VerificationStatus.UNDER_REVIEW),
      this.listByStatus(VerificationStatus.APPROVED),
      this.listByStatus(VerificationStatus.REJECTED),
    ]).pipe(map(([pending, approved, rejected]) => [...pending, ...approved, ...rejected]));
  }

  approve(nurseId: string): Observable<NurseResponse> {
    return this.http.patch<NurseResponse>(`${BASE_PATH}/${nurseId}/approve`, {});
  }

  reject(nurseId: string, request: NurseRejectionRequest): Observable<NurseResponse> {
    return this.http.patch<NurseResponse>(`${BASE_PATH}/${nurseId}/reject`, request);
  }
}
