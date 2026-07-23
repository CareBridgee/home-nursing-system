/**
 * Generic-but-flexible shape used across the four catalog admin controllers.
 * Each entity has different real fields (Allergy has `type`, ServiceType has
 * `estimatedDurationMinutes`/`basePrice`, etc.) so this is intentionally an
 * index-signature bag rather than one fixed interface — the actual shape for
 * each entity lives in that entity's `fields: CatalogFieldConfig[]` array in
 * its page component.
 */
export interface CatalogItem {
  id: string;
  [key: string]: any;
}

export type CatalogItemRequest = Record<string, any>;

export interface CatalogFieldOption {
  label: string;
  value: string;
}

/** Declarative description of one form field + table column. */
export interface CatalogFieldConfig {
  key: string;
  label: string;
  type: 'text' | 'textarea' | 'number' | 'select' | 'checkbox';
  required?: boolean;
  placeholder?: string;
  /** Required when type === 'select'. */
  options?: CatalogFieldOption[];
  /** For number fields, e.g. "0.01" for currency. Defaults to "1". */
  step?: string;
}
