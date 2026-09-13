import { absoluteAssetUrl, COLORS } from '../../common/Constants';
import {
  EmptyState,
  LoadingState,
  StatusBanner,
  UserAvatar
} from '../../components/Common';
import {
  AppUser,
  CommunityComment,
  CommunityPost,
  CommunityVolunteer,
  VolunteerApplication
} from '../../models/Models';
import { AnyiApiClient, userFriendlyError } from '../../services/ApiClient';
import { pickImages } from '../../services/MediaPicker';

function displayTime(value: string): string {
  if (value.length < 10) {
    return value;
  }
  return value.replace('T', ' ').replace('Z', '').substring(0, Math.min(16, value.length));
}

@Component
export struct CommunityPage {
  user: AppUser = new AppUser('', '', '', 'user', '');

  @State private posts: Array<CommunityPost> = [];
  @State private volunteers: Array<CommunityVolunteer> = [];
  @State private applications: Array<VolunteerApplication> = [];
  @State private loading: boolean = true;
  @State private message: string = '';
  @State private publishing: boolean = false;
  @State private showComposer: boolean = false;
  @State private postDraft: string = '';
  @State private selectedImageUris: Array<string> = [];
  @State private commentsPostId: string = '';
  @State private comments: Array<CommunityComment> = [];
  @State private commentDraft: string = '';
  @State private commentLoading: boolean = false;
  @State private applyingVolunteerId: string = '';
  @State private applicantName: string = '';
  @State private applicantPhone: string = '';
  @State private applicantNote: string = '';
  @State private actionId: string = '';
  @State private showVolunteerComposer: boolean = false;
  @State private volunteerTitle: string = '';
  @State private volunteerBody: string = '';
  @State private volunteerContact: string = '';
  @State private volunteerCoverUri: string = '';

  aboutToAppear(): void {
    this.refresh();
  }

  private api(): AnyiApiClient {
    return new AnyiApiClient(() => this.user.token);
  }

  private async refresh(): Promise<void> {
    this.loading = true;
    this.message = '';
    try {
      const postResponse = await this.api().listCommunityPosts();
      const volunteerResponse = await this.api().communityVolunteerInfo();
      this.posts = postResponse.posts;
      this.volunteers = volunteerResponse.volunteers;
      if (this.user.role === 'admin') {
        try {
          const applicationResponse = await this.api().listVolunteerApplications();
          this.applications = applicationResponse.applications;
        } catch (_) {
          this.applications = [];
        }
      }
    } catch (error) {
      this.message = userFriendlyError(error as Error, '社区加载失败');
    } finally {
      this.loading = false;
    }
  }

  private async choosePostImages(): Promise<void> {
    try {
      this.selectedImageUris = await pickImages(3);
    } catch (error) {
      this.message = userFriendlyError(error as Error, '没有选中图片');
    }
  }

  private async publishPost(): Promise<void> {
    const content: string = this.postDraft.trim();
    if (content.length === 0 && this.selectedImageUris.length === 0) {
      this.message = '请输入动态内容或选择图片';
      return;
    }
    this.publishing = true;
    this.message = '';
    try {
      const imageUrls: Array<string> = [];
      for (let index: number = 0; index < this.selectedImageUris.length; index++) {
        const response = await this.api().uploadImage(
          'community/post',
          this.selectedImageUris[index]
        );
        imageUrls.push(response.asset.url);
      }
      const response = await this.api().createCommunityPost(content, imageUrls);
      this.posts = [response.post].concat(this.posts);
      this.postDraft = '';
      this.selectedImageUris = [];
      this.showComposer = false;
      this.message = '动态已发布';
    } catch (error) {
      this.message = userFriendlyError(error as Error, '动态发布失败');
    } finally {
      this.publishing = false;
    }
  }

  private async likePost(post: CommunityPost): Promise<void> {
    this.actionId = post.id;
    try {
      const response = await this.api().likeCommunityPost(post.id);
      this.posts = this.posts.map((item: CommunityPost) =>
        item.id === post.id ? response.post : item
      );
    } catch (error) {
      this.message = userFriendlyError(error as Error, '点赞失败');
    } finally {
      this.actionId = '';
    }
  }

