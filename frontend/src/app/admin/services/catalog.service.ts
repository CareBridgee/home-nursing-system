import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CatalogItem, CatalogItemRequest } from '../models/catalog.model';

/**
 * Thin wrapper around one catalog admin endpoint.
 *
 * NOTE: your pasted controllers only exposed POST / PUT / DELETE (the
 * "write" half of admin catalog management). This assumes GET on the
 * same base path returns the list — most setups pair the admin
 * controller with a public read endpoint at the same or a sibling path.
 * If your list endpoint lives elsewhere (e.g. `/api/v1/catalog/medications`
 * without `/admin`), just change `listPath` where this class is
 * instantiated below.
 */
export class CatalogService {
  constructor(
    private http: HttpClient,
    private basePath: string,
    private listPath: string = basePath,
  ) {}

  list(): Observable<CatalogItem[]> {
    return this.http.get<CatalogItem[]>(this.listPath);
  }

  create(request: CatalogItemRequest): Observable<CatalogItem> {
    return this.http.post<CatalogItem>(this.basePath, request);
  }

  update(id: string, request: CatalogItemRequest): Observable<CatalogItem> {
    return this.http.put<CatalogItem>(`${this.basePath}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.basePath}/${id}`);
  }
}
