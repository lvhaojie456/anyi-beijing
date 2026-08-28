import { COLORS } from '../common/Constants';

function messageLooksLikeProblem(message: string): boolean {
  const markers: Array<string> = ['失败', '错误', '超时', '过期', '不正确', '不能', '未配置', '未通过', '没有'];
  return markers.some((value: string) => message.includes(value));
}

@Component
export struct StatusBanner {
  @Prop message: string = '';

  build() {
    if (this.message.length > 0) {
      Row({ space: 8 }) {
        Text(messageLooksLikeProblem(this.message) ? '!' : '✓')
          .fontSize(14)
          .fontWeight(FontWeight.Bold)
          .fontColor(messageLooksLikeProblem(this.message) ? COLORS.danger : COLORS.green)
        Text(this.message)
          .fontSize(13)
          .lineHeight(19)
          .fontColor(messageLooksLikeProblem(this.message) ? COLORS.danger : COLORS.green)
          .layoutWeight(1)
      }
      .width('100%')
      .padding({ left: 12, right: 12, top: 10, bottom: 10 })
      .backgroundColor(messageLooksLikeProblem(this.message) ? COLORS.dangerLight : COLORS.greenLight)
      .borderRadius(12)
      .border({
        width: 1,
        color: messageLooksLikeProblem(this.message) ? '#33B42318' : '#33315F4B'
      })
    }
  }
}

@Component
export struct UserAvatar {
  @Prop imageUrl: string = '';
  @Prop fallback: string = '安';
  @Prop size: number = 44;

  build() {
    Stack({ alignContent: Alignment.Center }) {
      if (this.imageUrl.length > 0) {
        Image(this.imageUrl)
          .width(this.size)
          .height(this.size)
          .objectFit(ImageFit.Cover)
          .borderRadius(this.size / 2)
      } else {
        Text(this.fallback.length > 0 ? this.fallback.substring(0, 1) : '安')
          .fontSize(this.size * 0.4)
          .fontWeight(FontWeight.Bold)
          .fontColor(COLORS.white)
      }
    }
    .width(this.size)
    .height(this.size)
    .borderRadius(this.size / 2)
    .backgroundColor(COLORS.green)
    .border({ width: 2, color: COLORS.white })
  }
}

@Component
export struct LoadingState {
  @Prop text: string = '正在加载…';

  build() {
    Column({ space: 12 }) {
      LoadingProgress()
        .width(30)
        .height(30)
        .color(COLORS.green)
      Text(this.text)
        .fontSize(13)
        .fontColor(COLORS.muted)
    }
    .width('100%')
    .padding(28)
  }
}

@Component
export struct EmptyState {
  @Prop title: string = '暂无内容';
  @Prop description: string = '';

  build() {
    Column({ space: 8 }) {
      Text('☁')
        .fontSize(36)
        .fontColor(COLORS.amber)
      Text(this.title)
        .fontSize(17)
        .fontWeight(FontWeight.Bold)
        .fontColor(COLORS.ink)
      if (this.description.length > 0) {
        Text(this.description)
          .fontSize(13)
          .fontColor(COLORS.muted)
          .textAlign(TextAlign.Center)
      }
    }
    .width('100%')
    .padding(28)
  }
}
