import { webview } from '@kit.ArkWeb';
import { COLORS, DEFAULT_DIGITAL_HUMAN_URL } from '../../common/Constants';
import { StatusBanner } from '../../components/Common';
import { AppUser, DigitalHumanMessage } from '../../models/Models';
import { AnyiApiClient, userFriendlyError } from '../../services/ApiClient';

interface CharacterItem {
  id: string;
  label: string;
  description: string;
}

@Component
export struct CompanionPage {
  user: AppUser = new AppUser('', '', '', 'user', '');

  @State private selectedCharacterId: string = 'grandpa';
  @State private messages: Array<DigitalHumanMessage> = [];
  @State private draft: string = '';
  @State private sending: boolean = false;
  @State private message: string = '';
  @State private memoryEnabled: boolean = false;
  @State private memoryCount: number = 0;
  @State private webMode: boolean = false;
  @State private digitalHumanUrl: string = DEFAULT_DIGITAL_HUMAN_URL;

  private controller: webview.WebviewController = new webview.WebviewController();
  private characters: Array<CharacterItem> = [
    { id: 'grandpa', label: '爷爷', description: '沉稳、温暖地听你慢慢说' },
    { id: 'grandma', label: '奶奶', description: '柔和、亲切地陪在你身边' }
  ];

  aboutToAppear(): void {
    if (this.messages.length === 0) {
      this.messages = this.initialMessages('grandpa');
    }
    this.loadConfig();
    this.loadMessages('grandpa');
    this.loadMemorySettings();
    this.loadMemories('grandpa');
  }

  private api(): AnyiApiClient {
    return new AnyiApiClient(() => this.user.token);
  }

  private async loadConfig(): Promise<void> {
    try {
      const response = await this.api().appConfig();
      if (response.digitalHuman && response.digitalHuman.url && response.digitalHuman.url.startsWith('http')) {
        this.digitalHumanUrl = response.digitalHuman.url;
      }
    } catch (_) {
      this.digitalHumanUrl = DEFAULT_DIGITAL_HUMAN_URL;
    }
  }

  private initialMessages(characterId: string): Array<DigitalHumanMessage> {
    const content: string = characterId === 'grandma'
      ? '孩子，奶奶在呢。你想说什么，我都听着。'
      : '孩子，爷爷在这儿。有什么话慢慢说。';
    return [{
      id: `welcome-${characterId}-${Date.now()}`,
      sender: 'assistant',
      content,
      createdAt: Date.now()
    }];
  }

  private async loadMessages(characterId: string): Promise<void> {
    try {
      const response = await this.api().listDigitalHumanMessages(characterId);
      if (this.selectedCharacterId !== characterId) {
        return;
      }
      this.messages = response.messages.length > 0 ? response.messages : this.initialMessages(characterId);
    } catch (error) {
      if (this.selectedCharacterId === characterId) {
        this.messages = this.initialMessages(characterId);
        this.message = userFriendlyError(error as Error, '聊天记录暂时没有加载');
      }
    }
  }

  private async loadMemorySettings(): Promise<void> {
    try {
      const response = await this.api().digitalHumanMemorySettings();
      this.memoryEnabled = response.enabled;
    } catch (_) {
      this.memoryEnabled = false;
    }
  }

  private async loadMemories(characterId: string): Promise<void> {
    try {
      const response = await this.api().listDigitalHumanMemories(characterId);
      if (this.selectedCharacterId === characterId) {
        this.memoryCount = response.memories.length;
      }
    } catch (_) {
      if (this.selectedCharacterId === characterId) {
        this.memoryCount = 0;
      }
    }
  }

  private async toggleMemory(): Promise<void> {
    try {
      const response = await this.api().setDigitalHumanMemoryEnabled(!this.memoryEnabled);
      this.memoryEnabled = response.enabled;
      this.message = this.memoryEnabled ? '长期记忆已开启' : '长期记忆已关闭';
    } catch (error) {
      this.message = userFriendlyError(error as Error, '设置长期记忆失败');
    }
  }

  private selectCharacter(id: string): void {
    if (this.selectedCharacterId === id || this.sending) {
      return;
    }
    this.selectedCharacterId = id;
    this.messages = this.initialMessages(id);
    this.draft = '';
    this.message = '';
    this.loadMessages(id);
    this.loadMemories(id);
  }

