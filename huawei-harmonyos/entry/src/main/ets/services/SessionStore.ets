import { common } from '@kit.AbilityKit';
import { preferences } from '@kit.ArkData';
import { asset } from '@kit.AssetStoreKit';
import { AppUser } from '../models/Models';

const STORE_NAME: string = 'anyi_memorial_app';
const KEY_USER_ID: string = 'session_user_id';
const KEY_USER_NAME: string = 'session_username';
const KEY_DISPLAY_NAME: string = 'session_display_name';
const KEY_USER_ROLE: string = 'session_role';
const KEY_USER_AVATAR: string = 'session_avatar_url';
const KEY_LEGACY_AUTH_TOKEN: string = 'session_token';
const TOKEN_ALIAS: string = 'anyi_memorial_session_token';

export class SessionStore {
  private store: preferences.Preferences;

  constructor(context: common.Context) {
    this.store = preferences.getPreferencesSync(context, { name: STORE_NAME });
  }

  read(): AppUser | undefined {
    let token: string = SecureTokenStore.read();
    if (token.length === 0) {
      token = this.stringValue(KEY_LEGACY_AUTH_TOKEN);
      if (token.length > 0) {
        try {
          SecureTokenStore.write(token);
          this.store.deleteSync(KEY_LEGACY_AUTH_TOKEN);
          this.flush();
        } catch (_) {
          this.store.deleteSync(KEY_LEGACY_AUTH_TOKEN);
          this.flush();
          return undefined;
        }
      }
    }
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
    SecureTokenStore.write(user.token);
    this.store.putSync(KEY_USER_ID, user.id);
    this.store.putSync(KEY_USER_NAME, user.username);
    this.store.putSync(KEY_DISPLAY_NAME, user.displayName);
    this.store.putSync(KEY_USER_ROLE, user.role);
    this.store.putSync(KEY_USER_AVATAR, user.avatarUrl);
    this.flush();
  }

  clear(): void {
    SecureTokenStore.clear();
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

class SecureTokenStore {
  static read(): string {
    try {
      const query: asset.AssetMap = new Map();
      query.set(asset.Tag.ALIAS, asciiBytes(TOKEN_ALIAS));
      query.set(asset.Tag.RETURN_TYPE, asset.ReturnType.ALL);
      query.set(asset.Tag.RETURN_LIMIT, 1);
      const result: Array<asset.AssetMap> = asset.querySync(query);
      const secret: asset.Value | undefined = result[0]?.get(asset.Tag.SECRET);
      return secret instanceof Uint8Array ? asciiString(secret) : '';
    } catch (_) {
      return '';
    }
  }

  static write(token: string): void {
    const attributes: asset.AssetMap = new Map();
    attributes.set(asset.Tag.ALIAS, asciiBytes(TOKEN_ALIAS));
    attributes.set(asset.Tag.SECRET, asciiBytes(token));
    attributes.set(asset.Tag.ACCESSIBILITY, asset.Accessibility.DEVICE_FIRST_UNLOCKED);
    attributes.set(asset.Tag.SYNC_TYPE, asset.SyncType.NEVER);
    attributes.set(asset.Tag.REQUIRE_ATTR_ENCRYPTED, true);
    attributes.set(asset.Tag.CONFLICT_RESOLUTION, asset.ConflictResolution.OVERWRITE);
    asset.addSync(attributes);
  }

  static clear(): void {
    try {
      const query: asset.AssetMap = new Map();
      query.set(asset.Tag.ALIAS, asciiBytes(TOKEN_ALIAS));
      asset.removeSync(query);
    } catch (_) {
      // Clearing an already absent asset is idempotent.
    }
  }
}

function asciiBytes(value: string): Uint8Array {
  const bytes: Uint8Array = new Uint8Array(value.length);
  for (let index: number = 0; index < value.length; index++) {
    bytes[index] = value.charCodeAt(index) & 0x7f;
  }
  return bytes;
}

function asciiString(value: Uint8Array): string {
  let result: string = '';
  for (let index: number = 0; index < value.length; index++) {
    result += String.fromCharCode(value[index]);
  }
  return result;
}
