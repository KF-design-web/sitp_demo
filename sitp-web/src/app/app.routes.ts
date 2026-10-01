import { Routes } from '@angular/router';
import { LoginPage } from './pages/login/login';
import { CatalogPage } from './pages/catalog/catalog';
import { RegisterPage } from './pages/register/register';
import { WelcomePage } from './pages/welcome/welcome';
import { ForgotPasswordPage } from './pages/forgot-password/forgot-password';
import { VerifyEmailPage } from './pages/verify-email/verify-email';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginPage, title: 'SITP — Sign in' },
  { path: 'register', component: RegisterPage, title: 'SITP — Create account' },
  { path: 'verify-email', component: VerifyEmailPage, title: 'SITP — Verify your email' },
  { path: 'forgot-password', component: ForgotPasswordPage, title: 'SITP — Reset password' },
  { path: 'welcome', component: WelcomePage, title: 'SITP — Welcome' },
  { path: 'courses', component: CatalogPage, title: 'SITP — Courses' },
];
