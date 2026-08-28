import { common } from '@kit.AbilityKit';
import { COLORS } from '../common/Constants';
import { AppUser, appUserFromProfile } from '../models/Models';
import { AnyiApiClient, ApiError } from '../services/ApiClient';
import { SessionStore } from '../services/SessionStore';
import { AuthPage } from './AuthPage';
import { CompanionPage } from './tabs/CompanionPage';
import { CommunityPage } from './tabs/CommunityPage';
import { HallPage } from './tabs/HallPage';
import { ProfilePage } from './tabs/ProfilePage';

interface TabItem {
  title: string;
  icon: string;
}

@Entry
@Component
struct Index {
  @State private ready: boolean = false;
  @State private currentUser: AppUser | undefined = undefined;
  @State private activeTab: number = 0;

  private store: SessionStore | undefined = undefined;
  private tabs: Array<TabItem> = [
    { title: '纪念馆', icon: '云' },
    { title: 'AI陪伴', icon: '伴' },
    { title: '社区', icon: '社' },
    { title: '我的', icon: '我' }
  ];

  aboutToAppear(): void {
    const context: common.UIAbilityContext = this.getUIContext().getHostContext() as common.UIAbilityContext;
    this.store = new SessionStore(context);
    this.currentUser = this.store.read();
    this.ready = true;
    this.verifySession();
  }

  private async verifySession(): Promise<void> {
    const user: AppUser | undefined = this.currentUser;
    if (user === undefined) {
      return;
    }
    const api: AnyiApiClient = new AnyiApiClient(() => user.token);
    try {
      const profile = await api.currentUser();
      const updated: AppUser = appUserFromProfile(profile, user.token);
      this.currentUser = updated;
      this.store?.save(updated);
    } catch (error) {
      const apiError: ApiError = error as ApiError;
      if (apiError.statusCode === 401) {
        this.logout();
      }
    }
  }

  private signedIn(user: AppUser): void {
    this.store?.save(user);
    this.currentUser = user;
    this.activeTab = 0;
  }

  private userChanged(user: AppUser): void {
    this.store?.save(user);
    this.currentUser = user;
  }

  private logout(): void {
    this.store?.clear();
    this.currentUser = undefined;
    this.activeTab = 0;
  }

  @Builder
  private tabButton(item: TabItem, index: number) {
    Column({ space: 3 }) {
      Stack({ alignContent: Alignment.Center }) {
        Text(item.icon)
          .fontSize(14)
          .fontWeight(FontWeight.Bold)
          .fontColor(this.activeTab === index ? COLORS.white : COLORS.muted)
      }
      .width(30)
      .height(26)
      .borderRadius(13)
      .backgroundColor(this.activeTab === index ? COLORS.green : '#00FFFFFF')

      Text(item.title)
        .fontSize(11)
        .fontWeight(this.activeTab === index ? FontWeight.Bold : FontWeight.Regular)
        .fontColor(this.activeTab === index ? COLORS.green : COLORS.muted)
    }
    .layoutWeight(1)
    .height(58)
    .justifyContent(FlexAlign.Center)
    .onClick(() => {
      this.activeTab = index;
    })
  }

  build() {
    if (!this.ready) {
      Stack({ alignContent: Alignment.Center }) {
        LoadingProgress()
          .width(36)
          .height(36)
          .color(COLORS.green)
      }
      .width('100%')
      .height('100%')
      .backgroundColor(COLORS.background)
    } else if (this.currentUser === undefined) {
      AuthPage({
        onSignedIn: (user: AppUser) => this.signedIn(user)
      })
    } else {
      Column() {
        Stack() {
          if (this.activeTab === 0) {
            HallPage({ user: this.currentUser as AppUser })
          } else if (this.activeTab === 1) {
            CompanionPage({ user: this.currentUser as AppUser })
          } else if (this.activeTab === 2) {
            CommunityPage({ user: this.currentUser as AppUser })
          } else {
            ProfilePage({
              user: this.currentUser as AppUser,
              onUserChanged: (user: AppUser) => this.userChanged(user),
              onLogout: () => this.logout()
            })
          }
        }
        .width('100%')
        .layoutWeight(1)

        Row() {
          ForEach(this.tabs, (item: TabItem, index: number) => {
            this.tabButton(item, index)
          })
        }
        .width('100%')
        .height(66)
        .padding({ left: 8, right: 8, bottom: 6 })
        .backgroundColor('#FAFFFEFA')
        .border({ width: { top: 1 }, color: COLORS.line })
      }
      .width('100%')
      .height('100%')
      .backgroundColor(COLORS.background)
    }
  }
}
