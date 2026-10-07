
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule, GraduationCap, Briefcase, Play } from 'lucide-angular';
import { CourseSummary, courseThumbnailUrl } from '../../courses/courses.models';
import { Button } from '../button/button';
import { LangService } from '../../core/lang.service';

@Component({
  selector: 'app-course-card',
  imports: [LucideAngularModule, RouterLink, Button],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article
      class="flex flex-col overflow-hidden rounded-xl border border-line bg-surface shadow-card"
    >
      @if (!thumbFailed()) {
        <img
          [src]="courseThumbnailUrl(course().id, course().title)"
          [alt]="tText(course().title)"
          class="aspect-video w-full object-cover"
          (error)="thumbFailed.set(true)"
        />
      } @else {
        <div class="grid aspect-video w-full place-items-center bg-gradient-to-br from-primary-soft to-bg">
          <lucide-icon [img]="play" [size]="32" class="text-muted" />
        </div>
      }

      <div class="flex flex-1 flex-col gap-3 p-6">
        <h3 class="text-lg font-semibold text-ink">{{ tText(course().title) }}</h3>

        <p class="whitespace-pre-line text-sm text-muted">{{ tText(course().description) }}</p>

        <span
          class="inline-flex items-center gap-1 self-start rounded-full px-2.5 py-0.5 text-xs font-medium"
          [class]="chip().classes"
        >
          <lucide-icon [img]="chip().icon" [size]="16" />
          {{ t('courses.audience-' + course().targetAudience) }}
        </span>
      </div>

      <div class="px-6 pb-6">
        <app-button variant="secondary" [routerLink]="['/courses', course().id]">
          {{ t('courses.view') }}
        </app-button>
      </div>
    </article>
  `,
})
export class CourseCard {
  private langService = inject(LangService);

  readonly courseThumbnailUrl = courseThumbnailUrl;

  readonly course = input.required<CourseSummary>();

  readonly thumbFailed = signal(false);

  readonly play = Play;
  readonly graduationCap = GraduationCap;
  readonly briefcase = Briefcase;

  readonly chip = computed(() =>
    this.course().targetAudience === 'INTERN'
      ? {
          icon: this.graduationCap,
          classes: 'bg-primary-soft text-primary border border-primary-soft',
        }
      : {
          icon: this.briefcase,
          classes: 'bg-bg text-muted border border-line',
        },
  );

  t(key: string): string {
    return this.langService.t(key);
  }

  tText(en: string): string {
    return this.langService.tText(en);
  }
}
