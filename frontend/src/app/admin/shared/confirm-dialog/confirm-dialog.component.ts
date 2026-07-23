import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (open) {
      <div class="scrim" (click)="cancelled.emit()">
        <div class="panel" role="alertdialog" aria-modal="true" (click)="$event.stopPropagation()">
          <h3>{{ title }}</h3>
          <p>{{ message }}</p>
          <div class="actions">
            <button type="button" class="btn btn--ghost" (click)="cancelled.emit()">Cancel</button>
            <button type="button" class="btn btn--danger" (click)="confirmed.emit()">{{ confirmLabel }}</button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .scrim {
      position: fixed; inset: 0; background: rgba(18, 32, 28, 0.42);
      display: grid; place-items: center; z-index: 60; padding: 1rem;
    }
    .panel {
      background: var(--admin-surface); border-radius: var(--admin-radius-lg);
      box-shadow: var(--admin-shadow-lg); padding: 1.75rem; width: 100%; max-width: 380px;
    }
    h3 { margin: 0 0 0.5rem; font: 600 1.05rem var(--admin-font-display); color: var(--admin-ink); }
    p { margin: 0 0 1.5rem; color: var(--admin-ink-soft); font: 400 0.9rem/1.5 var(--admin-font-body); }
    .actions { display: flex; justify-content: flex-end; gap: 0.6rem; }
    .btn {
      font: 600 0.85rem var(--admin-font-body); padding: 0.55rem 1.1rem;
      border-radius: var(--admin-radius-sm); border: 1px solid transparent; cursor: pointer;
    }
    .btn--ghost { background: transparent; border-color: var(--admin-border); color: var(--admin-ink); }
    .btn--ghost:hover { background: var(--admin-surface-sunken); }
    .btn--danger { background: var(--admin-danger); color: #fff; }
    .btn--danger:hover { background: #96371f; }
  `],
})
export class ConfirmDialogComponent {
  @Input() open = false;
  @Input() title = 'Are you sure?';
  @Input() message = 'This action cannot be undone.';
  @Input() confirmLabel = 'Delete';
  @Output() confirmed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();
}
