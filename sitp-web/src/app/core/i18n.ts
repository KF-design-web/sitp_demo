
export type Lang = 'en' | 'fr';

export const LANG_STORAGE_KEY = 'sitp-lang';

export const i18n: Record<string, { en: string; fr: string }> = {
  'app.wordmark': { en: 'SITP', fr: 'SITP' },
  'login.heading': { en: 'Welcome back', fr: 'Bon retour' },
  'login.submit': { en: 'Sign in', fr: 'Se connecter' },
  'login.loading': { en: 'Signing in…', fr: 'Connexion…' },
  'login.no-account-yet': { en: 'No account yet?', fr: 'Pas encore de compte ?' },
  'login.create-one': { en: 'Create one', fr: 'Créez-en un' },
  'register.heading': { en: 'Create your account', fr: 'Créer votre compte' },
  'register.email': { en: 'Email', fr: 'E-mail' },
  'register.email-hint': { en: 'It is your login name.', fr: "C'est votre identifiant." },
  'register.password': { en: 'Password', fr: 'Mot de passe' },
  'register.password-hint': { en: 'At least 8 characters.', fr: 'Au moins 8 caractères.' },
  'register.account-type': { en: 'I am a…', fr: 'Je suis…' },
  'register.account-type-INTERN': { en: 'Company intern (free)', fr: 'Stagiaire interne (gratuit)' },
  'register.account-type-OUTSIDER': {
    en: 'External participant (pays per course)',
    fr: 'Participant externe (paiement par cours)',
  },
  'register.track': { en: 'Track', fr: 'Filière' },
  'register.track-ACADEMIC': { en: 'Academic', fr: 'Académique' },
  'register.track-PROFESSIONAL': { en: 'Professional', fr: 'Professionnel' },
  'register.submit': { en: 'Create account', fr: 'Créer le compte' },
  'register.loading': { en: 'Creating account…', fr: 'Création du compte…' },
  'register.have-account': { en: 'Already have an account?', fr: 'Vous avez déjà un compte ?' },
  'register.sign-in-link': { en: 'Sign in', fr: 'Se connecter' },
  'register.success-auto-login': {
    en: 'Account created — signing you in…',
    fr: 'Compte créé — connexion en cours…',
  },
  'welcome.heading': { en: 'Welcome', fr: 'Bienvenue' },
  'welcome.logged-in-as': { en: 'Logged in as', fr: 'Connecté en tant que' },
  'welcome.browse-courses': { en: 'Browse the courses', fr: 'Parcourir les cours' },
  'header.login': { en: 'Log in', fr: 'Connexion' },
  'header.logout': { en: 'Log out', fr: 'Déconnexion' },
  'header.courses': { en: 'Courses', fr: 'Cours' },
  'error.unreachable': { en: 'Cannot reach the server.', fr: 'Impossible de joindre le serveur.' },
  'error.fallback': { en: 'Something went wrong.', fr: 'Une erreur est survenue.' },
  'error.401-login': { en: 'Invalid email or password.', fr: 'E-mail ou mot de passe invalide.' },
  'error.409-email': { en: 'This email is already registered.', fr: 'Cet e-mail est déjà enregistré.' },

  'error.email-invalid': { en: 'Enter a valid email address.', fr: 'Saisissez une adresse e-mail valide.' },
  'error.required': { en: 'This field is required.', fr: 'Ce champ est requis.' },

  'courses.heading': { en: 'Our courses', fr: 'Nos cours' },
  'courses.filter-all': { en: 'All', fr: 'Tous' },
  'courses.filter-intern': { en: 'For interns', fr: 'Pour stagiaires' },
  'courses.filter-outsider': { en: 'For outsiders', fr: 'Pour externes' },
  'courses.audience-INTERN': { en: 'For interns', fr: 'Pour stagiaires' },
  'courses.audience-OUTSIDER': { en: 'For outsiders', fr: 'Pour externes' },
  'courses.view': { en: 'View course', fr: 'Voir le cours' },
  'courses.empty-title': { en: 'No courses yet', fr: 'Pas encore de cours' },
  'courses.empty-text': {
    en: 'The noticeboard is empty — check back soon.',
    fr: "Le tableau d'affichage est vide — revenez bientôt.",
  },
  'courses.retry': { en: 'Try again', fr: 'Réessayer' },
};
