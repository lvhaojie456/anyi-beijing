import { createHash, randomUUID, timingSafeEqual } from "node:crypto";
import type { Context, Hono, MiddlewareHandler } from "hono";
import type { AppEnv } from "./index.js";

export class Live2dError extends Error {
  constructor(readonly status: number, readonly code: string) { super(code); }
}

type Job = {
  id: string; user_id: string; companion_id: string; request_id: string; request_hash: string;
  prompt: string; source_key: string | null; status: string; stage: string; progress: number;
  error_code: string | null; lease_token: string | null; lease_until: string | null;
  attempts: number; created_at: string; updated_at: string;
};
type Artifact = { name: string; asset_key: string; sha256: string; size_bytes: number; mime_type: string };
const stages = new Set(["queued", "preparing", "generating", "planning", "decomposing", "expressions", "refining", "rigging", "verifying", "uploading"]);
const uuid = /^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/;
const now = () => new Date().toISOString();
const expires = () => new Date(Date.now() + 120_000).toISOString();
const hash = (value: Uint8Array | string) => createHash("sha256").update(value).digest("hex");

export function live2dEnabled(c: Context<AppEnv>) {
  return c.env.LIVE2D_ENABLED === "true" && (c.env.LIVE2D_WORKER_TOKEN?.length || 0) >= 32;
}

function workerAuthorized(c: Context<AppEnv>) {
  const expected = c.env.LIVE2D_WORKER_TOKEN || "";
  const supplied = c.req.header("Authorization")?.replace(/^Bearer /, "") || "";
  const a=Buffer.from(expected), b=Buffer.from(supplied);
  return a.length >= 32 && a.length === b.length && timingSafeEqual(a,b);
}

export function live2dUploadLimit(c: Context<AppEnv>): number | null {
  return c.req.method === "POST" && /^\/internal\/live2d\/jobs\/[^/]+\/artifacts$/.test(c.req.path) && workerAuthorized(c)
    ? 256 * 1024 * 1024 : null;
}

function safeId(id: string) {
  if (!uuid.test(id)) throw new Live2dError(400, "live2d_invalid_id");
  return id;
}

export function safeLive2dPath(name: unknown): string {
  if (typeof name !== "string" || name.length > 200 || !/^[A-Za-z0-9_.-]+(?:\/[A-Za-z0-9_.-]+)*$/.test(name) ||
      name.split("/").some(p => p === "." || p === "..")) throw new Live2dError(400, "live2d_invalid_artifact_path");
  return name;
}

function artifactType(name: string) {
  if (name === "project.zip") return "application/zip";
  if (name === "preview.png") return "image/png";
  if (name === "preview.gif") return "image/gif";
  if (name === "validation.json") return "application/json";
  if (!name.startsWith("runtime/")) throw new Live2dError(400, "live2d_invalid_artifact_type");
  if (name.endsWith(".moc3")) return "application/octet-stream";
  if (name.endsWith(".png")) return "image/png";
  if (/\.(model3|motion3|physics3|pose3|exp3|cdi3)\.json$/.test(name)) return "application/json";
  throw new Live2dError(400, "live2d_invalid_artifact_type");
}

function checkBytes(bytes: Uint8Array, mime: string) {
  const prefix = Buffer.from(bytes.subarray(0, 8));
  if (mime === "application/json") {
    try { JSON.parse(new TextDecoder().decode(bytes)); } catch { throw new Live2dError(400, "live2d_invalid_json"); }
  } else if (mime === "image/png" && !prefix.equals(Buffer.from([137,80,78,71,13,10,26,10]))) {
    throw new Live2dError(415, "live2d_invalid_png");
  } else if (mime === "image/gif" && !/^GIF8[79]a/.test(prefix.toString())) {
    throw new Live2dError(415, "live2d_invalid_gif");
  } else if (mime === "application/zip" && prefix.readUInt32LE(0) !== 0x04034b50) {
    throw new Live2dError(415, "live2d_invalid_zip");
  } else if (mime === "application/octet-stream" && prefix.subarray(0,4).toString() !== "MOC3") {
    throw new Live2dError(415, "live2d_invalid_moc");
  }
}

async function companion(c: Context<AppEnv>, id: string) {
  const row = await c.env.DB.prepare("SELECT id FROM ai_companions WHERE id = ? AND user_id = ?")
    .bind(safeId(id), c.get("user").id).first();
  if (!row) throw new Live2dError(404, "ai_companion_not_found");
}

