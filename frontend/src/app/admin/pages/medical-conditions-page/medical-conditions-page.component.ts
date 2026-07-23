import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { CatalogFieldConfig } from '../../models/catalog.model';
import { CatalogService } from '../../services/catalog.service';
import { CatalogManagerComponent } from '../../shared/catalog-manager/catalog-manager.component';

@Component({
  selector: 'app-medical-conditions-page',
  standalone: true,
  imports: [CatalogManagerComponent],
  template: `
    <app-catalog-manager
      title="Medical Conditions"
      subtitle="Conditions patients can attach to their profile."
      itemNoun="medical condition"
      [fields]="fields"
      [service]="service"
    />
  `,
})
export class MedicalConditionsPageComponent {
  /** Matches MedicalConditionRequest(name, description) — confirmed against your DTO. */
  fields: CatalogFieldConfig[] = [
    { key: 'name', label: 'Name', type: 'text', required: true, placeholder: 'e.g. Type 2 Diabetes' },
    { key: 'description', label: 'Description', type: 'textarea', placeholder: 'Optional notes' },
  ];

  private http = inject(HttpClient);

  service = new CatalogService(
    this.http,
    'http://localhost:8080/api/v1/admin/catalog/medical-conditions',
    'http://localhost:8080/api/v1/medical-conditions'
  );
}
