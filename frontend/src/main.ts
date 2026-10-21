import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAppInitializer } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';

import { AppComponent } from './app.component';
import { initializeAuthSession } from './app/core/auth/auth.initializer';
import { authInterceptor } from './app/core/auth/auth.interceptor';
import { appRoutes } from './app/app.routes';

bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(appRoutes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAppInitializer(initializeAuthSession)
  ]
}).catch(console.error);
