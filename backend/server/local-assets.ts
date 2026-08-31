import { randomUUID } from "node:crypto";
import {
  existsSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  renameSync,
  rmSync,
  writeFileSync
} from "node:fs";
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

  async put(
    key: string,
    value: ArrayBuffer | Uint8Array | Buffer,
    options?: AssetMetadata
  ) {
    const filePath = this.resolveKey(key);
    mkdirSync(path.dirname(filePath), { recursive: true });
    const input = Buffer.from(value instanceof ArrayBuffer ? new Uint8Array(value) : value);
    const contentType = options?.httpMetadata?.contentType?.toLowerCase() || "";
    const output = contentType.startsWith("image/")
      ? await sanitizeImage(input, contentType)
      : input;
    const temporarySuffix = `${randomUUID()}.tmp`;
    const temporaryFilePath = `${filePath}.${temporarySuffix}`;
    const metadataPath = `${filePath}.meta.json`;
    const temporaryMetadataPath = `${metadataPath}.${temporarySuffix}`;
    try {
      // Publish the data file last. Readers only consider an asset present once
      // that path exists, so a process interruption cannot expose a partial
      // upload or a file without its metadata.
      writeFileSync(temporaryFilePath, output);
      writeFileSync(temporaryMetadataPath, JSON.stringify(options || {}, null, 2));
      renameSync(temporaryMetadataPath, metadataPath);
      renameSync(temporaryFilePath, filePath);
    } finally {
      rmSync(temporaryFilePath, { force: true });
      rmSync(temporaryMetadataPath, { force: true });
    }
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

async function sanitizeImage(input: Buffer, contentType: string) {
  const image = sharp(input, {
    failOn: "error",
    limitInputPixels: 40_000_000,
    sequentialRead: true
  }).rotate();
  switch (contentType) {
    case "image/jpeg":
      return image.jpeg({ quality: 90, mozjpeg: true }).toBuffer();
    case "image/png":
      return image.png({ compressionLevel: 9 }).toBuffer();
    case "image/webp":
      return image.webp({ quality: 88, effort: 4 }).toBuffer();
    default:
      throw new Error("unsupported_image_asset_type");
  }
}