  private async deletePost(post: CommunityPost): Promise<void> {
    this.actionId = post.id;
    try {
      await this.api().deleteCommunityPost(post.id);
      this.posts = this.posts.filter((item: CommunityPost) => item.id !== post.id);
      if (this.commentsPostId === post.id) {
        this.commentsPostId = '';
        this.comments = [];
      }
      this.message = '动态已删除';
    } catch (error) {
      this.message = userFriendlyError(error as Error, '动态删除失败');
    } finally {
      this.actionId = '';
    }
  }

  private async toggleComments(post: CommunityPost): Promise<void> {
    if (this.commentsPostId === post.id) {
      this.commentsPostId = '';
      this.comments = [];
      return;
    }
    this.commentsPostId = post.id;
    this.comments = [];
    this.commentDraft = '';
    this.commentLoading = true;
    try {
      const response = await this.api().listCommunityPostComments(post.id);
      this.comments = response.comments;
    } catch (error) {
      this.message = userFriendlyError(error as Error, '评论加载失败');
    } finally {
      this.commentLoading = false;
    }
  }

  private async publishComment(post: CommunityPost): Promise<void> {
    const content: string = this.commentDraft.trim();
    if (content.length === 0) {
      return;
    }
    this.commentLoading = true;
    try {
      const response = await this.api().createCommunityPostComment(post.id, content);
      this.comments = this.comments.concat([response.comment]);
      this.posts = this.posts.map((item: CommunityPost) =>
        item.id === post.id ? response.post : item
      );
      this.commentDraft = '';
    } catch (error) {
      this.message = userFriendlyError(error as Error, '评论发布失败');
    } finally {
      this.commentLoading = false;
    }
  }

  private async deleteComment(post: CommunityPost, comment: CommunityComment): Promise<void> {
    this.actionId = comment.id;
    try {
      const response = await this.api().deleteCommunityPostComment(post.id, comment.id);
      this.comments = this.comments.filter((item: CommunityComment) => item.id !== comment.id);
      this.posts = this.posts.map((item: CommunityPost) =>
        item.id === post.id ? response.post : item
      );
      this.message = '评论已删除';
    } catch (error) {
      this.message = userFriendlyError(error as Error, '评论删除失败');
    } finally {
      this.actionId = '';
    }
  }

  private async chooseVolunteerCover(): Promise<void> {
    try {
      const values: Array<string> = await pickImages(1);
      this.volunteerCoverUri = values.length > 0 ? values[0] : '';
    } catch (error) {
      this.message = userFriendlyError(error as Error, '没有选中封面');
    }
  }

  private async publishVolunteer(): Promise<void> {
    const title: string = this.volunteerTitle.trim();
    const body: string = this.volunteerBody.trim();
    if (title.length === 0 || body.length === 0) {
      this.message = '请填写义工招募标题和内容';
      return;
    }
    this.publishing = true;
    try {
      let imageUrl: string = '';
      if (this.volunteerCoverUri.length > 0) {
        const upload = await this.api().uploadImage('community/volunteer', this.volunteerCoverUri);
        imageUrl = upload.asset.url;
      }
      const response = await this.api().createCommunityVolunteer(
        title,
        body,
        this.volunteerContact.trim(),
        imageUrl
      );
      this.volunteers = [response.volunteer].concat(
        this.volunteers.filter((item: CommunityVolunteer) => item.id !== response.volunteer.id));
      this.showVolunteerComposer = false;
      this.volunteerTitle = '';
      this.volunteerBody = '';
      this.volunteerContact = '';
      this.volunteerCoverUri = '';
      this.message = '义工招募已发布';
    } catch (error) {
      this.message = userFriendlyError(error as Error, '义工招募发布失败');
    } finally {
      this.publishing = false;
    }
  }

