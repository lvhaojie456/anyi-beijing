import { absoluteAssetUrl, COLORS } from '../../common/Constants';
import { EmptyState, LoadingState, StatusBanner } from '../../components/Common';
import { AppUser, Memorial } from '../../models/Models';
import { AnyiApiClient, userFriendlyError } from '../../services/ApiClient';
import { pickSingleImage } from '../../services/MediaPicker';

@Component
export struct HallPage {
  user: AppUser = new AppUser('', '', '', 'user', '');

  @State private memorial: Memorial | undefined = undefined;
  @State private loading: boolean = true;
  @State private actionLoading: boolean = false;
  @State private showEditor: boolean = false;
  @State private editName: string = '';
  @State private selectedImageUri: string = '';
  @State private message: string = '';
  @State private durianUnlocked: boolean = false;

  aboutToAppear(): void {
    this.loadMemorial(true);
    this.loadDurianUnlock();
  }

  private api(): AnyiApiClient {
    return new AnyiApiClient(() => this.user.token);
  }

  private async loadMemorial(showLoading: boolean): Promise<void> {
    if (showLoading) {
      this.loading = true;
    }
    try {
      const response = await this.api().listMemorials();
      this.memorial = response.memorials.length > 0 ? response.memorials[0] : undefined;
      if (this.memorial !== undefined) {
        this.editName = this.memorial.name;
      } else {
        this.showEditor = true;
        this.editName = `${this.user.displayName}的纪念馆`;
      }
    } catch (error) {
      this.message = userFriendlyError(error as Error, '纪念馆加载失败');
    } finally {
      this.loading = false;
    }
  }

  private async loadDurianUnlock(): Promise<void> {
    try {
      const result = await this.api().featureUnlocked('offering_durian');
      this.durianUnlocked = result.unlocked;
    } catch (_) {
      this.durianUnlocked = false;
    }
  }

  private async choosePortrait(): Promise<void> {
    try {
      const uri: string = await pickSingleImage();
      if (uri.length > 0) {
        this.selectedImageUri = uri;
      }
    } catch (error) {
      this.message = userFriendlyError(error as Error, '没有选中照片');
    }
  }

  private async saveMemorial(): Promise<void> {
    const name: string = this.editName.trim();
    if (name.length === 0) {
      this.message = '请填写纪念馆名称';
      return;
    }
    this.actionLoading = true;
    this.message = '';
    try {
      let imageUrl: string = this.memorial?.imageUrl ? this.memorial.imageUrl : '';
      if (this.selectedImageUri.length > 0) {
        const upload = await this.api().uploadImage('memorial/portrait', this.selectedImageUri);
        imageUrl = upload.asset.url;
      }
      const response = this.memorial === undefined
        ? await this.api().createMemorial(name, imageUrl)
        : await this.api().updateMemorial(this.memorial.id, name, imageUrl);
      this.memorial = response.memorial;
      this.editName = response.memorial.name;
      this.selectedImageUri = '';
      this.showEditor = false;
      this.message = '纪念馆已保存到云端';
    } catch (error) {
      this.message = userFriendlyError(error as Error, '纪念馆保存失败');
    } finally {
      this.actionLoading = false;
    }
  }

  private async performOffering(action: string): Promise<void> {
    if (this.memorial === undefined || this.actionLoading) {
      return;
    }
    if (action === 'durian' && !this.durianUnlocked) {
      this.message = '榴莲供品尚未解锁，可在下方完成当前测试版解锁';
      return;
    }
    this.actionLoading = true;
    this.message = '';
    try {
      if (action === 'flower') {
        await this.api().offerFlower(this.memorial.id);
        this.message = '一束白菊已敬献';
      } else if (action === 'candle') {
        await this.api().lightCandle(this.memorial.id);
        this.message = '一盏心灯已点亮';
      } else if (action === 'incense') {
        await this.api().lightIncense(this.memorial.id);
        this.message = '一炷清香已敬上';
      } else {
        await this.api().offerFruit(this.memorial.id, action);
        this.message = action === 'apple' ? '苹果已摆上供桌' : '榴莲已摆上供桌';
      }
      await this.loadMemorial(false);
    } catch (error) {
      this.message = userFriendlyError(error as Error, '操作失败');
    } finally {
      this.actionLoading = false;
    }
  }

  private async unlockDurian(): Promise<void> {
    this.actionLoading = true;
    try {
      const result = await this.api().unlockFeature('offering_durian');
      this.durianUnlocked = result.unlocked;
      this.message = result.unlocked ? '榴莲供品已解锁' : '解锁未完成';
    } catch (error) {
      this.message = userFriendlyError(error as Error, '解锁失败');
    } finally {
      this.actionLoading = false;
    }
  }

