import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

interface NavLink {
  label: string;
  path: string;
  icon: string;
}

@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './admin-shell.component.html',
  styleUrl: './admin-shell.component.scss',
})
export class AdminShellComponent {
  navLinks: NavLink[] = [
    { label: 'Nurse Applications', path: 'nurses', icon: '👤' },
    { label: 'Medications', path: 'medications', icon: '💊' },
    { label: 'Service Types', path: 'service-types', icon: '🩺' },
    { label: 'Medical Conditions', path: 'medical-conditions', icon: '📋' },
    { label: 'Allergies', path: 'allergies', icon: '⚠️' },
  ];
}
