import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
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
  private nurseAdminService = inject(NurseAdminService);
  private fb = inject(FormBuilder);

  readonly VerificationStatus = VerificationStatus;
  readonly availableSteps = [
    'National ID Front',
    'National ID Back',
    'License',
    'Professional Certificate',
    'Other'
  ];

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

  detailOpen = signal(false);
  selectedNurse = signal<NurseResponse | null>(null);
  rejectFormOpen = signal(false);

  rejectForm = this.fb.group({
    overallReason: ['', [Validators.required, Validators.minLength(10)]],
    failedSteps: this.fb.array<FormGroup>([]),
  });

  get failedStepsArray(): FormArray {
    return this.rejectForm.get('failedSteps') as FormArray;
  }

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
    const status = this.activeTab().value;
    const request$ = status ? this.nurseAdminService.listByStatus(status) : this.nurseAdminService.listAll();
    request$.subscribe({
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

  openDetails(nurse: NurseResponse): void {
    this.selectedNurse.set(nurse);
    this.detailOpen.set(true);
  }

  closeDetails(): void {
    this.detailOpen.set(false);
    this.selectedNurse.set(null);
    this.rejectFormOpen.set(false);
    this.failedStepsArray.clear();
    this.rejectForm.reset();
  }

  openRejectForm(): void {
    this.rejectFormOpen.set(true);
    this.failedStepsArray.clear();
    this.addFailedStep(); // Add one empty step by default so @NotEmpty passes
  }

  closeRejectForm(): void {
    this.rejectFormOpen.set(false);
    this.failedStepsArray.clear();
    this.rejectForm.reset();
  }

  addFailedStep(): void {
    const stepGroup = this.fb.group({
      step: ['', Validators.required],
      reason: ['', Validators.required]
    });
    this.failedStepsArray.push(stepGroup);
  }

  removeFailedStep(index: number): void {
    this.failedStepsArray.removeAt(index);
  }

  approve(nurse: NurseResponse): void {
    this.actioningId.set(nurse.id);
    this.nurseAdminService.approve(nurse.id).subscribe({
      next: () => {
        this.actioningId.set(null);
        this.closeDetails();
        this.refresh();
      },
      error: (err) => {
        console.error('Approve error:', err);
        this.actioningId.set(null);
        this.error.set(`Couldn't approve ${nurse.firstName}. Check console for details.`);
      },
    });
  }

  submitReject(): void {
    // Ensure form is valid AND at least one failed step is added (satisfies @NotEmpty)
    if (this.rejectForm.invalid || this.failedStepsArray.length === 0) {
      this.rejectForm.markAllAsTouched();
      return;
    }

    const nurse = this.selectedNurse();
    if (!nurse) return;

    this.actioningId.set(nurse.id);

    // ✅ This payload now perfectly matches your Java NurseRejectionRequest DTO
    const payload = {
      overallReason: this.rejectForm.value.overallReason!,
      failedSteps: this.failedStepsArray.getRawValue(),
    };

    this.nurseAdminService.reject(nurse.id, payload).subscribe({
      next: () => {
        this.actioningId.set(null);
        this.closeDetails();
        this.refresh();
      },
      error: (err) => {
        console.error('Reject error:', err);
        this.actioningId.set(null);
        this.error.set("Couldn't reject this application. Check console for details.");
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
