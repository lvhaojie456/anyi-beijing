import { fileIo as fs } from '@kit.CoreFileKit';
import { http } from '@kit.NetworkKit';
import { API_BASE_URLS, MAX_IMAGE_BYTES } from '../common/Constants';
import {
  ApiErrorPayload,
  AiMemoriesResponse,
  AiMemorySettingsResponse,
  AppConfigResponse,
  AssetResponse,
  AuthResponse,
  CandleResponse,
  CommunityCommentResponse,
  CommunityCommentsResponse,
  CommunityPostResponse,
  CommunityPostsResponse,
  CommunityVolunteerResponse,
  DigitalHumanChatResponse,
  DigitalHumanMessagesResponse,
  FlowerResponse,
  FruitResponse,
  HealthResponse,
  IncenseResponse,
  MemorialListResponse,
  MemorialResponse,
  OkResponse,
  SingleCommunityVolunteerResponse,
  UnlockResponse,
  UserResponse,
  VolunteerApplicationResponse,
  VolunteerApplicationsResponse
} from '../models/Models';

interface RegisterPayload {
  username: string;
  password: string;
  displayName: string;
}

interface LoginPayload {
  username: string;
  password: string;
}

interface ProfilePayload {
  displayName: string;
  avatarUrl: string | null;
}

interface MemorialPayload {
  name: string;
  imageUrl: string | null;
}

interface OfferingPayload {
  type: string;
}

interface CommunityPostPayload {
  content: string;
  imageUrls: Array<string>;
}

interface ContentPayload {
  content: string;
}

interface DigitalHumanPayload {
  characterId: string;
  message: string;
}

interface VolunteerApplicationPayload {
  name: string;
  phone: string;
  note: string;
}

interface CommunityVolunteerPayload {
  title: string;
  body: string;
  contact: string;
  imageUrl: string | null;
}

interface VolunteerReviewPayload {
  status: string;
}

interface ImageUploadFormat {
  contentType: string;
  extension: string;
}

export class ApiError extends Error {
  statusCode: number;
  code: string;

  constructor(statusCode: number, code: string) {
    super(code);
    this.statusCode = statusCode;
    this.code = code;
  }
}

export class AnyiApiClient {
  private tokenProvider: () => string;

  constructor(tokenProvider: () => string = () => '') {
    this.tokenProvider = tokenProvider;
  }

  health(): Promise<HealthResponse> {
    return this.request<HealthResponse>(http.RequestMethod.GET, '/health');
  }

  appConfig(): Promise<AppConfigResponse> {
    return this.request<AppConfigResponse>(http.RequestMethod.GET, '/app/config');
  }

  register(username: string, password: string, displayName: string): Promise<AuthResponse> {
    const payload: RegisterPayload = { username, password, displayName };
    return this.request<AuthResponse>(http.RequestMethod.POST, '/auth/register', payload);
  }

  login(username: string, password: string): Promise<AuthResponse> {
    const payload: LoginPayload = { username, password };
    return this.request<AuthResponse>(http.RequestMethod.POST, '/auth/login', payload);
  }

  currentUser(): Promise<UserResponse> {
    return this.request<UserResponse>(http.RequestMethod.GET, '/me', undefined, true);
  }

  updateCurrentUser(displayName: string, avatarUrl: string): Promise<UserResponse> {
    const payload: ProfilePayload = {
      displayName,
      avatarUrl: avatarUrl.length > 0 ? avatarUrl : null
    };
    return this.request<UserResponse>(http.RequestMethod.PUT, '/me', payload, true);
  }

  deleteAccount(): Promise<OkResponse> {
    return this.request<OkResponse>(http.RequestMethod.DELETE, '/me', undefined, true);
  }

  listMemorials(): Promise<MemorialListResponse> {
    return this.request<MemorialListResponse>(http.RequestMethod.GET, '/memorials', undefined, true);
  }

  createMemorial(name: string, imageUrl: string): Promise<MemorialResponse> {
    const payload: MemorialPayload = { name, imageUrl: imageUrl.length > 0 ? imageUrl : null };
    return this.request<MemorialResponse>(http.RequestMethod.POST, '/memorials', payload, true);
  }

  updateMemorial(id: string, name: string, imageUrl: string): Promise<MemorialResponse> {
    const payload: MemorialPayload = { name, imageUrl: imageUrl.length > 0 ? imageUrl : null };
    return this.request<MemorialResponse>(http.RequestMethod.PUT, `/memorials/${id}`, payload, true);
  }