  @Builder
  private offeringButton(icon: string, label: string, detail: string, action: string) {
    Button() {
      Column({ space: 5 }) {
        Text(icon)
          .fontSize(26)
        Text(label)
          .fontSize(15)
          .fontWeight(FontWeight.Bold)
          .fontColor(COLORS.ink)
        Text(detail)
          .fontSize(11)
          .fontColor(COLORS.muted)
      }
      .width('100%')
    }
    .width('48%')
    .height(92)
    .backgroundColor(COLORS.paper)
    .borderRadius(16)
    .border({ width: 1, color: COLORS.line })
    .margin({ bottom: 10 })
    .enabled(!this.actionLoading)
    .onClick(() => this.performOffering(action))
  }

  @Builder
  private editorCard() {
    Column({ space: 13 }) {
      Row() {
        Column({ space: 3 }) {
          Text(this.memorial === undefined ? '创建云端纪念馆' : '编辑纪念馆')
            .fontSize(18)
            .fontWeight(FontWeight.Bold)
            .fontColor(COLORS.ink)
          Text('名称和照片会同步到所有安忆客户端')
            .fontSize(12)
            .fontColor(COLORS.muted)
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)
        if (this.memorial !== undefined) {
          Button('收起')
            .fontSize(12)
            .fontColor(COLORS.green)
            .backgroundColor(COLORS.greenLight)
            .height(34)
            .onClick(() => {
              this.showEditor = false;
              this.selectedImageUri = '';
              this.editName = this.memorial?.name ? this.memorial.name : '';
            })
        }
      }
      .width('100%')

      TextInput({ placeholder: '纪念馆名称', text: this.editName })
        .height(52)
        .fontSize(16)
        .backgroundColor(COLORS.white)
        .borderRadius(12)
        .border({ width: 1, color: COLORS.line })
        .padding({ left: 14, right: 14 })
        .onChange((value: string) => this.editName = value)

      Row({ space: 12 }) {
        Stack({ alignContent: Alignment.Center }) {
          if (this.selectedImageUri.length > 0) {
            Image(this.selectedImageUri)
              .width(72)
              .height(72)
              .objectFit(ImageFit.Cover)
              .borderRadius(36)
          } else if (this.memorial?.imageUrl) {
            Image(absoluteAssetUrl(this.memorial.imageUrl))
              .width(72)
              .height(72)
              .objectFit(ImageFit.Cover)
              .borderRadius(36)
          } else {
            Text('念')
              .fontSize(24)
              .fontWeight(FontWeight.Bold)
              .fontColor(COLORS.white)
          }
        }
        .width(72)
        .height(72)
        .borderRadius(36)
        .backgroundColor(COLORS.green)

        Column({ space: 6 }) {
          Text('纪念照片')
            .fontSize(14)
            .fontWeight(FontWeight.Bold)
            .fontColor(COLORS.ink)
          Text('从系统图库安全选择，无需申请完整相册权限')
            .fontSize(11)
            .fontColor(COLORS.muted)
          Button('选择照片')
            .height(34)
            .fontSize(13)
            .fontColor(COLORS.green)
            .backgroundColor(COLORS.greenLight)
            .onClick(() => this.choosePortrait())
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)
      }
      .width('100%')

      Button(this.actionLoading ? '正在保存…' : '保存到云端')
        .width('100%')
        .height(50)
        .fontSize(16)
        .fontWeight(FontWeight.Bold)
        .fontColor(COLORS.white)
        .backgroundColor(COLORS.night)
        .borderRadius(12)
        .enabled(!this.actionLoading)
        .onClick(() => this.saveMemorial())
    }
    .width('100%')
    .padding(16)
    .backgroundColor(COLORS.paper)
    .borderRadius(18)
    .border({ width: 1, color: COLORS.line })
  }

