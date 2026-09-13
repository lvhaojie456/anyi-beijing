export class AppUser {
  id: string;
  username: string;
  displayName: string;
  role: string;
  token: string;
  avatarUrl: string;

  constructor(
    id: string,
    username: string,
    displayName: string,
    role: string,
    token: string,
    avatarUrl: string = ''
  ) {
    this.id = id;
    this.username = username;
    this.displayName = displayName;
    this.role = role;
    this.token = token;
    this.avatarUrl = avatarUrl;
  }
}

export interface ApiUser {
  id: string;
  username: string;
  displayName: string;
  role: string;
  avatarUrl?: string | null;
}

export interface AuthResponse {
  user: ApiUser;
  token: string;
}

export interface UserResponse {
  user: ApiUser;
}

export interface HealthResponse {
  ok: boolean;
  service: string;
  time: string;
}

export interface ApiErrorPayload {
  error?: string;
}

export interface FlowerOffering {
  type: string;
  until: number;
}

export interface FruitOffering {
  type: string;
  until: number;
}

export interface Memorial {
  id: string;
  name: string;
  imageUrl?: string | null;
  flowerUntil: Array<number>;
  flowerOfferings: Array<FlowerOffering>;
  candleUntil: number;
  candleUntilList: Array<number>;
  incenseUntil: number;
  fruitOfferings: Array<FruitOffering>;
  createdAt: string;
  updatedAt: string;
}

export interface MemorialListResponse {
  memorials: Array<Memorial>;
}

export interface MemorialResponse {
  memorial: Memorial;
}

export interface FlowerResponse {
  flowerUntil: Array<number>;
  flowerOfferings: Array<FlowerOffering>;
}

export interface CandleResponse {
  candleUntil: number;
  candleUntilList: Array<number>;
}

export interface IncenseResponse {
  incenseUntil: number;
}

export interface FruitResponse {
  fruitOfferings: Array<FruitOffering>;
}

export interface CommunityPost {
  id: string;
  authorId: string;
  authorName: string;
  authorUsername: string;
  authorAvatarUrl?: string | null;
  content: string;
  imageUrls: Array<string>;
  likeCount: number;
  commentCount: number;
  likedByMe: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CommunityPostsResponse {
  posts: Array<CommunityPost>;
}

export interface CommunityPostResponse {
  post: CommunityPost;
}

export interface CommunityComment {
  id: string;
  postId: string;
  authorId: string;
  authorName: string;
  authorUsername: string;
  authorAvatarUrl?: string | null;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface CommunityCommentsResponse {
  comments: Array<CommunityComment>;
}

export interface CommunityCommentResponse {
  comment: CommunityComment;
  post: CommunityPost;
}

export interface CommunityVolunteer {
  id: string;
  title: string;
  body: string;
  contact?: string | null;
  imageUrl?: string | null;
  createdAt: string;
}

export interface CommunityVolunteerResponse {
  volunteer: CommunityVolunteer;
  volunteers: Array<CommunityVolunteer>;
}

export interface SingleCommunityVolunteerResponse {
  volunteer: CommunityVolunteer;
}

export interface VolunteerApplication {
  id: string;
  volunteerPostId: string;
  volunteerTitle: string;
  applicantId: string;
  applicantName: string;
  applicantUsername: string;
  name: string;
  phone: string;
  note: string;
  status: string;
  createdAt: string;
}

export interface VolunteerApplicationResponse {
  application: VolunteerApplication;
}

export interface VolunteerApplicationsResponse {
  applications: Array<VolunteerApplication>;
}

export interface DigitalHumanMessage {
  id: string;
  sender: string;
  content: string;
  createdAt: number;
}

export interface AiMemory {
  id: string;
  companionKey: string;
  memoryType: string;
  memoryKey: string;
  content: string;
  confidence: number;
  importance: number;
  createdAt: number;
  updatedAt: number;
  expiresAt?: number | null;
}

export interface DigitalHumanMessagesResponse {
  messages: Array<DigitalHumanMessage>;
}

export interface AiMemoriesResponse {
  memories: Array<AiMemory>;
}

export interface AiMemorySettingsResponse {
  enabled: boolean;
  consentedAt?: string | null;
}

export interface DigitalHumanChatResponse {
  characterId: string;
  model: string;
  message: DigitalHumanMessage;
  memories?: Array<AiMemory>;
}

export interface DigitalHumanConfig {
  enabled: boolean;
  url: string;
}

export interface AppConfigResponse {
  digitalHuman: DigitalHumanConfig;
}

export interface Asset {
  url: string;
  key?: string;
}

export interface AssetResponse {
  asset: Asset;
}

export interface OkResponse {
  ok?: boolean;
  deleted?: boolean;
}

export interface UnlockResponse {
  feature: string;
  unlocked: boolean;
}

export function appUserFromAuth(response: AuthResponse): AppUser {
  const value: string = response.user.avatarUrl ? response.user.avatarUrl : '';
  return new AppUser(
    response.user.id,
    response.user.username,
    response.user.displayName || response.user.username,
    response.user.role || 'user',
    response.token,
    value
  );
}

export function appUserFromProfile(response: UserResponse, token: string): AppUser {
  const value: string = response.user.avatarUrl ? response.user.avatarUrl : '';
  return new AppUser(
    response.user.id,
    response.user.username,
    response.user.displayName || response.user.username,
    response.user.role || 'user',
    token,
    value
  );
}
