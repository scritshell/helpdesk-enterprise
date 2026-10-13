import { bootstrapApplication } from '@angular/platform-browser';
import { Component } from '@angular/core';

@Component({ selector: 'app-root', standalone: true, template: '<main class="container py-5"><h1>HelpDesk empresarial</h1><p>Base del frontend preparada para el módulo de autenticación.</p></main>' })
class AppComponent {}

bootstrapApplication(AppComponent).catch(console.error);
