import { existsSync, mkdirSync, readFileSync, readdirSync, renameSync, rmSync, writeFileSync } from "node:fs";
import { randomUUID } from "node:crypto";
import path from "node:path";
import sharp from "sharp";

type AssetMetadata = {
  httpMetadata?: {
    contentType?: string;
  };
  customMetadata?: Record<string, string>;
};

export class LocalAssetBucket {
  private readonly root: string;
  private readonly variantJobs = new Map<string, Promise<void>>();

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

  async get(key: string, options?: { maxDimension?: number; format?: "webp" }) {
    const filePath = this.resolveKey(key);
    if (!existsSync(filePath)) {
      return null;
    }

    const metadataPath = `${filePath}.meta.json`;
    const metadata: AssetMetadata = existsSync(metadataPath)
      ? JSON.parse(readFileSync(metadataPath, "utf8"))
      : {};

    const contentType = metadata.httpMetadata?.contentType || "application/octet-stream";
    const maxDimension = options?.maxDimension;
    if (maxDimension && options?.format === "webp" && contentType.startsWith("image/")) {
      const variantPath = `${filePath}.thumb-${maxDimension}.webp`;
      await this.ensureWebpVariant(filePath, variantPath, maxDimension);
      if (existsSync(variantPath)) {
        return {
          body: readFileSync(variantPath),
          httpMetadata: { contentType: "image/webp" }
        };
      }
    }

    return {
      body: readFileSync(filePath),
      httpMetadata: metadata.httpMetadata || {}
    };
  }

  async delete(key: string) {
    const filePath = this.resolveKey(key);
    rmSync(filePath, { force: true });
    rmSync(`${filePath}.meta.json`, { force: true });
    const directory = path.dirname(filePath);
    const prefix = `${path.basename(filePath)}.thumb-`;
    if (existsSync(directory)) {
      for (const name of readdirSync(directory)) {
        if (name.startsWith(prefix) && name.endsWith(".webp")) {
          rmSync(path.join(directory, name), { force: true });
        }
      }
    }
  }

  private async ensureWebpVariant(sourcePath: string, variantPath: string, maxDimension: number) {
    if (existsSync(variantPath)) return;

    let job = this.variantJobs.get(variantPath);
    if (!job) {
      job = this.createWebpVariant(sourcePath, variantPath, maxDimension)
        .finally(() => this.variantJobs.delete(variantPath));
      this.variantJobs.set(variantPath, job);
    }
    await job;
  }

  private async createWebpVariant(sourcePath: string, variantPath: string, maxDimension: number) {
    const temporaryPath = `${variantPath}.${randomUUID()}.tmp`;
    try {
      const output = await sharp(sourcePath, {
        failOn: "error",
        limitInputPixels: 40_000_000
      })
        .rotate()
        .resize({
          width: maxDimension,
          height: maxDimension,
          fit: "inside",
          withoutEnlargement: true
        })
        .webp({ quality: 82, effort: 4 })
        .toBuffer();
      writeFileSync(temporaryPath, output);
      renameSync(temporaryPath, variantPath);
    } finally {
      rmSync(temporaryPath, { force: true });
    }
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