  private openVolunteerForm(volunteer: CommunityVolunteer): void {
    this.applyingVolunteerId = this.applyingVolunteerId === volunteer.id ? '' : volunteer.id;
    this.applicantName = this.user.displayName;
    this.applicantPhone = '';
    this.applicantNote = '';
  }

  private async submitVolunteerApplication(volunteer: CommunityVolunteer): Promise<void> {
    if (this.applicantName.trim().length === 0 || this.applicantPhone.trim().length === 0) {
      this.message = '请填写姓名和联系方式';
      return;
    }
    this.actionId = volunteer.id;
    try {
      await this.api().applyForCommunityVolunteer(
        volunteer.id,
        this.applicantName.trim(),
        this.applicantPhone.trim(),
        this.applicantNote.trim()
      );
      this.applyingVolunteerId = '';
      this.message = '义工报名已提交，等待管理员审核';
    } catch (error) {
      this.message = userFriendlyError(error as Error, '义工报名失败');
    } finally {
      this.actionId = '';
    }
  }

  private async reviewApplication(
    application: VolunteerApplication,
    status: string
  ): Promise<void> {
    this.actionId = application.id;
    try {
      const response = await this.api().reviewVolunteerApplication(application.id, status);
      this.applications = this.applications.map((item: VolunteerApplication) =>
        item.id === application.id ? response.application : item);
      this.message = status === 'approved' ? '报名已通过' : '报名已拒绝';
    } catch (error) {
      this.message = userFriendlyError(error as Error, '报名审核失败');
    } finally {
      this.actionId = '';
    }
  }

  @Builder
  private composerCard() {
    Column({ space: 12 }) {
      Row() {
        Text('发布一条社区动态')
          .fontSize(17)
          .fontWeight(FontWeight.Bold)
          .fontColor(COLORS.ink)
        Blank()
        Button('收起')
          .height(32)
          .fontSize(12)
          .fontColor(COLORS.green)
          .backgroundColor(COLORS.greenLight)
          .onClick(() => this.showComposer = false)
      }
      .width('100%')

      TextArea({
        placeholder: '分享一段回忆、一个故事，或此刻的心情…',
        text: this.postDraft
      })
        .width('100%')
        .height(116)
        .fontSize(15)
        .backgroundColor(COLORS.white)
        .borderRadius(12)
        .border({ width: 1, color: COLORS.line })
        .padding(12)
        .onChange((value: string) => this.postDraft = value)

      if (this.selectedImageUris.length > 0) {
        Row({ space: 8 }) {
          ForEach(this.selectedImageUris, (uri: string) => {
            Image(uri)
              .width(78)
              .height(78)
              .objectFit(ImageFit.Cover)
              .borderRadius(10)
          }, (uri: string) => uri)
        }
        .width('100%')
      }

      Row({ space: 10 }) {
        Button(
          this.selectedImageUris.length > 0
            ? `已选 ${this.selectedImageUris.length} 张`
            : '选择图片'
        )
          .layoutWeight(1)
          .height(44)
          .fontSize(13)
          .fontColor(COLORS.green)
          .backgroundColor(COLORS.greenLight)
          .onClick(() => this.choosePostImages())
        Button(this.publishing ? '正在发布…' : '发布')
          .layoutWeight(1)
          .height(44)
          .fontSize(14)
          .fontWeight(FontWeight.Bold)
          .fontColor(COLORS.white)
          .backgroundColor(this.publishing ? '#8FA79D' : COLORS.green)
          .enabled(!this.publishing)
          .onClick(() => this.publishPost())
      }
      .width('100%')
    }
    .width('100%')
    .padding(15)
    .backgroundColor(COLORS.paper)
    .borderRadius(18)
    .border({ width: 1, color: COLORS.line })
  }

