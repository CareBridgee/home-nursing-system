import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { CatalogFieldConfig } from '../../models/catalog.model';
import { CatalogService } from '../../services/catalog.service';
import { CatalogManagerComponent } from '../../shared/catalog-manager/catalog-manager.component';

@Component({
  selector: 'app-medications-page',
  standalone: true,
  imports: [CatalogManagerComponent],
  template: `
    <app-catalog-manager
      title="Medications"
      subtitle="Manage the medication catalog nurses and profiles reference."
      itemNoun="medication"
      [fields]="fields"
      [service]="service"
    />
  `,
})
export class MedicationsPageComponent {
  /** Matches MedicationRequest(name) — no description field on the backend. */
  fields: CatalogFieldConfig[] = [
    { key: 'name', label: 'Name', type: 'text', required: true, placeholder: 'e.g. Amoxicillin' },
  ];

  private http = inject(HttpClient);
  service = new CatalogService(
    this.http,
    'http://localhost:8080/api/v1/admin/catalog/medications',
    'http://localhost:8080/api/v1/medications'
  );
}
