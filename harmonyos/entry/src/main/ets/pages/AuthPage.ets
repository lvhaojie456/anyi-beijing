import { COLORS } from '../common/Constants';
import { StatusBanner } from '../components/Common';
import { AppUser, appUserFromAuth } from '../models/Models';
import { AnyiApiClient, userFriendlyError } from '../services/ApiClient';

@Component
export struct AuthPage {
  onSignedIn: (user: AppUser) => void = () => {};

  @State private isRegister: boolean = false;
  @State private username: string = '';
  @State private password: string = '';
  @State private message: string = '';
  @State private serverMessage: string = '正在检查云端服务…';
  @State private loading: boolean = false;

  private api: AnyiApiClient = new AnyiApiClient();

  aboutToAppear(): void {
    this.checkServer();
  }

  private async checkServer(): Promise<void> {
    try {
      await this.api.health();
      this.serverMessage = '云端服务正常，可以登录或注册';
    } catch (error) {
      this.serverMessage = userFriendlyError(error as Error, '云端服务暂时不可用');
    }
  }

  private async submit(): Promise<void> {
    const username: string = this.username.trim().toLowerCase();
    if (username.length === 0 || this.password.length === 0) {
      this.message = '请输入账号和密码';
      return;
    }
    if (this.isRegister && this.password.length < 8) {
      this.message = '密码至少需要 8 位';
      return;
    }
    this.loading = true;
    this.message = '';
    try {
      const response = this.isRegister
        ? await this.api.register(username, this.password, username)
        : await this.api.login(username, this.password);
      this.onSignedIn(appUserFromAuth(response));
    } catch (error) {
      this.message = userFriendlyError(error as Error, this.isRegister ? '注册失败' : '登录失败');
    } finally {
      this.loading = false;
    }
  }

  @Builder
  private modeButton(text: string, selected: boolean, registerMode: boolean) {
    Button(text)
      .layoutWeight(1)
      .height(48)
      .fontSize(16)
      .fontWeight(selected ? FontWeight.Bold : FontWeight.Medium)
      .fontColor(selected ? COLORS.ink : COLORS.muted)
      .backgroundColor(selected ? COLORS.white : '#00FFFFFF')
      .borderRadius(12)
      .onClick(() => {
        this.isRegister = registerMode;
        this.message = '';
      })
  }

  build() {
    Stack({ alignContent: Alignment.Top }) {
      Column() {
        Image($r('app.media.anyi_launch_clouds'))
          .width('100%')
          .height(330)
          .objectFit(ImageFit.Cover)
          .opacity(0.42)
      }
      .width('100%')
      .height('100%')
      .backgroundColor(COLORS.background)

      Scroll() {
        Column({ space: 14 }) {
          Column({ space: 8 }) {
            Stack({ alignContent: Alignment.Center }) {
              Text('♥')
                .fontSize(34)
                .fontColor(COLORS.white)
            }
            .width(76)
            .height(76)
            .borderRadius(22)
            .backgroundColor(COLORS.green)
            .shadow({ radius: 18, color: '#3325231F', offsetY: 8 })

            Text('安忆')
              .fontSize(36)
              .fontWeight(FontWeight.Bold)
              .fontColor(COLORS.ink)
            Text('把想念安放在云端')
              .fontSize(15)
              .fontColor(COLORS.muted)
          }
          .width('100%')
          .margin({ top: 46, bottom: 8 })

          Column({ space: 14 }) {
            Row() {
              this.modeButton('登录', !this.isRegister, false)
              this.modeButton('注册', this.isRegister, true)
            }
            .width('100%')
            .padding(4)
            .backgroundColor(COLORS.amberLight)
            .borderRadius(14)

            Text(this.isRegister ? '创建云端账号' : '使用账号密码登录')
              .width('100%')
              .fontSize(13)
              .fontColor(COLORS.muted)

            TextInput({ placeholder: '账号', text: this.username })
              .height(54)
              .fontSize(16)
              .fontColor(COLORS.ink)
              .backgroundColor(COLORS.white)
              .borderRadius(12)
              .border({ width: 1, color: COLORS.line })
              .padding({ left: 16, right: 16 })
              .onChange((value: string) => {
                this.username = value.replace(/\s/g, '');
              })

            TextInput({ placeholder: '密码', text: this.password })
              .type(InputType.Password)
              .height(54)
              .fontSize(16)
              .fontColor(COLORS.ink)
              .backgroundColor(COLORS.white)
              .borderRadius(12)
              .border({ width: 1, color: COLORS.line })
              .padding({ left: 16, right: 16 })
              .onChange((value: string) => {
                this.password = value;
              })

            StatusBanner({ message: this.message })

            Button(this.loading ? '正在连接云端…' : (this.isRegister ? '注册并进入' : '登录'))
              .width('100%')
              .height(54)
              .fontSize(17)
              .fontWeight(FontWeight.Bold)
              .fontColor(COLORS.white)
              .backgroundColor(this.loading ? '#8FA79D' : COLORS.night)
              .borderRadius(12)
              .enabled(!this.loading)
              .onClick(() => this.submit())
          }
          .width('100%')
          .padding(18)
          .backgroundColor('#F7FFFFFF')
          .borderRadius(18)
          .border({ width: 1, color: COLORS.line })
          .shadow({ radius: 16, color: '#1A25231F', offsetY: 6 })

          Row({ space: 8 }) {
            ForEach(['云端纪念', '数字陪伴', '人文社区'], (item: string) => {
              Text(item)
                .fontSize(12)
                .fontWeight(FontWeight.Medium)
                .fontColor(COLORS.muted)
                .padding({ left: 10, right: 10, top: 7, bottom: 7 })
                .backgroundColor('#EFFFFFFF')
                .borderRadius(16)
            })
          }

          StatusBanner({ message: this.serverMessage })
        }
        .width('100%')
        .constraintSize({ maxWidth: 560 })
        .padding({ left: 20, right: 20, bottom: 30 })
      }
      .width('100%')
      .height('100%')
      .scrollBar(BarState.Off)
    }
    .width('100%')
    .height('100%')
  }
}
