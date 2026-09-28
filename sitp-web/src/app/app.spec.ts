import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import {
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { App } from './app';
import { authInterceptor } from './core/auth-interceptor';

describe('App (the real shell)', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('renders the real shell: wordmark, main + outlet, honestly logged out', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;

    const wordmark = compiled.querySelector('header a');
    expect(wordmark).toBeTruthy();
    expect(wordmark?.textContent?.trim()).toBe('SITP');

    expect(compiled.querySelector('main')).toBeTruthy();
    expect(compiled.querySelector('router-outlet')).toBeTruthy();

    const buttonTexts = Array.from(
      compiled.querySelectorAll('button'),
      (b) => b.textContent?.trim() ?? ''
    );
    expect(buttonTexts).toContain('Log in');
    expect(buttonTexts).not.toContain('Log out');

    expect(buttonTexts).toContain('EN');
    expect(buttonTexts).toContain('FR');
  });
});