  private async send(): Promise<void> {
    const content: string = this.draft.trim();
    if (content.length === 0 || this.sending) {
      return;
    }
    const userMessage: DigitalHumanMessage = {
      id: `user-${Date.now()}`,
      sender: 'user',
      content,
      createdAt: Date.now()
    };
    this.messages = this.messages.concat([userMessage]);
    this.draft = '';
    this.sending = true;
    this.message = '';
    try {
      const response = await this.api().digitalHumanChat(this.selectedCharacterId, content);
      this.messages = this.messages.concat([response.message]);
      this.loadMemories(this.selectedCharacterId);
    } catch (error) {
      this.draft = content;
      this.message = userFriendlyError(error as Error, '陪伴回复暂时没有送达');
    } finally {
      this.sending = false;
    }
  }

  @Builder
  private characterButton(item: CharacterItem) {
    Button() {
      Row({ space: 8 }) {
        Stack({ alignContent: Alignment.Center }) {
          Text(item.id === 'grandpa' ? '爷' : '奶')
            .fontSize(15)
            .fontWeight(FontWeight.Bold)
            .fontColor(this.selectedCharacterId === item.id ? COLORS.white : COLORS.green)
        }
        .width(30)
        .height(30)
        .borderRadius(15)
        .backgroundColor(this.selectedCharacterId === item.id ? COLORS.green : COLORS.greenLight)
        Text(item.label)
          .fontSize(14)
          .fontWeight(FontWeight.Bold)
          .fontColor(COLORS.ink)
      }
    }
    .layoutWeight(1)
    .height(46)
    .backgroundColor(this.selectedCharacterId === item.id ? COLORS.amberLight : COLORS.paper)
    .borderRadius(14)
    .border({ width: 1, color: this.selectedCharacterId === item.id ? COLORS.amber : COLORS.line })
    .onClick(() => this.selectCharacter(item.id))
  }

  @Builder
  private chatBubble(item: DigitalHumanMessage) {
    Row() {
      if (item.sender === 'user') {
        Blank()
      }
      Column({ space: 3 }) {
        Text(item.content)
          .fontSize(15)
          .lineHeight(22)
          .fontColor(item.sender === 'user' ? COLORS.white : COLORS.ink)
        Text(item.sender === 'user' ? '我' : (this.selectedCharacterId === 'grandma' ? '奶奶' : '爷爷'))
          .fontSize(10)
          .fontColor(item.sender === 'user' ? '#D9FFFFFF' : COLORS.muted)
      }
      .alignItems(item.sender === 'user' ? HorizontalAlign.End : HorizontalAlign.Start)
      .constraintSize({ maxWidth: '82%' })
      .padding({ left: 13, right: 13, top: 10, bottom: 9 })
      .backgroundColor(item.sender === 'user' ? COLORS.green : COLORS.paper)
      .borderRadius(item.sender === 'user'
        ? { topLeft: 16, topRight: 4, bottomLeft: 16, bottomRight: 16 }
        : { topLeft: 4, topRight: 16, bottomLeft: 16, bottomRight: 16 })
      .border({ width: item.sender === 'user' ? 0 : 1, color: COLORS.line })
      if (item.sender !== 'user') {
        Blank()
      }
    }
    .width('100%')
    .padding({ top: 5, bottom: 5 })
  }