async function job(c: Context<AppEnv>, id: string, owned = true) {
  const row = await c.env.DB.prepare(`SELECT * FROM live2d_jobs WHERE id = ?${owned ? " AND user_id = ?" : ""}`)
    .bind(safeId(id), ...(owned ? [c.get("user").id] : [])).first<Job>();
  if (!row) throw new Live2dError(404, "live2d_job_not_found");
  return row;
}

async function leased(c: Context<AppEnv>) {
  const row = await job(c, c.req.param("id") || "", false);
  if (row.status !== "running" || row.lease_token !== c.req.header("X-Live2d-Lease") || !row.lease_until || row.lease_until <= now()) {
    throw new Live2dError(409, "live2d_lease_lost");
  }
  return row;
}

function serialize(row: Job) {
  return {
    id: row.id, companionId: row.companion_id, status: row.status, stage: row.stage,
    progress: row.progress, errorCode: row.error_code, createdAt: row.created_at, updatedAt: row.updated_at,
    refinementRequired: true, editorCompatibility: "unverified",
    modelId: row.status === "succeeded" ? `generated:${row.id}` : null,
    previewPath: row.status === "succeeded" ? `/ai/live2d/jobs/${row.id}/files/preview.png` : null,
    projectPath: row.status === "succeeded" ? `/ai/live2d/jobs/${row.id}/files/project.zip` : null
  };
}

async function storeAsset(c: Context<AppEnv>, row: Job, key: string, bytes: Uint8Array, mime: string) {
  // Generated atlas pixels and hashes must remain byte-for-byte intact.
  await c.env.ASSETS.put(key, bytes, { httpMetadata: { contentType: "application/octet-stream" } });
  try {
    await c.env.DB.prepare(`INSERT INTO assets (id,owner_id,asset_key,url,mime_type,size_bytes,created_at,visibility)
      VALUES (?,?,?,?,?,?,?,'private')`).bind(randomUUID(),row.user_id,key,`/assets/${key}`,mime,bytes.length,now()).run();
  } catch (error) {
    await c.env.ASSETS.delete(key);
    throw error;
  }
}

async function files(c: Context<AppEnv>, row: Job) {
  return (await c.env.DB.prepare("SELECT * FROM live2d_artifacts WHERE job_id = ? AND lease_token = ?")
    .bind(row.id,row.lease_token).all<Artifact>()).results;
}

export function validateRuntime(model: Record<string, any>, names: Set<string>) {
  const refs = model.FileReferences;
  if (model.Version !== 3 || !refs || typeof refs.Moc !== "string" || !refs.Moc.endsWith(".moc3") ||
      !Array.isArray(refs.Textures) || !refs.Textures.length || refs.Textures.length > 8) {
    throw new Live2dError(422,"live2d_model_invalid");
  }
  const required: unknown[] = [refs.Moc, ...refs.Textures];
  for (const key of ["Physics","Pose","DisplayInfo"]) if (refs[key]) required.push(refs[key]);
  if (refs.Expressions) {
    if (!Array.isArray(refs.Expressions)) throw new Live2dError(422,"live2d_model_invalid");
    for (const expression of refs.Expressions) required.push(expression.File);
  }
  for (const motions of Object.values(refs.Motions || {})) {
    if (!Array.isArray(motions)) throw new Live2dError(422,"live2d_model_invalid");
    for (const motion of motions) {
      if (motion.Sound) throw new Live2dError(422,"live2d_external_audio_not_supported");
      required.push(motion.File);
    }
  }
  for (const ref of required) {
    const name = `runtime/${safeLive2dPath(ref)}`;
    artifactType(name);
    if (!names.has(name)) throw new Live2dError(422,"live2d_artifacts_incomplete");
  }
}

