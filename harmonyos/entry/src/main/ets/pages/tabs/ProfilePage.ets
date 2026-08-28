import { common } from '@kit.AbilityKit';
import {
  absoluteAssetUrl,
  ADMIN_URL,
  COLORS,
  LEGAL_AI_URL,
  LEGAL_PRIVACY_URL,
  LEGAL_TERMS_URL
} from '../../common/Constants';
import { StatusBanner, UserAvatar } from '../../components/Common';
import { AppUser, appUserFromProfile } from '../../models/Models';
import { AnyiApiClient, userFriendlyError } from '../../services/ApiClient';
import { pickSingleImage } from '../../services/MediaPicker';

@Component
export struct ProfilePage {
  user: AppUser = new AppUser('', '', '', 'user', '');
  onUserChanged: (user: AppUser) => void = () => {};
  onLogout: () => void = () => {};

  @State private displayName: string = '';
  @State private selectedAvatarUri: string = '';
  @State private loading: boolean = false;
  @State private message: string = '';
  @State private confirmDelete: boolean = false;

  aboutToAppear(): void {
    this.displayName = this.user.displayName;
  }

  private api(): AnyiApiClient {
    return new AnyiApiClient(() => this.user.token);
  }

  private async chooseAvatar(): Promise<void> {
    try {
      const uri: string = await pickSingleImage();
      if (uri.length > 0) {
        this.selectedAvatarUri = uri;
      }
    } catch (error) {
      this.message = userFriendlyError(error as Error, '没有选中头像');
    }
  }

  private async saveProfile(): Promise<void> {
    const name: string = this.displayName.trim();
    if (name.length === 0) {
      this.message = '昵称不能为空';
      return;
    }
    this.loading = true;
    this.message = '';
    try {
      let avatarUrl: string = this.user.avatarUrl;
      if (this.selectedAvatarUri.length > 0) {
        const upload = await this.api().uploadImage('profile/avatar', this.selectedAvatarUri);
        avatarUrl = upload.asset.url;
      }
      const response = await this.api().updateCurrentUser(name, avatarUrl);
      const updated: AppUser = appUserFromProfile(response, this.user.token);
      this.selectedAvatarUri = '';
      this.onUserChanged(updated);
      this.message = '个人资料已同步到云端';
    } catch (error) {
      this.message = userFriendlyError(error as Error, '资料保存失败');
    } finally {
      this.loading = false;
    }
  }

  private async openUrl(url: string): Promise<void> {
    try {
      const context: common.UIAbilityContext = this.getUIContext().getHostContext() as common.UIAbilityContext;
      await context.openLink(url);
    } catch (_) {
      this.message = '没有找到可以打开此链接的应用';
    }
  }

  private async deleteAccount(): Promise<void> {
    this.loading = true;
    try {
      await this.api().deleteAccount();
      this.onLogout();
    } catch (error) {
      this.message = userFriendlyError(error as Error, '账号注销失败');
      this.confirmDelete = false;
      this.loading = false;
    }
  }

  @Builder
  private linkRow(icon: string, title: string, subtitle: string, url: string) {
    Row({ space: 12 }) {
      Stack({ alignContent: Alignment.Center }) {
        Text(icon)
          .fontSize(17)
          .fontColor(COLORS.green)
      }
      .width(36)
      .height(36)
      .borderRadius(12)
      .backgroundColor(COLORS.greenLight)
      Column({ space: 2 }) {
        Text(title)
          .fontSize(14)
          .fontWeight(FontWeight.Bold)
          .fontColor(COLORS.ink)
        Text(subtitle)
          .fontSize(11)
          .fontColor(COLORS.muted)
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)
      Text('›')
        .fontSize(24)
        .fontColor(COLORS.muted)
    }
    .width('100%')
    .height(62)
    .padding({ left: 12, right: 12 })
    .backgroundColor(COLORS.paper)
    .borderRadius(14)
    .border({ width: 1, color: COLORS.line })
    .onClick(() => this.openUrl(url))
  }

