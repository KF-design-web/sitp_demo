import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { SessionService } from '../../core/session.service';
import { LangService } from '../../core/lang.service';
import { TrainerService } from '../../trainer/trainer.service';
import { TrainerCourse } from '../../trainer/trainer.models';
import { Button } from '../../ui/button/button';
import { ErrorSlip } from '../../ui/error-slip/error-slip';
import { TextField } from '../../ui/text-field/text-field';
import { SelectField } from '../../ui/select-field/select-field';
import { ConfirmModal } from '../../ui/confirm-modal/confirm-modal';
import { ProfileWidget } from '../../ui/profile-widget/profile-widget';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-trainer-dashboard-page',
  imports: [RouterLink, Button, ErrorSlip, TextField, SelectField, ConfirmModal, ProfileWidget, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto mt-8 max-w-6xl px-4">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <h1 class="text-[28px] font-bold text-ink">{{ t('trainer.heading') }}</h1>
        <div class="flex items-center gap-3">
          <app-button variant="primary" compact (click)="startCreate()">
            + {{ t('trainer.newCourse') }}
          </app-button>
          <app-profile-widget />
        </div>
      </div>

      <p class="mt-1 text-sm text-muted">{{ t('trainer.studioSubtitle') }}</p>

      <div class="mt-5">
        <app-error-slip [message]="errorMessage()" />
      </div>

      @if (loading()) {
        <p class="mt-10 text-sm text-muted">{{ t('trainer.loading') }}</p>
      } @else if (courses().length === 0) {
        <div class="mx-auto mt-10 max-w-xl rounded-xl border border-dashed border-line bg-surface p-10 text-center">
          <p class="text-base font-medium text-ink">{{ t('trainer.emptyTitle') }}</p>
          <p class="mt-1 text-sm text-muted">{{ t('trainer.emptyText') }}</p>
          <app-button variant="primary" compact class="mt-4" (click)="startCreate()">
            {{ t('trainer.newCourse') }}
          </app-button>
        </div>
      } @else {
        <div class="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
          @for (course of courses(); track course.id) {
            <div class="flex flex-col overflow-hidden rounded-xl border border-line bg-surface shadow-card">
              <div class="h-36 w-full bg-primary/5">
                @if (course.thumbnailUrl) {
                  <img [src]="course.thumbnailUrl" alt="" class="h-full w-full object-cover"
                       (error)="$any($event.target).style.display = 'none'" />
                }
              </div>
              <div class="flex flex-1 flex-col p-4">
                <div class="flex items-start justify-between gap-2">
                  <h3 class="text-sm font-semibold leading-snug text-ink">{{ course.title }}</h3>
                  <span class="shrink-0 rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">
                    {{ course.targetAudience === 'INTERN' ? t('courses.audience-INTERN') : t('courses.audience-OUTSIDER') }}
                  </span>
                </div>
                @if (course.labels) {
                  <div class="mt-2 flex flex-wrap gap-1">
                    @for (label of course.labels.split(','); track label) {
                      <span class="rounded-full bg-bg px-2 py-0.5 text-[11px] text-muted">{{ label.trim() }}</span>
                    }
                  </div>
                }
                <p class="mt-2 line-clamp-2 text-xs text-muted">{{ course.description }}</p>

                <div class="mt-4 flex items-center justify-between gap-2 border-t border-line pt-3">
                  <app-button variant="primary" compact routerLink="/trainer/courses/{{ course.id }}">
                    {{ t('trainer.uploadVideos') }}
                  </app-button>
                  <div class="flex items-center gap-2">
                    <app-button variant="secondary" compact (click)="startEdit(course)">
                      {{ t('trainer.editCourse') }}
                    </app-button>
                    <app-button variant="secondary" compact (click)="askDelete(course)">
                      {{ t('trainer.deleteCourse') }}
                    </app-button>
                  </div>
                </div>
              </div>
            </div>
          }
        </div>
      }
    </div>

    @if (courseFormOpen()) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-ink/40 p-4" (click)="closeCourseForm()">
        <section
          role="dialog"
          aria-modal="true"
          class="max-h-[90vh] w-full max-w-md overflow-y-auto rounded-xl border border-line bg-surface p-6 shadow-card"
          (click)="$event.stopPropagation()"
        >
          <h2 class="text-lg font-bold text-ink">
            {{ editingCourse() ? t('trainer.editCourseTitle') : t('trainer.newCourseTitle') }}
          </h2>

          <form [formGroup]="courseForm" (ngSubmit)="submitCourseForm()" class="mt-4 space-y-3">
            <app-text-field formControlName="title" type="text" [label]="t('trainer.title')" [error]="t('error.required')" />
            <div>
              <label class="text-sm font-medium text-ink">{{ t('trainer.description') }}</label>
              <textarea
                formControlName="description"
                rows="3"
                class="mt-1 w-full rounded-xl border border-line bg-surface px-3 py-2 text-base text-ink
                       focus:outline-none focus:ring-2 focus:ring-primary"
              ></textarea>
            </div>
            <app-text-field formControlName="labels" type="text" [label]="t('trainer.labels')" [hint]="t('trainer.labelsHint')" />
            <app-text-field formControlName="thumbnailUrl" type="text" [label]="t('trainer.thumbnail')" [hint]="t('trainer.thumbnailHint')" />

            @if (!editingCourse()) {
              <app-select-field
                formControlName="targetAudience"
                [label]="t('trainer.audience')"
                [error]="t('error.required')"
                [options]="audienceOptions()"
              />
              <app-text-field formControlName="price" type="text" [label]="t('trainer.price')" [hint]="t('trainer.priceHint')" />
            } @else {
              <p class="text-xs text-muted">{{ t('trainer.audienceLocked') }}</p>
            }

            <div class="flex justify-end gap-2 pt-2">
              <app-button type="button" variant="secondary" compact (click)="closeCourseForm()">
                {{ t('logout-confirm.cancel') }}
              </app-button>
              <app-button type="submit" compact [loading]="savingCourse()">
                {{ t('trainer.saveCourse') }}
              </app-button>
            </div>
            @if (courseError()) {
              <p class="text-xs text-red-600">{{ courseError() }}</p>
            }
          </form>
        </section>
      </div>
    }

    @if (deletingCourse(); as course) {
      <app-confirm-modal
        [title]="t('trainer.deleteCourseTitle')"
        [text]="t('trainer.deleteCourseText')"
        [confirmLabel]="t('trainer.deleteCourse')"
        [cancelLabel]="t('logout-confirm.cancel')"
        (confirm)="confirmDelete(course.id)"
        (cancel)="deletingCourse.set(null)"
      />
    }
  `,
})
export class TrainerDashboardPage {
  private session = inject(SessionService);
  private langService = inject(LangService);
  private trainerService = inject(TrainerService);
  private router = inject(Router);
  private fb = inject(NonNullableFormBuilder);

  readonly user = this.session.user;
  readonly loading = signal(true);
  readonly errorMessage = signal('');
  readonly courses = signal<TrainerCourse[]>([]);

  readonly courseFormOpen = signal(false);
  readonly editingCourse = signal<TrainerCourse | null>(null);
  readonly savingCourse = signal(false);
  readonly courseError = signal('');

  readonly deletingCourse = signal<TrainerCourse | null>(null);

  readonly courseForm = this.fb.group({
    title: ['', [Validators.required]],
    description: [''],
    labels: [''],
    thumbnailUrl: [''],
    targetAudience: [''],
    price: [''],
  });

  constructor() {
    const u = this.user();
    if (u === null) {
      this.router.navigate(['/login']);
      return;
    }
    if (u.role !== 'TRAINER') {
      this.router.navigate(['/dashboard']);
      return;
    }
    this.loadCourses();
  }

  audienceOptions(): { value: string; label: string }[] {
    return [
      { value: 'INTERN', label: this.t('courses.audience-INTERN') },
      { value: 'OUTSIDER', label: this.t('courses.audience-OUTSIDER') },
    ];
  }

  t(key: string): string {
    return this.langService.t(key);
  }

  loadCourses(): void {
    this.trainerService.myCourses().subscribe({
      next: (courses) => {
        this.courses.set(courses);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.t('error.fallback'));
        this.loading.set(false);
      },
    });
  }

  startCreate(): void {
    this.editingCourse.set(null);
    this.courseForm.patchValue({
      title: '',
      description: '',
      labels: '',
      thumbnailUrl: '',
      targetAudience: '',
      price: '',
    });
    this.courseError.set('');
    this.courseFormOpen.set(true);
  }

  startEdit(course: TrainerCourse): void {
    this.editingCourse.set(course);
    this.courseForm.patchValue({
      title: course.title,
      description: course.description ?? '',
      labels: course.labels ?? '',
      thumbnailUrl: course.thumbnailUrl ?? '',
      targetAudience: course.targetAudience,
      price: course.price != null ? String(course.price) : '',
    });
    this.courseError.set('');
    this.courseFormOpen.set(true);
  }

  closeCourseForm(): void {
    this.courseFormOpen.set(false);
    this.courseError.set('');
  }

  submitCourseForm(): void {
    if (this.courseForm.invalid) {
      this.courseForm.markAllAsTouched();
      return;
    }

    this.savingCourse.set(true);
    this.courseError.set('');
    const values = this.courseForm.getRawValue();
    const editing = this.editingCourse();

    const body = {
      title: values.title,
      description: values.description,
      labels: values.labels,
      thumbnailUrl: values.thumbnailUrl,
      targetAudience: editing ? undefined : values.targetAudience,
      price: values.price,
    };

    const request = editing
      ? this.trainerService.updateCourse(editing.id, body)
      : this.trainerService.createCourse(body);

    request.subscribe({
      next: () => {
        this.savingCourse.set(false);
        this.courseFormOpen.set(false);
        this.loadCourses();
      },
      error: (err: { status?: number; error?: { message?: string } }) => {
        this.savingCourse.set(false);
        const msg = err?.error?.message;
        this.courseError.set(msg ?? this.t('error.fallback'));
      },
    });
  }

  askDelete(course: TrainerCourse): void {
    this.deletingCourse.set(course);
  }

  confirmDelete(courseId: number): void {
    this.deletingCourse.set(null);
    this.trainerService.deleteCourse(courseId).subscribe({
      next: () => this.loadCourses(),
      error: (err: { status?: number; error?: { message?: string } }) => {
        const msg = err?.error?.message;
        this.errorMessage.set(msg ?? this.t('error.fallback'));
      },
    });
  }
}