  @Builder
  private memorialStage(memorial: Memorial) {
    Stack({ alignContent: Alignment.Center }) {
      Image($r('app.media.anyi_hall_memorial_bg_v3'))
        .width('100%')
        .height('100%')
        .objectFit(ImageFit.Cover)
        .borderRadius(20)

      Column({ space: 10 }) {
        Stack({ alignContent: Alignment.Center }) {
          if (memorial.imageUrl) {
            Image(absoluteAssetUrl(memorial.imageUrl))
              .width(116)
              .height(146)
              .objectFit(ImageFit.Cover)
              .borderRadius(8)
          } else {
            Text('念')
              .fontSize(42)
              .fontWeight(FontWeight.Bold)
              .fontColor(COLORS.green)
          }
        }
        .width(128)
        .height(158)
        .padding(6)
        .backgroundColor('#FFFDF7')
        .border({ width: 4, color: '#98713A' })
        .borderRadius(10)
        .shadow({ radius: 12, color: '#4025231F', offsetY: 6 })

        Text(memorial.name)
          .fontSize(19)
          .fontWeight(FontWeight.Bold)
          .fontColor('#3B2C19')
          .padding({ left: 18, right: 18, top: 8, bottom: 8 })
          .backgroundColor('#EBE2C7')
          .borderRadius(6)

        Row({ space: 14 }) {
          Text(`花 ${memorial.flowerOfferings.length}/2`)
            .fontSize(11)
            .fontColor('#6D5534')
          Text(`灯 ${memorial.candleUntilList.length}/2`)
            .fontSize(11)
            .fontColor('#6D5534')
          Text(memorial.incenseUntil > Date.now() ? '香火延续' : '待敬香')
            .fontSize(11)
            .fontColor('#6D5534')
        }
        .padding({ left: 12, right: 12, top: 6, bottom: 6 })
        .backgroundColor('#DDFBF3E2')
        .borderRadius(14)
      }

      Image($r('app.media.anyi_hall_stage_foreground_empty'))
        .width('100%')
        .height('100%')
        .objectFit(ImageFit.Fill)
        .opacity(0.28)
    }
    .width('100%')
    .height(390)
    .borderRadius(20)
    .clip(true)
    .border({ width: 1, color: '#80C99532' })
  }

  build() {
    Stack({ alignContent: Alignment.Top }) {
      Image($r('app.media.anyi_hall_page_bg'))
        .width('100%')
        .height('100%')
        .objectFit(ImageFit.Cover)
        .opacity(0.35)

      Scroll() {
        Column({ space: 14 }) {
          Row() {
            Column({ space: 4 }) {
              Text('云端纪念馆')
                .fontSize(27)
                .fontWeight(FontWeight.Bold)
                .fontColor(COLORS.ink)
              Text('在安静的地方，留住重要的记忆')
                .fontSize(13)
                .fontColor(COLORS.muted)
            }
            .alignItems(HorizontalAlign.Start)
            .layoutWeight(1)

            if (this.memorial !== undefined) {
              Button('编辑')
                .height(38)
                .fontSize(13)
                .fontColor(COLORS.green)
                .backgroundColor('#EFFFFFFF')
                .border({ width: 1, color: COLORS.line })
                .onClick(() => this.showEditor = true)
            }
          }
          .width('100%')

          StatusBanner({ message: this.message })

          if (this.loading) {
            LoadingState({ text: '正在打开纪念馆…' })
          } else {
            if (this.showEditor) {
              this.editorCard()
            }

            if (this.memorial !== undefined) {
              this.memorialStage(this.memorial)

              Column({ space: 12 }) {
                Row() {
                  Text('寄托思念')
                    .fontSize(18)
                    .fontWeight(FontWeight.Bold)
                    .fontColor(COLORS.ink)
                  Blank()
                  if (this.actionLoading) {
                    LoadingProgress()
                      .width(22)
                      .height(22)
                      .color(COLORS.green)
                  }
                }
                .width('100%')

                Flex({ wrap: FlexWrap.Wrap, justifyContent: FlexAlign.SpaceBetween }) {
                  this.offeringButton('💐', '敬献鲜花', '白菊花束', 'flower')
                  this.offeringButton('🕯', '点亮心灯', '持续十分钟', 'candle')
                  this.offeringButton('〰', '敬一炷香', '香火延续', 'incense')
                  this.offeringButton('🍎', '供奉苹果', '最多三个', 'apple')
                  this.offeringButton('🌿', '供奉榴莲', this.durianUnlocked ? '已解锁' : '待解锁', 'durian')
                }
                .width('100%')

                if (!this.durianUnlocked) {
                  Button('解锁榴莲供品（当前测试流程）')
                    .width('100%')
                    .height(44)
                    .fontSize(13)
                    .fontColor('#72551E')
                    .backgroundColor(COLORS.amberLight)
                    .borderRadius(12)
                    .enabled(!this.actionLoading)
                    .onClick(() => this.unlockDurian())
                }
              }
              .width('100%')
              .padding(16)
              .backgroundColor('#F5FFFEFA')
              .borderRadius(18)
              .border({ width: 1, color: COLORS.line })
            } else if (!this.showEditor) {
              EmptyState({ title: '还没有纪念馆', description: '创建一个纪念馆，把重要的人和故事留在云端。' })
            }
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
    .backgroundColor('#F2F7F1E8')
  }
}