  @Builder
  private commentPanel(post: CommunityPost) {
    Column({ space: 9 }) {
      if (this.commentLoading && this.comments.length === 0) {
        Row({ space: 8 }) {
          LoadingProgress()
            .width(18)
            .height(18)
            .color(COLORS.green)
          Text('正在加载评论…')
            .fontSize(12)
            .fontColor(COLORS.muted)
        }
        .width('100%')
      } else if (this.comments.length === 0) {
        Text('还没有评论，来温柔地说第一句话吧。')
          .width('100%')
          .fontSize(12)
          .fontColor(COLORS.muted)
      } else {
        ForEach(this.comments, (comment: CommunityComment) => {
          Row({ space: 8 }) {
            UserAvatar({
              imageUrl: comment.authorAvatarUrl ? absoluteAssetUrl(comment.authorAvatarUrl) : '',
              fallback: comment.authorName,
              size: 30
            })
            Column({ space: 2 }) {
              Text(comment.authorName)
                .fontSize(12)
                .fontWeight(FontWeight.Bold)
                .fontColor(COLORS.ink)
              Text(comment.content)
                .fontSize(13)
                .lineHeight(19)
                .fontColor(COLORS.ink)
            }
            .alignItems(HorizontalAlign.Start)
            .layoutWeight(1)
            if (post.authorId === this.user.id || this.user.role === 'admin') {
              Button('删除')
                .height(28)
                .fontSize(10)
                .fontColor(COLORS.danger)
                .backgroundColor(COLORS.dangerLight)
                .enabled(this.actionId.length === 0)
                .onClick(() => this.deleteComment(post, comment))
            }
          }
          .width('100%')
        }, (comment: CommunityComment) => comment.id)
      }

      Row({ space: 8 }) {
        TextInput({ placeholder: '写下评论…', text: this.commentDraft })
          .layoutWeight(1)
          .height(42)
          .fontSize(13)
          .backgroundColor(COLORS.white)
          .borderRadius(12)
          .border({ width: 1, color: COLORS.line })
          .padding({ left: 11, right: 11 })
          .onChange((value: string) => this.commentDraft = value)
        Button('发送')
          .width(64)
          .height(40)
          .fontSize(12)
          .fontColor(COLORS.white)
          .backgroundColor(COLORS.green)
          .enabled(!this.commentLoading)
          .onClick(() => this.publishComment(post))
      }
      .width('100%')
    }
    .width('100%')
    .padding(12)
    .backgroundColor('#F3F1EC')
    .borderRadius(12)
  }

  @Builder
  private postCard(post: CommunityPost) {
    Column({ space: 11 }) {
      Row({ space: 10 }) {
        UserAvatar({
          imageUrl: post.authorAvatarUrl ? absoluteAssetUrl(post.authorAvatarUrl) : '',
          fallback: post.authorName,
          size: 42
        })
        Column({ space: 2 }) {
          Text(post.authorName)
            .fontSize(15)
            .fontWeight(FontWeight.Bold)
            .fontColor(COLORS.ink)
          Text(`@${post.authorUsername} · ${displayTime(post.createdAt)}`)
            .fontSize(11)
            .fontColor(COLORS.muted)
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)
        if (post.authorId === this.user.id || this.user.role === 'admin') {
          Button('删除')
            .height(30)
            .fontSize(11)
            .fontColor(COLORS.danger)
            .backgroundColor(COLORS.dangerLight)
            .enabled(this.actionId.length === 0)
            .onClick(() => this.deletePost(post))
        }
      }
      .width('100%')

      if (post.content.length > 0) {
        Text(post.content)
          .width('100%')
          .fontSize(15)
          .lineHeight(23)
          .fontColor(COLORS.ink)
      }

      if (post.imageUrls.length > 0) {
        Flex({ wrap: FlexWrap.Wrap }) {
          ForEach(post.imageUrls, (value: string) => {
            Image(absoluteAssetUrl(value))
              .width(post.imageUrls.length === 1 ? '100%' : '32%')
              .height(post.imageUrls.length === 1 ? 210 : 106)
              .objectFit(ImageFit.Cover)
              .borderRadius(10)
              .margin({ right: 4, bottom: 4 })
          }, (value: string) => value)
        }
        .width('100%')
      }

      Row({ space: 10 }) {
        Button(post.likedByMe ? `♥ ${post.likeCount}` : `♡ ${post.likeCount}`)
          .layoutWeight(1)
          .height(38)
          .fontSize(13)
          .fontColor(post.likedByMe ? COLORS.danger : COLORS.muted)
          .backgroundColor(post.likedByMe ? COLORS.dangerLight : '#F4F2ED')
          .enabled(this.actionId.length === 0)
          .onClick(() => this.likePost(post))
        Button(`评论 ${post.commentCount}`)
          .layoutWeight(1)
          .height(38)
          .fontSize(13)
          .fontColor(COLORS.green)
          .backgroundColor(COLORS.greenLight)
          .onClick(() => this.toggleComments(post))
      }
      .width('100%')

      if (this.commentsPostId === post.id) {
        this.commentPanel(post)
      }
    }
    .width('100%')
    .padding(15)
    .backgroundColor(COLORS.paper)
    .borderRadius(18)
    .border({ width: 1, color: COLORS.line })
  }

