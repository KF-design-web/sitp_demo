import { Routes } from '@angular/router';
import { LoginPage } from './pages/login/login';
import { CatalogPage } from './pages/catalog/catalog';
import { RegisterPage } from './pages/register/register';
import { WelcomePage } from './pages/welcome/welcome';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginPage, title: 'SITP — Sign in' },
  { path: 'register', component: RegisterPage, title: 'SITP — Create account' },
  { path: 'welcome', component: WelcomePage, title: 'SITP — Welcome' },
  { path: 'courses', component: CatalogPage, title: 'SITP — Courses' },
];
