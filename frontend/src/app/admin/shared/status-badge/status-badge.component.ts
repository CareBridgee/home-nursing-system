import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

export type BadgeTone = 'neutral' | 'accent' | 'success' | 'danger';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="badge badge--{{ tone }}"><ng-content /></span>`,
  styles: [`
    .badge {
      display: inline-flex;
      align-items: center;
      gap: 0.35rem;
      padding: 0.28rem 0.7rem;
      border-radius: 999px;
      font: 600 0.72rem/1 var(--admin-font-body);
      letter-spacing: 0.03em;
      text-transform: uppercase;
      white-space: nowrap;
    }
    .badge--neutral  { background: var(--admin-surface-sunken); color: var(--admin-ink-soft); }
    .badge--accent   { background: var(--admin-accent-soft);    color: #8A5417; }
    .badge--success  { background: var(--admin-success-soft);   color: var(--admin-success); }
    .badge--danger   { background: var(--admin-danger-soft);    color: var(--admin-danger); }
  `],
})
export class StatusBadgeComponent {
  @Input() tone: BadgeTone = 'neutral';
}