  @Builder
  private volunteerCard(volunteer: CommunityVolunteer) {
    Column({ space: 10 }) {
      if (volunteer.imageUrl) {
        Image(absoluteAssetUrl(volunteer.imageUrl))
          .width('100%')
          .height(150)
          .objectFit(ImageFit.Cover)
          .borderRadius(12)
      }
      Row() {
        Text('义工招募')
          .fontSize(11)
          .fontWeight(FontWeight.Bold)
          .fontColor('#72551E')
          .padding({ left: 9, right: 9, top: 5, bottom: 5 })
          .backgroundColor(COLORS.amberLight)
          .borderRadius(12)
        Blank()
        Text(displayTime(volunteer.createdAt))
          .fontSize(10)
          .fontColor(COLORS.muted)
      }
      .width('100%')
      Text(volunteer.title)
        .width('100%')
        .fontSize(18)
        .fontWeight(FontWeight.Bold)
        .fontColor(COLORS.ink)
      Text(volunteer.body)
        .width('100%')
        .fontSize(13)
        .lineHeight(20)
        .fontColor(COLORS.muted)
      if (volunteer.contact) {
        Text(`联系说明：${volunteer.contact}`)
          .width('100%')
          .fontSize(12)
          .fontColor(COLORS.green)
      }
      Button(this.applyingVolunteerId === volunteer.id ? '收起报名' : '我要报名')
        .width('100%')
        .height(42)
        .fontSize(13)
        .fontWeight(FontWeight.Bold)
        .fontColor(COLORS.white)
        .backgroundColor(COLORS.green)
        .onClick(() => this.openVolunteerForm(volunteer))

      if (this.applyingVolunteerId === volunteer.id) {
        Column({ space: 9 }) {
          TextInput({ placeholder: '姓名', text: this.applicantName })
            .height(44)
            .backgroundColor(COLORS.white)
            .borderRadius(10)
            .border({ width: 1, color: COLORS.line })
            .padding({ left: 11, right: 11 })
            .onChange((value: string) => this.applicantName = value)
          TextInput({ placeholder: '手机号或其他联系方式', text: this.applicantPhone })
            .height(44)
            .backgroundColor(COLORS.white)
            .borderRadius(10)
            .border({ width: 1, color: COLORS.line })
            .padding({ left: 11, right: 11 })
            .onChange((value: string) => this.applicantPhone = value)
          TextArea({ placeholder: '想说的话（选填）', text: this.applicantNote })
            .height(76)
            .backgroundColor(COLORS.white)
            .borderRadius(10)
            .border({ width: 1, color: COLORS.line })
            .padding(10)
            .onChange((value: string) => this.applicantNote = value)
          Button(this.actionId === volunteer.id ? '正在提交…' : '提交报名')
            .width('100%')
            .height(42)
            .fontSize(13)
            .fontColor(COLORS.white)
            .backgroundColor(COLORS.night)
            .enabled(this.actionId.length === 0)
            .onClick(() => this.submitVolunteerApplication(volunteer))
        }
        .width('100%')
        .padding(12)
        .backgroundColor('#F4F2ED')
        .borderRadius(12)
      }
    }
    .width('100%')
    .padding(15)
    .backgroundColor(COLORS.paper)
    .borderRadius(18)
    .border({ width: 1, color: COLORS.line })
  }