  @Builder
  private localCompanion() {
    Scroll() {
      Column({ space: 14 }) {
        Row() {
          Column({ space: 4 }) {
            Text('AI 陪伴')
              .fontSize(27)
              .fontWeight(FontWeight.Bold)
              .fontColor(COLORS.ink)
            Text('温柔倾听，不替代真实的人与专业帮助')
              .fontSize(13)
              .fontColor(COLORS.muted)
          }
          .alignItems(HorizontalAlign.Start)
          .layoutWeight(1)
          Button('2D 数字人')
            .height(38)
            .fontSize(13)
            .fontColor(COLORS.green)
            .backgroundColor(COLORS.greenLight)
            .onClick(() => this.webMode = true)
          Button(this.memoryEnabled ? `记忆 ${this.memoryCount}` : '开启记忆')
            .height(38)
            .fontSize(13)
            .fontColor(COLORS.green)
            .backgroundColor(this.memoryEnabled ? COLORS.amberLight : COLORS.greenLight)
            .onClick(() => this.toggleMemory())
        }
        .width('100%')

        Row({ space: 10 }) {
          ForEach(this.characters, (item: CharacterItem) => {
            this.characterButton(item)
          })
        }
        .width('100%')

        Stack({ alignContent: Alignment.Bottom }) {
          Image($r('app.media.anyi_ai_page_bg'))
            .width('100%')
            .height('100%')
            .objectFit(ImageFit.Cover)
            .opacity(0.55)

          if (this.selectedCharacterId === 'grandma') {
            Image($r('app.media.anyi_digital_grandma'))
              .width('90%')
              .height('94%')
              .objectFit(ImageFit.Contain)
          } else {
            Image($r('app.media.anyi_digital_grandpa'))
              .width('90%')
              .height('94%')
              .objectFit(ImageFit.Contain)
          }

          Column({ space: 2 }) {
            Text(this.selectedCharacterId === 'grandma' ? '奶奶' : '爷爷')
              .fontSize(18)
              .fontWeight(FontWeight.Bold)
              .fontColor(COLORS.ink)
            Text(this.selectedCharacterId === 'grandma'
              ? '孩子，奶奶在呢'
              : '孩子，爷爷在这儿')
              .fontSize(12)
              .fontColor(COLORS.muted)
          }
          .width('100%')
          .padding(12)
          .backgroundColor('#EFFFFFFF')
        }
        .width('100%')
        .height(310)
        .borderRadius(20)
        .clip(true)
        .border({ width: 1, color: COLORS.line })

        Column({ space: 8 }) {
          List({ space: 0 }) {
            ForEach(this.messages, (item: DigitalHumanMessage) => {
              ListItem() {
                this.chatBubble(item)
              }
            }, (item: DigitalHumanMessage) => item.id)
            if (this.sending) {
              ListItem() {
                Row({ space: 8 }) {
                  LoadingProgress()
                    .width(18)
                    .height(18)
                    .color(COLORS.green)
                  Text('正在认真听你说…')
                    .fontSize(12)
                    .fontColor(COLORS.muted)
                }
                .width('100%')
                .padding(10)
              }
            }
          }
          .width('100%')
          .height(330)
          .scrollBar(BarState.Off)

          StatusBanner({ message: this.message })

          Row({ space: 10 }) {
            TextArea({ placeholder: '把想说的话写下来…', text: this.draft })
              .layoutWeight(1)
              .height(58)
              .fontSize(14)
              .backgroundColor(COLORS.white)
              .borderRadius(14)
              .border({ width: 1, color: COLORS.line })
              .padding({ left: 12, right: 12, top: 8, bottom: 8 })
              .onChange((value: string) => this.draft = value)
            Button('发送')
              .width(76)
              .height(52)
              .fontSize(14)
              .fontWeight(FontWeight.Bold)
              .fontColor(COLORS.white)
              .backgroundColor(this.sending ? '#8FA79D' : COLORS.green)
              .borderRadius(14)
              .enabled(!this.sending)
              .onClick(() => this.send())
          }
          .width('100%')
        }
        .width('100%')
        .padding(14)
        .backgroundColor('#F5FFFEFA')
        .borderRadius(18)
        .border({ width: 1, color: COLORS.line })
      }
      .width('100%')
      .constraintSize({ maxWidth: 760 })
      .padding({ left: 16, right: 16, top: 18, bottom: 28 })
    }
    .width('100%')
    .height('100%')
    .scrollBar(BarState.Off)
  }

  @Builder
  private digitalHumanWeb() {
    Column() {
      Row({ space: 10 }) {
        Button('返回')
          .height(38)
          .fontSize(13)
          .fontColor(COLORS.green)
          .backgroundColor(COLORS.greenLight)
          .onClick(() => this.webMode = false)
        Column({ space: 2 }) {
          Text('2D 数字人')
            .fontSize(19)
            .fontWeight(FontWeight.Bold)
            .fontColor(COLORS.ink)
          Text('由安忆云端数字人服务提供')
            .fontSize(11)
            .fontColor(COLORS.muted)
        }
        .alignItems(HorizontalAlign.Start)
      }
      .width('100%')
      .height(62)
      .padding({ left: 14, right: 14 })
      .backgroundColor(COLORS.paper)
      .border({ width: { bottom: 1 }, color: COLORS.line })

      Web({ src: this.digitalHumanUrl, controller: this.controller })
        .width('100%')
        .layoutWeight(1)
        .javaScriptAccess(true)
        .domStorageAccess(true)
        .onlineImageAccess(true)
        .zoomAccess(false)
    }
    .width('100%')
    .height('100%')
  }

  build() {
    Stack() {
      if (this.webMode) {
        this.digitalHumanWeb()
      } else {
        this.localCompanion()
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(COLORS.background)
  }
}