  offerFlower(memorialId: string): Promise<FlowerResponse> {
    const payload: OfferingPayload = { type: 'wreath' };
    return this.request<FlowerResponse>(http.RequestMethod.POST, `/memorials/${memorialId}/flowers`, payload, true);
  }

  lightCandle(memorialId: string): Promise<CandleResponse> {
    return this.request<CandleResponse>(http.RequestMethod.POST, `/memorials/${memorialId}/candle`, undefined, true);
  }

  lightIncense(memorialId: string): Promise<IncenseResponse> {
    return this.request<IncenseResponse>(http.RequestMethod.POST, `/memorials/${memorialId}/incense`, undefined, true);
  }

  offerFruit(memorialId: string, type: string): Promise<FruitResponse> {
    const payload: OfferingPayload = { type };
    return this.request<FruitResponse>(http.RequestMethod.POST, `/memorials/${memorialId}/fruits`, payload, true);
  }

  featureUnlocked(feature: string): Promise<UnlockResponse> {
    return this.request<UnlockResponse>(http.RequestMethod.GET, `/feature-unlocks/${feature}`, undefined, true);
  }

  unlockFeature(feature: string): Promise<UnlockResponse> {
    return this.request<UnlockResponse>(http.RequestMethod.POST, `/feature-unlocks/${feature}`, undefined, true);
  }

  listCommunityPosts(): Promise<CommunityPostsResponse> {
    return this.request<CommunityPostsResponse>(http.RequestMethod.GET, '/community/posts', undefined, true);
  }

  createCommunityPost(content: string, imageUrls: Array<string>): Promise<CommunityPostResponse> {
    const payload: CommunityPostPayload = { content, imageUrls };
    return this.request<CommunityPostResponse>(http.RequestMethod.POST, '/community/posts', payload, true);
  }

  deleteCommunityPost(postId: string): Promise<OkResponse> {
    return this.request<OkResponse>(http.RequestMethod.DELETE, `/community/posts/${postId}`, undefined, true);
  }

  likeCommunityPost(postId: string): Promise<CommunityPostResponse> {
    return this.request<CommunityPostResponse>(http.RequestMethod.POST, `/community/posts/${postId}/like`, undefined, true);
  }

  listCommunityPostComments(postId: string): Promise<CommunityCommentsResponse> {
    return this.request<CommunityCommentsResponse>(http.RequestMethod.GET,
      `/community/posts/${postId}/comments`, undefined, true);
  }

  createCommunityPostComment(postId: string, content: string): Promise<CommunityCommentResponse> {
    const payload: ContentPayload = { content };
    return this.request<CommunityCommentResponse>(http.RequestMethod.POST,
      `/community/posts/${postId}/comments`, payload, true);
  }

  deleteCommunityPostComment(postId: string, commentId: string): Promise<CommunityPostResponse> {
    return this.request<CommunityPostResponse>(http.RequestMethod.DELETE,
      `/community/posts/${postId}/comments/${commentId}`, undefined, true);
  }

  communityVolunteerInfo(): Promise<CommunityVolunteerResponse> {
    return this.request<CommunityVolunteerResponse>(http.RequestMethod.GET, '/community/volunteer', undefined, true);
  }

  createCommunityVolunteer(title: string, body: string, contact: string,
    imageUrl: string): Promise<SingleCommunityVolunteerResponse> {
    const payload: CommunityVolunteerPayload = {
      title,
      body,
      contact,
      imageUrl: imageUrl.length > 0 ? imageUrl : null
    };
    return this.request<SingleCommunityVolunteerResponse>(http.RequestMethod.POST,
      '/community/volunteer', payload, true);
  }

  applyForCommunityVolunteer(volunteerId: string, name: string, phone: string,
    note: string): Promise<VolunteerApplicationResponse> {
    const payload: VolunteerApplicationPayload = { name, phone, note };
    return this.request<VolunteerApplicationResponse>(http.RequestMethod.POST,
      `/community/volunteer/${volunteerId}/applications`, payload, true);
  }

  listVolunteerApplications(): Promise<VolunteerApplicationsResponse> {
    return this.request<VolunteerApplicationsResponse>(http.RequestMethod.GET,
      '/community/volunteer/applications?status=all', undefined, true);
  }