  @Builder
  private adminApplicationCard(application: VolunteerApplication) {
    Column({ space: 8 }) {
      Row() {
        Text(application.volunteerTitle)
          .fontSize(14)
          .fontWeight(FontWeight.Bold)
          .fontColor(COLORS.ink)
          .layoutWeight(1)
        Text(application.status === 'pending' ? '待审核' :
          (application.status === 'approved' ? '已通过' : '已拒绝'))
          .fontSize(11)
          .fontColor(application.status === 'approved' ? COLORS.green :
            (application.status === 'rejected' ? COLORS.danger : '#72551E'))
      }
      .width('100%')
      Text(`${application.name} · ${application.phone}`)
        .width('100%')
        .fontSize(13)
        .fontColor(COLORS.ink)
      if (application.note.length > 0) {
        Text(application.note)
          .width('100%')
          .fontSize(12)
          .fontColor(COLORS.muted)
      }
      if (application.status === 'pending') {
        Row({ space: 8 }) {
          Button('通过')
            .layoutWeight(1)
            .height(36)
            .fontSize(12)
            .fontColor(COLORS.white)
            .backgroundColor(COLORS.green)
            .enabled(this.actionId.length === 0)
            .onClick(() => this.reviewApplication(application, 'approved'))
          Button('拒绝')
            .layoutWeight(1)
            .height(36)
            .fontSize(12)
            .fontColor(COLORS.danger)
            .backgroundColor(COLORS.dangerLight)
            .enabled(this.actionId.length === 0)
            .onClick(() => this.reviewApplication(application, 'rejected'))
        }
        .width('100%')
      }
    }
    .width('100%')
    .padding(12)
    .backgroundColor('#F7F5EF')
    .borderRadius(12)
    .border({ width: 1, color: COLORS.line })
  }

  @Builder
  private volunteerComposerCard() {
    Column({ space: 10 }) {
      Row() {
        Text('发布义工招募')
          .fontSize(17)
          .fontWeight(FontWeight.Bold)
          .fontColor(COLORS.ink)
        Blank()
        Button('收起')
          .height(30)
          .fontSize(11)
          .fontColor(COLORS.green)
          .backgroundColor(COLORS.greenLight)
          .onClick(() => this.showVolunteerComposer = false)
      }
      .width('100%')
      TextInput({ placeholder: '招募标题', text: this.volunteerTitle })
        .height(44)
        .backgroundColor(COLORS.white)
        .borderRadius(10)
        .border({ width: 1, color: COLORS.line })
        .padding({ left: 11, right: 11 })
        .onChange((value: string) => this.volunteerTitle = value)
      TextArea({ placeholder: '招募内容', text: this.volunteerBody })
        .height(90)
        .backgroundColor(COLORS.white)
        .borderRadius(10)
        .border({ width: 1, color: COLORS.line })
        .padding(10)
        .onChange((value: string) => this.volunteerBody = value)
      TextInput({ placeholder: '联系说明', text: this.volunteerContact })
        .height(44)
        .backgroundColor(COLORS.white)
        .borderRadius(10)
        .border({ width: 1, color: COLORS.line })
        .padding({ left: 11, right: 11 })
        .onChange((value: string) => this.volunteerContact = value)
      if (this.volunteerCoverUri.length > 0) {
        Image(this.volunteerCoverUri)
          .width('100%')
          .height(140)
          .objectFit(ImageFit.Cover)
          .borderRadius(10)
      }
      Row({ space: 8 }) {
        Button(this.volunteerCoverUri.length > 0 ? '已选封面' : '选择封面')
          .layoutWeight(1)
          .height(40)
          .fontSize(12)
          .fontColor(COLORS.green)
          .backgroundColor(COLORS.greenLight)
          .onClick(() => this.chooseVolunteerCover())
        Button(this.publishing ? '正在发布…' : '发布招募')
          .layoutWeight(1)
          .height(40)
          .fontSize(12)
          .fontColor(COLORS.white)
          .backgroundColor(COLORS.green)
          .enabled(!this.publishing)
          .onClick(() => this.publishVolunteer())
      }
      .width('100%')
    }
    .width('100%')
    .padding(14)
    .backgroundColor(COLORS.paper)
    .borderRadius(18)
    .border({ width: 1, color: COLORS.line })
  }