  build() {
    Stack({ alignContent: Alignment.Top }) {
      Image($r('app.media.anyi_profile_page_bg'))
        .width('100%')
        .height('100%')
        .objectFit(ImageFit.Cover)
        .opacity(0.28)

      Scroll() {
        Column({ space: 14 }) {
          Column({ space: 4 }) {
            Text('个人设置')
              .width('100%')
              .fontSize(27)
              .fontWeight(FontWeight.Bold)
              .fontColor(COLORS.ink)
            Text('管理资料、合规信息和账号安全')
              .width('100%')
              .fontSize(13)
              .fontColor(COLORS.muted)
          }

          StatusBanner({ message: this.message })

          Column({ space: 14 }) {
            Row({ space: 14 }) {
              if (this.selectedAvatarUri.length > 0) {
                UserAvatar({ imageUrl: this.selectedAvatarUri, fallback: this.displayName, size: 82 })
              } else {
                UserAvatar({
                  imageUrl: this.user.avatarUrl.length > 0 ? absoluteAssetUrl(this.user.avatarUrl) : '',
                  fallback: this.displayName,
                  size: 82
                })
              }
              Column({ space: 5 }) {
                Text(this.user.displayName)
                  .fontSize(20)
                  .fontWeight(FontWeight.Bold)
                  .fontColor(COLORS.ink)
                Text(`@${this.user.username}`)
                  .fontSize(12)
                  .fontColor(COLORS.muted)
                Text(this.user.role === 'admin' ? '管理员账号' : '云端账号')
                  .fontSize(11)
                  .fontColor(this.user.role === 'admin' ? '#72551E' : COLORS.green)
                  .padding({ left: 9, right: 9, top: 4, bottom: 4 })
                  .backgroundColor(this.user.role === 'admin' ? COLORS.amberLight : COLORS.greenLight)
                  .borderRadius(12)
              }
              .alignItems(HorizontalAlign.Start)
              .layoutWeight(1)
            }
            .width('100%')

            Button('从图库选择头像')
              .width('100%')
              .height(42)
              .fontSize(13)
              .fontColor(COLORS.green)
              .backgroundColor(COLORS.greenLight)
              .onClick(() => this.chooseAvatar())

            TextInput({ placeholder: '昵称', text: this.displayName })
              .width('100%')
              .height(50)
              .fontSize(15)
              .backgroundColor(COLORS.white)
              .borderRadius(12)
              .border({ width: 1, color: COLORS.line })
              .padding({ left: 13, right: 13 })
              .onChange((value: string) => this.displayName = value)

            Button(this.loading ? '正在保存…' : '保存个人资料')
              .width('100%')
              .height(48)
              .fontSize(15)
              .fontWeight(FontWeight.Bold)
              .fontColor(COLORS.white)
              .backgroundColor(this.loading ? '#8FA79D' : COLORS.night)
              .enabled(!this.loading)
              .onClick(() => this.saveProfile())
          }
          .width('100%')
          .padding(16)
          .backgroundColor('#F5FFFEFA')
          .borderRadius(18)
          .border({ width: 1, color: COLORS.line })

          Column({ space: 9 }) {
            Text('协议与服务')
              .width('100%')
              .fontSize(18)
              .fontWeight(FontWeight.Bold)
              .fontColor(COLORS.ink)
            this.linkRow('隐', '隐私政策', '了解数据如何被使用与保护', LEGAL_PRIVACY_URL)
            this.linkRow('约', '用户协议', '查看账号与服务使用条款', LEGAL_TERMS_URL)
            this.linkRow('AI', 'AI 陪伴说明', 'AI 不是真实逝者，也不替代专业帮助', LEGAL_AI_URL)
            if (this.user.role === 'admin') {
              this.linkRow('管', 'Web 管理后台', '订单、审核与运营管理', ADMIN_URL)
            }
          }
          .width('100%')

          Column({ space: 10 }) {
            Text('HarmonyOS 原生版')
              .width('100%')
              .fontSize(14)
              .fontWeight(FontWeight.Bold)
              .fontColor(COLORS.ink)
            Text('ArkTS + ArkUI · HarmonyOS 6.0 / API 20\n与 Android 客户端共用安忆云端数据')
              .width('100%')
              .fontSize(12)
              .lineHeight(19)
              .fontColor(COLORS.muted)
          }
          .width('100%')
          .padding(14)
          .backgroundColor(COLORS.greenLight)
          .borderRadius(14)

          Button('退出登录')
            .width('100%')
            .height(48)
            .fontSize(14)
            .fontColor(COLORS.green)
            .backgroundColor(COLORS.paper)
            .border({ width: 1, color: COLORS.green })
            .onClick(() => this.onLogout())

          if (!this.confirmDelete) {
            Button('注销账号')
              .width('100%')
              .height(44)
              .fontSize(13)
              .fontColor(COLORS.danger)
              .backgroundColor(COLORS.dangerLight)
              .onClick(() => this.confirmDelete = true)
          } else {
            Column({ space: 10 }) {
              Text('确认注销账号？')
                .width('100%')
                .fontSize(16)
                .fontWeight(FontWeight.Bold)
                .fontColor(COLORS.danger)
              Text('账号会被停用，相关上传素材会进入删除队列。此操作不可在客户端撤销。')
                .width('100%')
                .fontSize(12)
                .lineHeight(18)
                .fontColor(COLORS.muted)
              Row({ space: 10 }) {
                Button('取消')
                  .layoutWeight(1)
                  .height(42)
                  .fontColor(COLORS.muted)
                  .backgroundColor('#F0EEE8')
                  .onClick(() => this.confirmDelete = false)
                Button(this.loading ? '正在注销…' : '确认注销')
                  .layoutWeight(1)
                  .height(42)
                  .fontColor(COLORS.white)
                  .backgroundColor(COLORS.danger)
                  .enabled(!this.loading)
                  .onClick(() => this.deleteAccount())
              }
              .width('100%')
            }
            .width('100%')
            .padding(14)
            .backgroundColor(COLORS.dangerLight)
            .borderRadius(14)
            .border({ width: 1, color: '#40B42318' })
          }
        }
        .width('100%')
        .constraintSize({ maxWidth: 760 })
        .padding({ left: 16, right: 16, top: 18, bottom: 28 })
      }
      .width('100%')
      .height('100%')
      .scrollBar(BarState.Off)
    }
    .width('100%')
    .height('100%')
    .backgroundColor(COLORS.background)
  }
}
