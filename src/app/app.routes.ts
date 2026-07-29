import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { LoginComponent } from './features/auth/login/login.component';
import { PoliciesComponent } from './pages/policies/policies.component';
import { ThirdPartiesComponent } from './pages/third-parties/third-parties.component';
import { BillsComponent } from './pages/bills/bills.component';
import { CoveragesComponent } from './pages/coverages/coverages.component';
import { EndorsementsComponent } from './pages/endorsements/endorsements.component';
import { PolicyDetailComponent } from './pages/policies-details/policy-detail.component';
import { SubscriptionComponent } from './pages/subscription/subscription.component';
import { AddThirdPartyComponent } from './pages/add-third-party/add-third-party.component';
import { ThirdPartyDetailComponent } from './pages/third-party-detail/third-party-detail.component';
import { ProfileComponent } from './pages/profile/profile.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', component: DashboardComponent },
  { path: 'profile', component: ProfileComponent },
  { path: 'policies', redirectTo: '/?tab=policies', pathMatch: 'full' },
  { path: 'third-parties', redirectTo: '/?tab=third-parties', pathMatch: 'full' },
  { path: 'bills', redirectTo: '/?tab=bills', pathMatch: 'full' },
  { path: 'coverages', redirectTo: '/?tab=coverages', pathMatch: 'full' },
  { path: 'endorsements', redirectTo: '/?tab=endorsements', pathMatch: 'full' },
  { path: 'policies/:id', component: PolicyDetailComponent },
  { path: 'third-parties/:id', component: ThirdPartyDetailComponent },
  { path: 'subscription', component: SubscriptionComponent },
  { path: 'add-third-party', component: AddThirdPartyComponent },
];