  reviewVolunteerApplication(applicationId: string, status: string): Promise<VolunteerApplicationResponse> {
    const payload: VolunteerReviewPayload = { status };
    return this.request<VolunteerApplicationResponse>(http.RequestMethod.PUT,
      `/community/volunteer/applications/${applicationId}`, payload, true);
  }

  digitalHumanChat(characterId: string, message: string): Promise<DigitalHumanChatResponse> {
    const payload: DigitalHumanPayload = { characterId, message };
    return this.request<DigitalHumanChatResponse>(http.RequestMethod.POST,
      '/app/digital-human/chat', payload, true, 60000);
  }

  listDigitalHumanMessages(characterId: string): Promise<DigitalHumanMessagesResponse> {
    return this.request<DigitalHumanMessagesResponse>(http.RequestMethod.GET,
      `/app/digital-human/messages?characterId=${characterId}`, undefined, true);
  }

  listDigitalHumanMemories(characterId: string): Promise<AiMemoriesResponse> {
    return this.request<AiMemoriesResponse>(http.RequestMethod.GET,
      `/app/digital-human/memories?characterId=${characterId}`, undefined, true);
  }

  digitalHumanMemorySettings(): Promise<AiMemorySettingsResponse> {
    return this.request<AiMemorySettingsResponse>(http.RequestMethod.GET,
      '/app/digital-human/memory-settings', undefined, true);
  }

  setDigitalHumanMemoryEnabled(enabled: boolean): Promise<AiMemorySettingsResponse> {
    return this.request<AiMemorySettingsResponse>(http.RequestMethod.PUT,
      '/app/digital-human/memory-settings', { enabled }, true);
  }

  deleteAllDigitalHumanMemories(characterId: string): Promise<OkResponse> {
    return this.request<OkResponse>(http.RequestMethod.DELETE,
      `/app/digital-human/memories?characterId=${characterId}`, undefined, true);
  }

  deleteDigitalHumanMemory(memoryId: string): Promise<OkResponse> {
    return this.request<OkResponse>(http.RequestMethod.DELETE,
      `/app/digital-human/memories/${memoryId}`, undefined, true);
  }

  deleteAllDigitalHumanMemories(characterId: string): Promise<OkResponse> {
    return this.request<OkResponse>(http.RequestMethod.DELETE,
      `/app/digital-human/memories?characterId=${characterId}`, undefined, true);
  }

  async uploadImage(scope: string, uri: string): Promise<AssetResponse> {
    const file: fs.File = fs.openSync(uri, fs.OpenMode.READ_ONLY);
    try {
      const size: number = fs.statSync(file.fd).size;
      if (size <= 0 || size > MAX_IMAGE_BYTES) {
        throw new Error('file_size_invalid');
      }
      const data: ArrayBuffer = new ArrayBuffer(size);
      fs.readSync(file.fd, data);
      const format: ImageUploadFormat = this.detectImageUploadFormat(data);
      return await this.multipartRequest(scope, `image-${Date.now()}${format.extension}`, format.contentType, data);
    } finally {
      fs.closeSync(file.fd);
    }
  }

  private detectImageUploadFormat(data: ArrayBuffer): ImageUploadFormat {
    const bytes: Uint8Array = new Uint8Array(data);
    if (bytes.length >= 3 && bytes[0] === 0xFF && bytes[1] === 0xD8 && bytes[2] === 0xFF) {
      return { contentType: 'image/jpeg', extension: '.jpg' };
    }
    if (bytes.length >= 8 && bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4E &&
      bytes[3] === 0x47 && bytes[4] === 0x0D && bytes[5] === 0x0A && bytes[6] === 0x1A &&
      bytes[7] === 0x0A) {
      return { contentType: 'image/png', extension: '.png' };
    }
    if (bytes.length >= 12 && bytes[0] === 0x52 && bytes[1] === 0x49 && bytes[2] === 0x46 &&
      bytes[3] === 0x46 && bytes[8] === 0x57 && bytes[9] === 0x45 && bytes[10] === 0x42 &&
      bytes[11] === 0x50) {
      return { contentType: 'image/webp', extension: '.webp' };
    }
    throw new ApiError(415, 'unsupported_file_type');
  }

