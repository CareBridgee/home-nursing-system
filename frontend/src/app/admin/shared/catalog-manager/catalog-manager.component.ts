import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CatalogFieldConfig, CatalogItem } from '../../models/catalog.model';
import { CatalogService } from '../../services/catalog.service';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-catalog-manager',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ConfirmDialogComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './catalog-manager.component.html',
  styleUrl: './catalog-manager.component.scss',
})
export class CatalogManagerComponent implements OnInit {
  /** Page heading, e.g. "Medications". */
  @Input({ required: true }) title!: string;
  /** One-line description shown under the heading. */
  @Input() subtitle = '';
  /** Noun used in copy, e.g. "medication" — lowercase, singular. */
  @Input({ required: true }) itemNoun!: string;
  /** Field-driven form + table column definition — order here is display order. */
  @Input({ required: true }) fields!: CatalogFieldConfig[];
  /** Service instance talking to the right admin endpoint. */
  @Input({ required: true }) service!: CatalogService;

  items = signal<CatalogItem[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  saving = signal(false);

  formOpen = signal(false);
  editingId = signal<string | null>(null);
  pendingDeleteId = signal<string | null>(null);

  private fb = inject(FormBuilder);
  form = this.fb.group({});

  ngOnInit(): void {
    this.buildForm();
    this.refresh();
  }

  private buildForm(): void {
    const controls: Record<string, any> = {};
    for (const field of this.fields) {
      const defaultValue = field.type === 'checkbox' ? false : field.type === 'number' ? null : '';
      controls[field.key] = [defaultValue, field.required ? [Validators.required] : []];
    }
    this.form = this.fb.group(controls);
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.list().subscribe({
      next: (data) => {
        this.items.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(`Couldn't load ${this.itemNoun}s. Check the API connection and try again.`);
        this.loading.set(false);
      },
    });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form.reset();
    this.formOpen.set(true);
  }

  openEdit(item: CatalogItem): void {
    this.editingId.set(item.id);
    this.form.reset();
    this.form.patchValue(item as any);
    this.formOpen.set(true);
  }

  closeForm(): void {
    this.formOpen.set(false);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);

    // Coerce number-field values: <input type="number"> yields strings from
    // reactive forms, but the backend DTOs expect Integer/BigDecimal.
    const raw = this.form.value as Record<string, any>;
    const payload: Record<string, any> = {};
    for (const field of this.fields) {
      const value = raw[field.key];
      payload[field.key] = field.type === 'number' && value !== null && value !== ''
        ? Number(value)
        : value;
    }

    const id = this.editingId();
    const request$ = id ? this.service.update(id, payload) : this.service.create(payload);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.formOpen.set(false);
        this.refresh();
      },
      error: () => {
        this.saving.set(false);
        this.error.set(`Couldn't save this ${this.itemNoun}. Check the form and try again.`);
      },
    });
  }

  confirmDelete(item: CatalogItem): void {
    this.pendingDeleteId.set(item.id);
  }

  cancelDelete(): void {
    this.pendingDeleteId.set(null);
  }

  performDelete(): void {
    const id = this.pendingDeleteId();
    if (!id) return;
    this.service.delete(id).subscribe({
      next: () => {
        this.pendingDeleteId.set(null);
        this.refresh();
      },
      error: () => {
        this.pendingDeleteId.set(null);
        this.error.set(`Couldn't delete this ${this.itemNoun}. It may be referenced elsewhere.`);
      },
    });
  }

  /** Formats a field's value for table display — resolves select labels, boolean glyphs, etc. */
  displayValue(item: CatalogItem, field: CatalogFieldConfig): string {
    const value = item[field.key];
    if (value === null || value === undefined || value === '') return '—';
    if (field.type === 'checkbox') return value ? 'Yes' : 'No';
    if (field.type === 'select') {
      return field.options?.find((o) => o.value === value)?.label ?? String(value);
    }
    return String(value);
  }
}
