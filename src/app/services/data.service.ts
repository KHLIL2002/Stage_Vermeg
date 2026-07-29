import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class DataService {
  private http = inject(HttpClient);

  getPolicies(status?: string): Observable<any[]> {
    const params = status ? `?status=${status}` : '';
    return this.http.get<any[]>(`/api/policies${params}`);
  }

  getPolicyByNumber(policyNumber: string): Observable<any> {
    return this.http.get<any>(`/api/policies/${policyNumber}`);
  }

  getPolicyRoles(policyNumber: string): Observable<any[]> {
    return this.http.get<any[]>(`/api/policies/${policyNumber}/roles`);
  }

  getCoverages(policyNumber?: string): Observable<any[]> {
    if (policyNumber) {
      return this.http.get<any[]>(`/api/policies/${policyNumber}/coverages`);
    }
    return this.http.get<any[]>('/api/coverages');
  }

  getEndorsements(): Observable<any[]> {
    return this.http.get<any[]>('/api/endorsements');
  }

  getThirdParties(): Observable<any[]> {
    return this.http.get<any[]>('/api/third-parties');
  }

  getThirdPartyById(identifier: string): Observable<any> {
    return this.http.get<any>(`/api/third-parties/${identifier}`);
  }

  getPoliciesByHolder(identifier: string): Observable<any[]> {
    return this.getPolicies().pipe(
      map(list => list.filter(p => p.holder?.identifier === identifier))
    );
  }

  getBills(): Observable<any[]> {
    return this.http.get<any[]>('/api/bills');
  }

  getPolicyBills(policyNumber: string): Observable<any[]> {
    return this.getBills().pipe(
      map(bills => bills.filter(b => b.policyNumber === policyNumber))
    );
  }

  getProducts(): Observable<any[]> {
    return this.http.get<any[]>('/api/products');
  }

  createPolicy(data: any): Observable<any> {
    return this.http.post<any>('/api/policies', data);
  }

  createThirdParty(data: any): Observable<any> {
    return this.http.post<any>('/api/third-parties', data);
  }
}