  private async multipartRequest(scope: string, fileName: string, contentType: string,
    data: ArrayBuffer): Promise<AssetResponse> {
    const request: http.HttpRequest = http.createHttp();
    const headers: Record<string, string> = {
      'Accept': 'application/json',
      'Content-Type': 'multipart/form-data',
      'Authorization': `Bearer ${this.tokenProvider()}`
    };
    const options: http.HttpRequestOptions = {
      method: http.RequestMethod.POST,
      header: headers,
      expectDataType: http.HttpDataType.STRING,
      usingCache: false,
      connectTimeout: 15000,
      readTimeout: 120000,
      multiFormDataList: [
        {
          name: 'scope',
          contentType: 'text/plain',
          data: scope
        },
        {
          name: 'file',
          contentType,
          data,
          remoteFileName: fileName
        }
      ]
    };
    try {
      const response: http.HttpResponse = await request.request(`${API_BASE_URLS[0]}/assets`, options);
      return this.parseResponse<AssetResponse>(response);
    } finally {
      request.destroy();
    }
  }

  private async request<T>(method: http.RequestMethod, path: string, body: Object | undefined = undefined,
    authorized: boolean = false, readTimeout: number = 30000): Promise<T> {
    let lastError: Error = new Error('request_failed');
    for (let index: number = 0; index < API_BASE_URLS.length; index++) {
      const request: http.HttpRequest = http.createHttp();
      const headers: Record<string, string> = {
        'Accept': 'application/json',
        'Content-Type': 'application/json; charset=utf-8'
      };
      if (authorized && this.tokenProvider().length > 0) {
        headers['Authorization'] = `Bearer ${this.tokenProvider()}`;
      }
      const options: http.HttpRequestOptions = {
        method,
        header: headers,
        expectDataType: http.HttpDataType.STRING,
        usingCache: false,
        connectTimeout: 15000,
        readTimeout
      };
      if (body !== undefined) {
        options.extraData = JSON.stringify(body);
      }
      try {
        const response: http.HttpResponse = await request.request(`${API_BASE_URLS[index]}${path}`, options);
        return this.parseResponse<T>(response);
      } catch (error) {
        lastError = error as Error;
      } finally {
        request.destroy();
      }
    }
    throw lastError;
  }

  private parseResponse<T>(response: http.HttpResponse): T {
    const text: string = typeof response.result === 'string' ? response.result : JSON.stringify(response.result);
    if (response.responseCode < 200 || response.responseCode >= 300) {
      let code: string = 'request_failed';
      if (text.length > 0) {
        const payload: ApiErrorPayload = JSON.parse(text) as ApiErrorPayload;
        code = payload.error ? payload.error : code;
      }
      throw new ApiError(response.responseCode, code);
    }
    return JSON.parse(text.length > 0 ? text : '{}') as T;
  }
}

export function userFriendlyError(error: Error, fallback: string): string {
  const code: string = error instanceof ApiError ? error.code : error.message;
  switch (code) {
    case 'invalid_username':
      return '账号需为 3–32 位小写字母、数字或下划线';
    case 'username_exists':
      return '账号已存在，请直接登录';
    case 'weak_password':
      return '密码至少需要 8 位';
    case 'invalid_credentials':
      return '账号或密码不正确';
    case 'missing_token':
    case 'invalid_token':
    case 'invalid_token_user':
      return '登录状态已过期，请重新登录';
    case 'flower_limit_reached':
      return '当前已有 2 份鲜花，冷却结束后再献花';
    case 'candle_limit_reached':
      return '当前已有 2 根蜡烛，燃尽后可继续点亮';
    case 'incense_active':
      return '香火仍在延续，请稍后再敬香';
    case 'apple_offering_limit_reached':
      return '当前已有 3 个苹果，稍后可继续供奉';
    case 'durian_offering_requires_payment':
      return '榴莲供品需要先完成解锁';
    case 'durian_offering_limit_reached':
      return '当前已有 1 个榴莲，稍后可继续供奉';
    case 'community_post_empty':
      return '请输入内容或选择图片';
    case 'community_post_delete_forbidden':
      return '只能删除自己发布的动态';
    case 'file_size_invalid':
      return '图片为空或超过 20MB';
    case 'unsupported_file_type':
      return '仅支持 JPEG、PNG 或 WebP 图片';
    case 'upload_content_rejected':
      return '上传内容未通过审核，请更换图片';
    case 'rate_limit_exceeded':
      return '操作太频繁，请稍后再试';
    default:
      return code.length > 0 && code !== 'request_failed' ? `${fallback}：${code}` : fallback;
  }
}