  build() {
    Scroll() {
      Column({ space: 14 }) {
        Row() {
          Column({ space: 4 }) {
            Text('人文社区')
              .fontSize(27)
              .fontWeight(FontWeight.Bold)
              .fontColor(COLORS.ink)
            Text('分享记忆，也把善意留给彼此')
              .fontSize(13)
              .fontColor(COLORS.muted)
          }
          .alignItems(HorizontalAlign.Start)
          .layoutWeight(1)
          Button(this.showComposer ? '编辑中' : '发布')
            .height(38)
            .fontSize(13)
            .fontColor(COLORS.white)
            .backgroundColor(COLORS.green)
            .onClick(() => this.showComposer = true)
        }
        .width('100%')

        StatusBanner({ message: this.message })

        if (this.showComposer) {
          this.composerCard()
        }

        if (this.user.role === 'admin') {
          if (this.showVolunteerComposer) {
            this.volunteerComposerCard()
          } else {
            Button('管理员：发布义工招募')
              .width('100%')
              .height(42)
              .fontSize(13)
              .fontColor('#72551E')
              .backgroundColor(COLORS.amberLight)
              .borderRadius(12)
              .onClick(() => this.showVolunteerComposer = true)
          }
        }

        if (this.volunteers.length > 0) {
          Column({ space: 9 }) {
            Row() {
              Text('社区义工')
                .fontSize(18)
                .fontWeight(FontWeight.Bold)
                .fontColor(COLORS.ink)
              Blank()
              Text(`${this.volunteers.length} 个招募计划`)
                .fontSize(11)
                .fontColor(COLORS.muted)
            }
            .width('100%')
            ForEach(this.volunteers, (volunteer: CommunityVolunteer) => {
              this.volunteerCard(volunteer)
            }, (volunteer: CommunityVolunteer) => volunteer.id)
          }
          .width('100%')
        }

        if (this.user.role === 'admin' && this.applications.length > 0) {
          Column({ space: 9 }) {
            Text('义工报名审核')
              .width('100%')
              .fontSize(18)
              .fontWeight(FontWeight.Bold)
              .fontColor(COLORS.ink)
            ForEach(this.applications, (application: VolunteerApplication) => {
              this.adminApplicationCard(application)
            }, (application: VolunteerApplication) => application.id)
          }
          .width('100%')
          .padding(14)
          .backgroundColor(COLORS.paper)
          .borderRadius(18)
          .border({ width: 1, color: COLORS.line })
        }

        Row() {
          Text('社区动态')
            .fontSize(18)
            .fontWeight(FontWeight.Bold)
            .fontColor(COLORS.ink)
          Blank()
          Button('刷新')
            .height(32)
            .fontSize(12)
            .fontColor(COLORS.green)
            .backgroundColor(COLORS.greenLight)
            .enabled(!this.loading)
            .onClick(() => this.refresh())
        }
        .width('100%')

        if (this.loading) {
          LoadingState({ text: '正在打开人文社区…' })
        } else if (this.posts.length === 0) {
          EmptyState({
            title: '还没有社区动态',
            description: '写下第一段故事，让温柔从这里开始。'
          })
        } else {
          ForEach(this.posts, (post: CommunityPost) => {
            this.postCard(post)
          }, (post: CommunityPost) => post.id)
        }
      }
      .width('100%')
      .constraintSize({ maxWidth: 760 })
      .padding({ left: 16, right: 16, top: 18, bottom: 28 })
    }
    .width('100%')
    .height('100%')
    .scrollBar(BarState.Off)
    .backgroundColor(COLORS.background)
  }
}
