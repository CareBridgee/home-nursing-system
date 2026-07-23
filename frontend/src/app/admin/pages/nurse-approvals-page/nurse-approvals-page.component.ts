import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NurseResponse, VerificationStatus } from '../../models/nurse.model';
import { NurseAdminService } from '../../services/nurse-admin.service';
import { StatusBadgeComponent, BadgeTone } from '../../shared/status-badge/status-badge.component';

interface StatusTab {
  label: string;
  value: VerificationStatus | null;
}

@Component({
  selector: 'app-nurse-approvals-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, StatusBadgeComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './nurse-approvals-page.component.html',
  styleUrl: './nurse-approvals-page.component.scss',
})
export class NurseApprovalsPageComponent implements OnInit {
  readonly VerificationStatus = VerificationStatus;

  tabs: StatusTab[] = [
    { label: 'Pending review', value: VerificationStatus.UNDER_REVIEW },
    { label: 'Approved', value: VerificationStatus.APPROVED },
    { label: 'Rejected', value: VerificationStatus.REJECTED },
    { label: 'All', value: null },
  ];

  activeTab = signal<StatusTab>(this.tabs[0]);
  nurses = signal<NurseResponse[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  actioningId = signal<string | null>(null);

  rejectDialogOpen = signal(false);
  rejectTargetId = signal<string | null>(null);

  private nurseAdminService = inject(NurseAdminService);
  private fb = inject(FormBuilder);
  rejectForm = this.fb.group({
    reason: ['', [Validators.required, Validators.minLength(5)]],
  });

  ngOnInit(): void {
    this.refresh();
  }

  selectTab(tab: StatusTab): void {
    this.activeTab.set(tab);
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set(null);
    this.nurseAdminService.listByStatus(this.activeTab().value).subscribe({
      next: (data) => {
        this.nurses.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set("Couldn't load nurse applications. Check the API connection and try again.");
        this.loading.set(false);
      },
    });
  }

  approve(nurse: NurseResponse): void {
    this.actioningId.set(nurse.id);
    this.nurseAdminService.approve(nurse.id).subscribe({
      next: () => {
        this.actioningId.set(null);
        this.refresh();
      },
      error: () => {
        this.actioningId.set(null);
        this.error.set(`Couldn't approve ${nurse.fullName}. Try again.`);
      },
    });
  }

  openReject(nurse: NurseResponse): void {
    this.rejectTargetId.set(nurse.id);
    this.rejectForm.reset();
    this.rejectDialogOpen.set(true);
  }

  closeReject(): void {
    this.rejectDialogOpen.set(false);
    this.rejectTargetId.set(null);
  }

  submitReject(): void {
    if (this.rejectForm.invalid) {
      this.rejectForm.markAllAsTouched();
      return;
    }
    const id = this.rejectTargetId();
    if (!id) return;

    this.actioningId.set(id);
    this.nurseAdminService.reject(id, { reason: this.rejectForm.value.reason! }).subscribe({
      next: () => {
        this.actioningId.set(null);
        this.closeReject();
        this.refresh();
      },
      error: () => {
        this.actioningId.set(null);
        this.error.set("Couldn't reject this application. Try again.");
      },
    });
  }

  statusTone(status: VerificationStatus): BadgeTone {
    switch (status) {
      case VerificationStatus.APPROVED: return 'success';
      case VerificationStatus.REJECTED: return 'danger';
      default: return 'accent';
    }
  }
}
