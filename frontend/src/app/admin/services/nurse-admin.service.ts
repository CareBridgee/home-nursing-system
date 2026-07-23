import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { NurseRejectionRequest, NurseResponse, VerificationStatus } from '../models/nurse.model';

const BASE_PATH = 'http://localhost:8080/api/v1/admin/nurses';

@Injectable({ providedIn: 'root' })
export class NurseAdminService {
  constructor(private http: HttpClient) {}

  listByStatus(status: VerificationStatus | null): Observable<NurseResponse[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<NurseResponse[]>(BASE_PATH, { params });
  }

  approve(nurseId: string): Observable<NurseResponse> {
    return this.http.patch<NurseResponse>(`${BASE_PATH}/${nurseId}/approve`, {});
  }

  reject(nurseId: string, request: NurseRejectionRequest): Observable<NurseResponse> {
    return this.http.patch<NurseResponse>(`${BASE_PATH}/${nurseId}/reject`, request);
  }
}
