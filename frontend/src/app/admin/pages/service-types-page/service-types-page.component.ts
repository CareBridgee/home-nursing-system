import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { CatalogFieldConfig } from '../../models/catalog.model';
import { CatalogService } from '../../services/catalog.service';
import { CatalogManagerComponent } from '../../shared/catalog-manager/catalog-manager.component';

@Component({
  selector: 'app-service-types-page',
  standalone: true,
  imports: [CatalogManagerComponent],
  template: `
    <app-catalog-manager
      title="Service Types"
      subtitle="The services nurses can offer and families can book."
      itemNoun="service type"
      [fields]="fields"
      [service]="service"
    />
  `,
})
export class ServiceTypesPageComponent {
  /** Matches ServiceTypeRequest(name, description, estimatedDurationMinutes, basePrice). */
  fields: CatalogFieldConfig[] = [
    { key: 'name', label: 'Name', type: 'text', required: true, placeholder: 'e.g. Wound Care' },
    { key: 'description', label: 'Description', type: 'textarea', placeholder: 'Optional notes' },
    { key: 'estimatedDurationMinutes', label: 'Duration (minutes)', type: 'number', required: true, step: '1' },
    { key: 'basePrice', label: 'Base Price', type: 'number', required: true, step: '0.01' },
  ];

  private http = inject(HttpClient);
  service = new CatalogService(
    this.http,
    'http://localhost:8080/api/v1/admin/catalog/service-types',
    'http://localhost:8080/api/v1/service-types'
  );
}
