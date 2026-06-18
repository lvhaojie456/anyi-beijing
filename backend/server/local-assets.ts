import { existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import path from "node:path";

type AssetMetadata = {
  httpMetadata?: {
    contentType?: string;
  };
  customMetadata?: Record<string, string>;
};

export class LocalAssetBucket {
  private readonly root: string;

  constructor(root: string) {
    this.root = path.resolve(root);
    mkdirSync(this.root, { recursive: true });
  }

  async put(key: string, value: ArrayBuffer | Uint8Array | Buffer, options?: AssetMetadata) {
    const filePath = this.resolveKey(key);
    mkdirSync(path.dirname(filePath), { recursive: true });
    writeFileSync(filePath, Buffer.from(value instanceof ArrayBuffer ? new Uint8Array(value) : value));
    writeFileSync(`${filePath}.meta.json`, JSON.stringify(options || {}, null, 2));
  }

  async get(key: string) {
    const filePath = this.resolveKey(key);
    if (!existsSync(filePath)) {
      return null;
    }

    const metadataPath = `${filePath}.meta.json`;
    const metadata: AssetMetadata = existsSync(metadataPath)
      ? JSON.parse(readFileSync(metadataPath, "utf8"))
      : {};

    return {
      body: readFileSync(filePath),
      httpMetadata: metadata.httpMetadata || {}
    };
  }

  async delete(key: string) {
    const filePath = this.resolveKey(key);
    rmSync(filePath, { force: true });
    rmSync(`${filePath}.meta.json`, { force: true });
  }

  private resolveKey(key: string) {
    const normalized = key.replace(/\\/g, "/").replace(/^\/+/g, "");
    const filePath = path.resolve(this.root, normalized);
    if (filePath !== this.root && !filePath.startsWith(`${this.root}${path.sep}`)) {
      throw new Error("asset_key_outside_root");
    }
    return filePath;
  }
}
