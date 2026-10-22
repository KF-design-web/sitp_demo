import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LangService } from '../../core/lang.service';
import { TrainerService } from '../../trainer/trainer.service';
import { TrainerCourseDetail, TrainerChapter } from '../../trainer/trainer.models';
import { Button } from '../../ui/button/button';
import { ErrorSlip } from '../../ui/error-slip/error-slip';
import { TextField } from '../../ui/text-field/text-field';
import { ConfirmModal } from '../../ui/confirm-modal/confirm-modal';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-trainer-course-page',
  imports: [RouterLink, Button, ErrorSlip, TextField, ConfirmModal, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto mt-8 max-w-6xl px-4">
      <a routerLink="/trainer" class="text-sm font-medium text-primary hover:underline">
        ← {{ t('trainer.backToStudio') }}
      </a>

      <div class="mt-4 rounded-xl border border-line bg-surface shadow-card overflow-hidden">
        <div class="flex flex-col sm:flex-row">
          <div class="h-40 bg-primary/5 sm:h-auto sm:w-72 shrink-0">
            @if (course()?.thumbnailUrl) {
              <img [src]="course()!.thumbnailUrl" alt="" class="h-full w-full object-cover"
                   (error)="$any($event.target).style.display = 'none'" />
            }
          </div>
          <div class="flex-1 p-5">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <h1 class="text-xl font-bold text-ink">{{ course()?.title }}</h1>
              <span class="rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
                {{ course()?.targetAudience === 'INTERN' ? t('courses.audience-INTERN') : t('courses.audience-OUTSIDER') }}
              </span>
            </div>
            <p class="mt-1 text-sm text-muted">{{ course()?.description }}</p>
            @if (labelsLine() !== '') {
              <div class="mt-2 flex flex-wrap gap-1">
                @for (label of labelsLine().split(','); track label) {
                  <span class="rounded-full bg-bg px-2 py-0.5 text-[11px] text-muted">{{ label.trim() }}</span>
                }
              </div>
            }
            <app-button variant="secondary" compact class="mt-3" (click)="startCourseEdit()">
              {{ t('trainer.editCourse') }}
            </app-button>
          </div>
        </div>
      </div>

      <div class="mt-6 flex items-center justify-between">
        <h2 class="text-lg font-bold text-ink">{{ t('trainer.library') }}</h2>
        <app-button variant="primary" compact (click)="startAddEpisode()">
          + {{ t('trainer.addEpisode') }}
        </app-button>
      </div>

      <div class="mt-4">
        <app-error-slip [message]="errorMessage()" />
      </div>

      @if (loading()) {
        <p class="mt-8 text-sm text-muted">{{ t('trainer.loading') }}</p>
      } @else if (episodes().length === 0) {
        <div class="mt-6 rounded-xl border border-dashed border-line bg-surface p-8 text-center">
          <p class="text-base font-medium text-ink">{{ t('trainer.noEpisodes') }}</p>
          <p class="mt-1 text-sm text-muted">{{ t('trainer.noEpisodesText') }}</p>
        </div>
      } @else {
        <div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
          @for (episode of episodes(); track episode.id) {
            <div class="rounded-xl border border-line bg-surface shadow-card overflow-hidden">
              <div class="flex h-32 items-center justify-center bg-ink/80 text-2xl font-bold text-surface">
                {{ episode.position }}
              </div>
              @if (episode.thumbnailUrl) {
                <img [src]="episode.thumbnailUrl" alt="" class="h-32 w-full object-cover -mt-32"
                     (error)="$any($event.target).style.display = 'none'" />
              }
              <div class="p-4">
                <h3 class="text-sm font-semibold text-ink leading-snug">{{ episode.title }}</h3>
                @if (episode.labels) {
                  <div class="mt-2 flex flex-wrap gap-1">
                    @for (label of episode.labels.split(','); track label) {
                      <span class="rounded-full bg-bg px-2 py-0.5 text-[11px] text-muted">{{ label.trim() }}</span>
                    }
                  </div>
                }
                <p class="mt-2 line-clamp-2 text-xs text-muted">{{ episode.description }}</p>
                <p class="mt-1 truncate text-[11px] text-muted">{{ episode.videoUrl }}</p>
                <div class="mt-3 flex flex-wrap gap-2">
                  <app-button variant="secondary" compact (click)="startEpisodeEdit(episode)">
                    {{ t('trainer.editEpisode') }}
                  </app-button>
                  <app-button variant="secondary" compact (click)="deletingEpisode.set(episode)">
                    {{ t('trainer.deleteEpisode') }}
                  </app-button>
                </div>
              </div>
            </div>
          }
        </div>
      }
    </div>

    @if (episodeFormOpen()) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-ink/40 p-4" (click)="closeEpisodeForm()">
        <section
          role="dialog"
          aria-modal="true"
          class="max-h-[90vh] w-full max-w-md overflow-y-auto rounded-xl border border-line bg-surface p-6 shadow-card"
          (click)="$event.stopPropagation()"
        >
          <h2 class="text-lg font-bold text-ink">
            {{ editingEpisode() ? t('trainer.editEpisodeTitle') : t('trainer.addEpisodeTitle') }}
          </h2>

          <form [formGroup]="episodeForm" (ngSubmit)="submitEpisodeForm()" class="mt-4 space-y-3">
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
            <app-text-field formControlName="videoUrl" type="text" [label]="t('trainer.videoUrl')" [hint]="t('trainer.videoUrlHint')" />
            <app-text-field formControlName="thumbnailUrl" type="text" [label]="t('trainer.thumbnail')" [hint]="t('trainer.thumbnailHint')" />
            @if (editingEpisode()) {
              <app-text-field formControlName="position" type="text" [label]="t('trainer.position')" [hint]="t('trainer.positionHint')" />
            }

            <div class="flex justify-end gap-2 pt-2">
              <app-button type="button" variant="secondary" compact (click)="closeEpisodeForm()">
                {{ t('logout-confirm.cancel') }}
              </app-button>
              <app-button type="submit" compact [loading]="savingEpisode()">
                {{ t('trainer.saveEpisode') }}
              </app-button>
            </div>
            @if (episodeError()) {
              <p class="text-xs text-red-600">{{ episodeError() }}</p>
            }
          </form>
        </section>
      </div>
    }

    @if (deletingEpisode(); as episode) {
      <app-confirm-modal
        [title]="t('trainer.deleteEpisodeTitle')"
        [text]="t('trainer.deleteEpisodeText')"
        [confirmLabel]="t('trainer.deleteEpisode')"
        [cancelLabel]="t('logout-confirm.cancel')"
        (confirm)="confirmDeleteEpisode(episode.id)"
        (cancel)="deletingEpisode.set(null)"
      />
    }

    @if (courseFormOpen()) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-ink/40 p-4" (click)="closeCourseForm()">
        <section
          role="dialog"
          aria-modal="true"
          class="max-h-[90vh] w-full max-w-md overflow-y-auto rounded-xl border border-line bg-surface p-6 shadow-card"
          (click)="$event.stopPropagation()"
        >
          <h2 class="text-lg font-bold text-ink">{{ t('trainer.editCourseTitle') }}</h2>
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
            <p class="text-xs text-muted">{{ t('trainer.audienceLocked') }}</p>
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
  `,
})
export class TrainerCoursePage {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private langService = inject(LangService);
  private trainerService = inject(TrainerService);
  private fb = inject(NonNullableFormBuilder);

  readonly courseId = Number(this.route.snapshot.paramMap.get('id') ?? 0);

  readonly loading = signal(true);
  readonly errorMessage = signal('');
  readonly course = signal<TrainerCourseDetail | null>(null);
  readonly episodes = signal<TrainerChapter[]>([]);

  readonly courseFormOpen = signal(false);
  readonly savingCourse = signal(false);
  readonly courseError = signal('');

  readonly episodeFormOpen = signal(false);
  readonly editingEpisode = signal<TrainerChapter | null>(null);
  readonly savingEpisode = signal(false);
  readonly episodeError = signal('');

  readonly deletingEpisode = signal<TrainerChapter | null>(null);

  readonly courseForm = this.fb.group({
    title: ['', Validators.required],
    description: [''],
    labels: [''],
    thumbnailUrl: [''],
  });

  readonly episodeForm = this.fb.group({
    title: ['', Validators.required],
    description: [''],
    labels: [''],
    videoUrl: [''],
    thumbnailUrl: [''],
    position: [''],
  });

  constructor() {
    if (!Number.isFinite(this.courseId) || this.courseId <= 0) {
      this.router.navigate(['/trainer']);
      return;
    }
    this.loadAll();
  }

  t(key: string): string {
    return this.langService.t(key);
  }

  labelsLine(): string {
    return this.course()?.labels ?? '';
  }

  loadAll(): void {
    this.loading.set(true);
    this.trainerService.courseDetail(this.courseId).subscribe({
      next: (course) => {
        this.course.set(course);
        this.episodes.set(course.chapters ?? []);
        this.loading.set(false);
      },
      error: (err: { status?: number }) => {
        if (err?.status === 403 || err?.status === 404) {
          this.router.navigate(['/trainer']);
          return;
        }
        this.errorMessage.set(this.t('error.fallback'));
        this.loading.set(false);
      },
    });
  }

  startCourseEdit(): void {
    const course = this.course();
    if (!course) return;
    this.courseForm.patchValue({
      title: course.title,
      description: course.description ?? '',
      labels: course.labels ?? '',
      thumbnailUrl: course.thumbnailUrl ?? '',
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
    const { title, description, labels, thumbnailUrl } = this.courseForm.getRawValue();

    this.trainerService.updateCourse(this.courseId, {
      title, description, labels, thumbnailUrl,
    }).subscribe({
      next: () => {
        this.savingCourse.set(false);
        this.courseFormOpen.set(false);
        this.loadAll();
      },
      error: (err: { error?: { message?: string } }) => {
        this.savingCourse.set(false);
        this.courseError.set(err?.error?.message ?? this.t('error.fallback'));
      },
    });
  }

  startAddEpisode(): void {
    this.editingEpisode.set(null);
    this.episodeForm.patchValue({
      title: '',
      description: '',
      labels: '',
      videoUrl: '',
      thumbnailUrl: '',
      position: '',
    });
    this.episodeError.set('');
    this.episodeFormOpen.set(true);
  }

  startEpisodeEdit(episode: TrainerChapter): void {
    this.editingEpisode.set(episode);
    this.episodeForm.patchValue({
      title: episode.title,
      description: episode.description ?? '',
      labels: episode.labels ?? '',
      videoUrl: episode.videoUrl ?? '',
      thumbnailUrl: episode.thumbnailUrl ?? '',
      position: String(episode.position),
    });
    this.episodeError.set('');
    this.episodeFormOpen.set(true);
  }

  closeEpisodeForm(): void {
    this.episodeFormOpen.set(false);
    this.episodeError.set('');
  }

  submitEpisodeForm(): void {
    if (this.episodeForm.invalid) {
      this.episodeForm.markAllAsTouched();
      return;
    }

    this.savingEpisode.set(true);
    this.episodeError.set('');
    const values = this.episodeForm.getRawValue();
    const editing = this.editingEpisode();

    const request = editing
      ? this.trainerService.updateEpisode(editing.id, values)
      : this.trainerService.addEpisode(this.courseId, values);

    request.subscribe({
      next: () => {
        this.savingEpisode.set(false);
        this.episodeFormOpen.set(false);
        this.loadAll();
      },
      error: (err: { error?: { message?: string } }) => {
        this.savingEpisode.set(false);
        this.episodeError.set(err?.error?.message ?? this.t('error.fallback'));
      },
    });
  }

  confirmDeleteEpisode(episodeId: number): void {
    this.deletingEpisode.set(null);
    this.trainerService.deleteEpisode(episodeId).subscribe({
      next: () => this.loadAll(),
      error: (err: { error?: { message?: string } }) => {
        this.errorMessage.set(err?.error?.message ?? this.t('error.fallback'));
      },
    });
  }
}
