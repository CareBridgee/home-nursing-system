import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { CatalogFieldConfig } from '../../models/catalog.model';
import { CatalogService } from '../../services/catalog.service';
import { CatalogManagerComponent } from '../../shared/catalog-manager/catalog-manager.component';

@Component({
  selector: 'app-allergies-page',
  standalone: true,
  imports: [CatalogManagerComponent],
  template: `
    <app-catalog-manager
      title="Allergies"
      subtitle="Allergies patients can attach to their profile."
      itemNoun="allergy"
      [fields]="fields"
      [service]="service"
    />
  `,
})
export class AllergiesPageComponent {
  /** Matches AllergyRequest(String name, AllergyType type). */
  fields: CatalogFieldConfig[] = [
    { key: 'name', label: 'Name', type: 'text', required: true, placeholder: 'e.g. Penicillin' },
    {
      key: 'type',
      label: 'Type',
      type: 'select',
      required: true,
      options: [
        { label: 'Drug', value: 'DRUG' },
        { label: 'Food', value: 'FOOD' },
        { label: 'Other', value: 'OTHER' },
      ],
    },
  ];

  private http = inject(HttpClient);

  // 1st arg: Admin URL for POST/PUT/DELETE
  // 2nd arg: Public URL for GET (list)
  service = new CatalogService(
    this.http,
    'http://localhost:8080/api/v1/admin/catalog/allergies',
    'http://localhost:8080/api/v1/allergies'
  );
}
