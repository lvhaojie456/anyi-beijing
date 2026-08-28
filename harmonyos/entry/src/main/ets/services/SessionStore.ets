import { common } from '@kit.AbilityKit';
import { preferences } from '@kit.ArkData';
import { AppUser } from '../models/Models';

const STORE_NAME: string = 'anyi_memorial_app';
const KEY_USER_ID: string = 'session_user_id';
const KEY_USER_NAME: string = 'session_username';
const KEY_DISPLAY_NAME: string = 'session_display_name';
const KEY_USER_ROLE: string = 'session_role';
const KEY_USER_AVATAR: string = 'session_avatar_url';
const KEY_AUTH_TOKEN: string = 'session_token';

export class SessionStore {
  private store: preferences.Preferences;

  constructor(context: common.Context) {
    this.store = preferences.getPreferencesSync(context, { name: STORE_NAME });
  }

  read(): AppUser | undefined {
    const token: string = this.stringValue(KEY_AUTH_TOKEN);
    const id: string = this.stringValue(KEY_USER_ID);
    const username: string = this.stringValue(KEY_USER_NAME);
    if (token.length === 0 || id.length === 0 || username.length === 0) {
      return undefined;
    }
    const displayName: string = this.stringValue(KEY_DISPLAY_NAME) || username;
    const role: string = this.stringValue(KEY_USER_ROLE) || 'user';
    return new AppUser(id, username, displayName, role, token, this.stringValue(KEY_USER_AVATAR));
  }

  save(user: AppUser): void {
    this.store.putSync(KEY_USER_ID, user.id);
    this.store.putSync(KEY_USER_NAME, user.username);
    this.store.putSync(KEY_DISPLAY_NAME, user.displayName);
    this.store.putSync(KEY_USER_ROLE, user.role);
    this.store.putSync(KEY_AUTH_TOKEN, user.token);
    this.store.putSync(KEY_USER_AVATAR, user.avatarUrl);
    this.flush();
  }

  clear(): void {
    this.store.clearSync();
    this.flush();
  }

  private stringValue(key: string): string {
    const value: preferences.ValueType = this.store.getSync(key, '');
    return typeof value === 'string' ? value : '';
  }

  private flush(): void {
    this.store.flush().catch(() => {
      console.error('Anyi session preferences flush failed');
    });
  }
}