export function registerLive2dRoutes(app: Hono<AppEnv>, auth: MiddlewareHandler<AppEnv>, readImage: (v: FormDataEntryValue | null) => Promise<File>) {
  const worker: MiddlewareHandler<AppEnv> = async (c,next) => {
    if (!workerAuthorized(c)) throw new Live2dError(401,"live2d_worker_unauthorized");
    await next();
  };

  app.get("/ai/live2d/config",auth,c => c.json({enabled:live2dEnabled(c), maxImageBytes:8*1024*1024}));
  app.get("/ai/companions/:id/live2d/jobs",auth,async c => {
    await companion(c,c.req.param("id"));
    const rows=await c.env.DB.prepare("SELECT * FROM live2d_jobs WHERE companion_id = ? AND user_id = ? ORDER BY created_at DESC LIMIT 20")
      .bind(c.req.param("id"),c.get("user").id).all<Job>();
    return c.json({jobs:rows.results.map(serialize)});
  });
  app.get("/ai/live2d/jobs/:id",auth,async c => c.json({job:serialize(await job(c,c.req.param("id")))}));

  app.post("/ai/companions/:id/live2d/jobs",auth,async c => {
    if (!live2dEnabled(c)) throw new Live2dError(503,"live2d_not_configured");
    const companionId=c.req.param("id");
    await companion(c,companionId);
    const form=await c.req.formData();
    const requestId=safeId(c.req.header("Idempotency-Key") || "");
    const prompt=String(form.get("prompt") || "").trim();
    if (prompt.length>2000) throw new Live2dError(400,"live2d_prompt_too_long");
    const input=form.get("file");
    const image=input ? await readImage(input) : null;
    if (!prompt && !image) throw new Live2dError(400,"live2d_input_required");
    if (image && image.size>8*1024*1024) throw new Live2dError(413,"live2d_image_too_large");
    const bytes=image ? new Uint8Array(await image.arrayBuffer()) : null;
    const requestHash=hash(JSON.stringify([companionId,prompt,bytes ? hash(bytes) : null]));
    const existing=await c.env.DB.prepare("SELECT * FROM live2d_jobs WHERE user_id = ? AND request_id = ?")
      .bind(c.get("user").id,requestId).first<Job>();
    if (existing) {
      if (existing.request_hash!==requestHash) throw new Live2dError(409,"live2d_request_conflict");
      return c.json({job:serialize(existing)},200);
    }
    const pending=await c.env.DB.prepare("SELECT COUNT(*) AS count FROM live2d_jobs WHERE user_id = ? AND created_at > ?")
      .bind(c.get("user").id,new Date(Date.now()-86400000).toISOString()).first<{count:number}>();
    if ((pending?.count || 0)>=10) throw new Live2dError(429,"live2d_daily_limit");
    const id=randomUUID();
    const row={id,user_id:c.get("user").id,companion_id:companionId} as Job;
    const sourceKey=bytes ? `${row.user_id}/ai/live2d/${companionId}/${id}/source` : null;
    if (bytes) await storeAsset(c,row,sourceKey!,bytes,image!.type);
    try {
      await c.env.DB.prepare(`INSERT INTO live2d_jobs
        (id,user_id,companion_id,request_id,request_hash,prompt,source_key,active_key,created_at,updated_at)
        VALUES (?,?,?,?,?,?,?,?,?,?)`).bind(id,row.user_id,companionId,requestId,requestHash,prompt,sourceKey,companionId,now(),now()).run();
    } catch (error) {
      if (sourceKey) {
        await c.env.ASSETS.delete(sourceKey);
        await c.env.DB.prepare("DELETE FROM assets WHERE asset_key = ?").bind(sourceKey).run();
      }
      const duplicate=await c.env.DB.prepare("SELECT * FROM live2d_jobs WHERE user_id = ? AND request_id = ?")
        .bind(row.user_id,requestId).first<Job>();
      if (duplicate && duplicate.request_hash===requestHash) return c.json({job:serialize(duplicate)},200);
      const active=await c.env.DB.prepare("SELECT id FROM live2d_jobs WHERE active_key = ?").bind(companionId).first();
      if (active) throw new Live2dError(409,"live2d_job_already_active");
      throw error;
    }
    return c.json({job:serialize(await job(c,id))},202);
  });

  app.post("/ai/live2d/jobs/:id/cancel",auth,async c => {
    const row=await job(c,c.req.param("id"));
    await c.env.DB.prepare("UPDATE live2d_jobs SET status = 'cancelled', stage = 'cancelled', active_key = NULL, lease_until = NULL, updated_at = ? WHERE id = ? AND status IN ('queued','running')")
      .bind(now(),row.id).run();
    return c.json({job:serialize(await job(c,row.id))});
  });
  app.post("/ai/live2d/jobs/:id/retry",auth,async c => {
    if (!live2dEnabled(c)) throw new Live2dError(503,"live2d_not_configured");
    const row=await job(c,c.req.param("id"));
    if (!['failed','cancelled'].includes(row.status)) throw new Live2dError(409,"live2d_not_retryable");
    const active=await c.env.DB.prepare("SELECT id FROM live2d_jobs WHERE active_key = ?").bind(row.companion_id).first();
    if (active) throw new Live2dError(409,"live2d_job_already_active");
    try {
      await c.env.DB.prepare("UPDATE live2d_jobs SET status = 'queued', stage = 'queued', progress = 0, error_code = NULL, lease_token = NULL, lease_until = NULL, attempts = 0, active_key = ?, updated_at = ? WHERE id = ? AND status IN ('failed','cancelled')")
        .bind(row.companion_id,now(),row.id).run();
    } catch { throw new Live2dError(409,"live2d_job_already_active"); }
    return c.json({job:serialize(await job(c,row.id))});
  });
  app.post("/ai/live2d/jobs/:id/activate",auth,async c => {
    const row=await job(c,c.req.param("id"));
    if (row.status!=="succeeded") throw new Live2dError(409,"live2d_not_ready");
    await c.env.DB.prepare("UPDATE ai_companions SET live2d_job_id = ?, live2d_model = NULL, updated_at = ? WHERE id = ? AND user_id = ?")
      .bind(row.id,now(),row.companion_id,row.user_id).run();
    return c.json({ok:true,modelId:`generated:${row.id}`});
  });
  app.get("/ai/live2d/jobs/:id/files/*",auth,async c => {
    const row=await job(c,c.req.param("id"));
    if (row.status!=="succeeded") throw new Live2dError(404,"live2d_not_ready");
    const name=safeLive2dPath(decodeURIComponent(c.req.path.split('/files/')[1] || ""));
    const artifact=await c.env.DB.prepare("SELECT * FROM live2d_artifacts WHERE job_id = ? AND lease_token = ? AND name = ?")
      .bind(row.id,row.lease_token,name).first<Artifact>();
    if (!artifact) throw new Live2dError(404,"live2d_file_not_found");
    const object=await c.env.ASSETS.get(artifact.asset_key);
    if (!object) throw new Live2dError(404,"live2d_file_not_found");
    return new Response(object.body,{headers:{'Content-Type':artifact.mime_type,'Cache-Control':'private, no-store',
      ...(name==='project.zip' ? {'Content-Disposition':`attachment; filename="live2d-${row.id}.zip"`} : {})}});
  });

  app.post("/internal/live2d/jobs/claim",worker,async c => {
    if (!live2dEnabled(c)) return c.json({job:null});
    await c.env.DB.prepare("UPDATE live2d_jobs SET status = 'failed', error_code = 'worker_unavailable', active_key = NULL, updated_at = ? WHERE status = 'running' AND lease_until < ? AND attempts >= 3")
      .bind(now(),now()).run();
    for (let attempt=0;attempt<4;attempt++) {
      const row=await c.env.DB.prepare("SELECT * FROM live2d_jobs WHERE status = 'queued' OR (status = 'running' AND lease_until < ? AND attempts < 3) ORDER BY created_at LIMIT 1")
        .bind(now()).first<Job>();
      if (!row) return c.json({job:null});
      const lease=randomUUID();
      const result=await c.env.DB.prepare("UPDATE live2d_jobs SET status = 'running', stage = 'preparing', progress = 1, lease_token = ?, lease_until = ?, attempts = attempts + 1, updated_at = ? WHERE id = ? AND (status = 'queued' OR (status = 'running' AND lease_until < ? AND attempts < 3))")
        .bind(lease,expires(),now(),row.id,now()).run();
      if (Number(result.meta.changes)>0) return c.json({job:{id:row.id,prompt:row.prompt,leaseToken:lease,
        inputPath:row.source_key ? `/internal/live2d/jobs/${row.id}/input` : null}});
    }
    return c.json({job:null});
  });
  app.get("/internal/live2d/jobs/:id/input",worker,async c => {
    const row=await leased(c);
    if (!row.source_key) throw new Live2dError(404,"live2d_input_missing");
    const object=await c.env.ASSETS.get(row.source_key);
    if (!object) throw new Live2dError(404,"live2d_input_missing");
    return new Response(object.body,{headers:{'Content-Type':'application/octet-stream','Cache-Control':'no-store'}});
  });
  app.post("/internal/live2d/jobs/:id/heartbeat",worker,async c => {
    const row=await leased(c);
    const body=await c.req.json();
    if (!stages.has(body.stage) || !Number.isInteger(body.progress) || body.progress<0 || body.progress>99) throw new Live2dError(400,"live2d_invalid_progress");
    const result=await c.env.DB.prepare("UPDATE live2d_jobs SET stage = ?, progress = ?, lease_until = ?, updated_at = ? WHERE id = ? AND lease_token = ? AND status = 'running'")
      .bind(body.stage,Math.max(row.progress,body.progress),expires(),now(),row.id,row.lease_token).run();
    if (!Number(result.meta.changes)) throw new Live2dError(409,"live2d_lease_lost");
    return c.json({ok:true});
  });
  app.post("/internal/live2d/jobs/:id/artifacts",worker,async c => {
    const row=await leased(c);
    const name=safeLive2dPath(c.req.query('name'));
    const mime=artifactType(name);
    const form=await c.req.formData();
    const file=form.get('file');
    const limit=name==='project.zip' ? 240*1024*1024 : 32*1024*1024;
    if (!(file instanceof File) || file.size<8 || file.size>limit) throw new Live2dError(413,"live2d_artifact_size");
    const bytes=new Uint8Array(await file.arrayBuffer());
    const digest=hash(bytes);
    if (digest!==c.req.header('X-Content-SHA256')) throw new Live2dError(422,"live2d_artifact_hash");
    checkBytes(bytes,mime);
    const previous=(await files(c,row)).find(a=>a.name===name);
    if (previous) {
      if (previous.sha256!==digest) throw new Live2dError(409,"live2d_artifact_conflict");
      return c.json({ok:true});
    }
    const uploaded=await files(c,row);
    if (uploaded.length>=100 || uploaded.reduce((sum,a)=>sum+Number(a.size_bytes),0)+bytes.length>350*1024*1024) throw new Live2dError(413,"live2d_package_too_large");
    const key=`${row.user_id}/ai/live2d/${row.companion_id}/${row.id}/${row.lease_token}/${randomUUID()}/${name}`;
    await storeAsset(c,row,key,bytes,mime);
    try {
      await leased(c);
      await c.env.DB.prepare("INSERT INTO live2d_artifacts (job_id,lease_token,name,asset_key,sha256,size_bytes,mime_type) VALUES (?,?,?,?,?,?,?)")
        .bind(row.id,row.lease_token,name,key,digest,bytes.length,mime).run();
      await leased(c);
    } catch (error) {
      await c.env.ASSETS.delete(key);
      await c.env.DB.prepare("DELETE FROM assets WHERE asset_key = ?").bind(key).run();
      await c.env.DB.prepare("DELETE FROM live2d_artifacts WHERE asset_key = ?").bind(key).run();
      throw error;
    }
    return c.json({ok:true},201);
  });
  app.post("/internal/live2d/jobs/:id/complete",worker,async c => {
    const row=await job(c,c.req.param('id'),false);
    if (row.status==='succeeded' && row.lease_token===c.req.header('X-Live2d-Lease')) return c.json({ok:true});
    await leased(c);
    const artifacts=await files(c,row);
    const names=new Set(artifacts.map(a=>a.name));
    for (const name of ['runtime/model.model3.json','preview.png','project.zip','validation.json']) {
      if (!names.has(name)) throw new Live2dError(422,'live2d_artifacts_incomplete');
    }
    async function document(name:string) {
      const object=await c.env.ASSETS.get(artifacts.find(a=>a.name===name)!.asset_key);
      if (!object) throw new Live2dError(422,'live2d_artifacts_incomplete');
      return new Response(object.body).json();
    }
    validateRuntime(await document('runtime/model.model3.json'),names);
    const validation=await document('validation.json');
    if (validation.corePassed!==true || validation.motionPassed!==true || validation.psdPassed!==true) throw new Live2dError(422,'live2d_validation_failed');
    const result=await c.env.DB.prepare("UPDATE live2d_jobs SET status = 'succeeded', stage = 'completed', progress = 100, active_key = NULL, lease_until = NULL, updated_at = ? WHERE id = ? AND lease_token = ? AND status = 'running'")
      .bind(now(),row.id,row.lease_token).run();
    if (!Number(result.meta.changes)) throw new Live2dError(409,'live2d_lease_lost');
    return c.json({ok:true});
  });
  app.post("/internal/live2d/jobs/:id/fail",worker,async c => {
    const row=await leased(c);
    // Keep provider responses, prompts, credentials and local paths out of the client error.
    await c.env.DB.prepare("UPDATE live2d_jobs SET status = 'failed', stage = 'failed', error_code = 'generation_failed', active_key = NULL, lease_until = NULL, updated_at = ? WHERE id = ? AND lease_token = ? AND status = 'running'")
      .bind(now(),row.id,row.lease_token).run();
    return c.json({ok:true});
  });
}
