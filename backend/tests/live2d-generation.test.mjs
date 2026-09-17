import assert from 'node:assert/strict';
import { createHash, randomUUID } from 'node:crypto';
import { mkdtemp, rm } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import app from '../dist-node/src/index.js';
import { safeLive2dPath, validateRuntime } from '../dist-node/src/live2d.js';
import { openSqliteDatabase, migrateSqlite, SqliteDatabaseAdapter } from '../dist-node/server/sqlite-db.js';

const png=Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9ZlSAAAAAASUVORK5CYII=','base64');

test('runtime rejects external and traversal references',()=>{
  for (const name of ['../secret','/absolute','a/../secret','https://host/model','a\\b','a/%2e%2e/b']) assert.throws(()=>safeLive2dPath(name));
  const document={Version:3,FileReferences:{Moc:'model.moc3',Textures:['texture.png']}};
  assert.throws(()=>validateRuntime(document,new Set(['runtime/model.moc3'])));
  assert.doesNotThrow(()=>validateRuntime(document,new Set(['runtime/model.moc3','runtime/texture.png'])));
  document.FileReferences.Motions={Idle:[{File:'idle.motion3.json',Sound:'https://host/voice'}]};
  assert.throws(()=>validateRuntime(document,new Set()));
});

test('Live2D queue ownership, idempotency, lease fencing, validation and binding',async()=>{
  const root=await mkdtemp(path.join(os.tmpdir(),'anyi-live2d-'));
  const raw=openSqliteDatabase(path.join(root,'db.sqlite'));
  migrateSqlite(raw,path.resolve('migrations'));
  const objects=new Map();
  const workerToken='test-live2d-worker-secret-'.repeat(3);
  const env={DB:new SqliteDatabaseAdapter(raw),ASSETS:{
    async put(k,v,options){objects.set(k,{body:new Uint8Array(v),httpMetadata:options?.httpMetadata});},
    async get(k){return objects.get(k)||null;},async delete(k){objects.delete(k);}
  },AUTH_SECRET:'test-only-anyi-live2d-auth-secret-2026',RATE_LIMIT_ENABLED:'false',LIVE2D_ENABLED:'true',LIVE2D_WORKER_TOKEN:workerToken};
  async function req(url,{token='',method='GET',body,lease,key,headers={}}={}) {
    const h=new Headers(headers);
    if(token) h.set('Authorization','Bearer '+token);
    if(lease) h.set('X-Live2d-Lease',lease);
    if(key) h.set('Idempotency-Key',key);
    if(body && !(body instanceof FormData)){h.set('Content-Type','application/json');body=JSON.stringify(body);}
    const r=await app.fetch(new Request('http://localhost'+url,{method,headers:h,body}),env);
    const bytes=new Uint8Array(await r.arrayBuffer());
    return {status:r.status,data:r.headers.get('Content-Type')?.includes('json')?JSON.parse(new TextDecoder().decode(bytes)):bytes};
  }
  async function register(name) {
    const body=new FormData();
    for(const [k,v] of Object.entries({username:name,password:'Live2dTestPassword2026',displayName:name,gender:'男',acceptedTerms:'true',acceptedPrivacy:'true'}))body.set(k,v);
    body.set('file',new File([png],'avatar.png',{type:'image/png'}));
    const result=await req('/auth/register',{method:'POST',body});
    assert.equal(result.status,201,JSON.stringify(result.data));return result.data.token;
  }
  async function submit(companion,token,key,prompt='全身人物',withImage=false) {
    const body=new FormData();body.set('prompt',prompt);
    if(withImage)body.set('file',new File([png],'source.png',{type:'image/png'}));
    return req(`/ai/companions/${companion}/live2d/jobs`,{token,method:'POST',body,key});
  }
  try {
    const owner=await register('live2d_owner');
    const other=await register('live2d_other');
    const created=await req('/ai/companions',{token:owner,method:'POST',body:{displayName:'测试人物',relation:'朋友'}});
    const companion=created.data.companion.id;
    assert.equal((await req('/internal/live2d/jobs/claim',{token:owner,method:'POST',body:{}})).status,401);
    const key=randomUUID();
    const results=await Promise.all([submit(companion,owner,key,'全身人物',true),submit(companion,owner,key,'全身人物',true)]);
    assert.ok(results.every(r=>[200,202].includes(r.status)),JSON.stringify(results));
    const id=results[0].data.job.id;
    assert.equal(id,results[1].data.job.id);
    assert.equal((await submit(companion,owner,key,'另一个人物')).status,409);
    assert.equal((await submit(companion,other,randomUUID())).status,404);
    assert.equal((await req(`/ai/live2d/jobs/${id}`,{token:other})).status,404);
    const claimed=await req('/internal/live2d/jobs/claim',{token:workerToken,method:'POST',body:{}});
    const lease=claimed.data.job.leaseToken;
    assert.equal((await req('/internal/live2d/jobs/claim',{token:workerToken,method:'POST',body:{}})).data.job,null);
    assert.deepEqual(Buffer.from((await req(claimed.data.job.inputPath,{token:workerToken,lease})).data),png);
    assert.equal((await req(`/internal/live2d/jobs/${id}/heartbeat`,{token:workerToken,lease:'old',method:'POST',body:{stage:'rigging',progress:80}})).status,409);
    assert.equal((await req(`/internal/live2d/jobs/${id}/complete`,{token:workerToken,lease,method:'POST',body:{}})).status,422);
    await req(`/ai/live2d/jobs/${id}/cancel`,{token:owner,method:'POST',body:{}});
    assert.equal((await req(`/internal/live2d/jobs/${id}/heartbeat`,{token:workerToken,lease,method:'POST',body:{stage:'rigging',progress:80}})).status,409);
    await req(`/ai/live2d/jobs/${id}/retry`,{token:owner,method:'POST',body:{}});
    const again=await req('/internal/live2d/jobs/claim',{token:workerToken,method:'POST',body:{}});
    const currentLease=again.data.job.leaseToken;
    assert.notEqual(currentLease,lease);
    const artifacts={
      'runtime/model.model3.json':Buffer.from(JSON.stringify({Version:3,FileReferences:{Moc:'model.moc3',Textures:['texture.png']}})),
      'runtime/model.moc3':Buffer.from('MOC3fixture'), 'runtime/texture.png':png, 'preview.png':png,
      'project.zip':Buffer.from([80,75,3,4,0,0,0,0]),
      'validation.json':Buffer.from(JSON.stringify({corePassed:true,motionPassed:true,psdPassed:true}))
    };
    async function upload(name,bytes,leaseValue=currentLease,sha=null){
      const body=new FormData();body.set('file',new File([bytes],'blob'));
      return req(`/internal/live2d/jobs/${id}/artifacts?name=${encodeURIComponent(name)}`,{token:workerToken,method:'POST',body,lease:leaseValue,
        headers:{'X-Content-SHA256':sha||createHash('sha256').update(bytes).digest('hex')}});
    }
    assert.equal((await upload('../escape',png)).status,400);
    assert.equal((await upload('preview.png',png,lease)).status,409);
    assert.equal((await upload('preview.png',png,currentLease,'bad')).status,422);
    for(const [name,bytes] of Object.entries(artifacts))assert.equal((await upload(name,bytes)).status,201,name);
    assert.equal((await upload('preview.png',png)).status,200);
    assert.equal((await req(`/ai/live2d/jobs/${id}/files/preview.png`,{token:owner})).status,404);
    assert.equal((await req(`/internal/live2d/jobs/${id}/complete`,{token:workerToken,lease:currentLease,method:'POST',body:{}})).status,200);
    assert.equal((await req(`/internal/live2d/jobs/${id}/complete`,{token:workerToken,lease:currentLease,method:'POST',body:{}})).status,200);
    assert.equal((await req(`/ai/live2d/jobs/${id}/files/preview.png`,{token:other})).status,404);
    assert.deepEqual(Buffer.from((await req(`/ai/live2d/jobs/${id}/files/preview.png`,{token:owner})).data),png);
    assert.equal((await req(`/ai/live2d/jobs/${id}/activate`,{token:owner,method:'POST',body:{}})).status,200);
    assert.equal((await req(`/ai/companions/${companion}`,{token:owner})).data.companion.live2dModel,'generated:'+id);
    await req(`/ai/companions/${companion}/live2d`,{token:owner,method:'PATCH',body:{live2dModel:'kei'}});
    assert.equal((await req(`/ai/companions/${companion}`,{token:owner})).data.companion.live2dModel,'kei');
    raw.exec("CREATE TRIGGER fail_companion_delete BEFORE DELETE ON ai_companions BEGIN SELECT RAISE(ABORT, 'simulated delete failure'); END");
    const failedDelete=await req(`/ai/companions/${companion}`,{token:owner,method:'DELETE'});
    assert.equal(failedDelete.status,500);
    assert.equal(raw.prepare("SELECT count(*) AS n FROM asset_delete_queue WHERE reason = 'live2d_companion_deleted'").get().n,0);
    assert.equal((await req(`/ai/live2d/jobs/${id}`,{token:owner})).data.job.status,'succeeded');
    raw.exec('DROP TRIGGER fail_companion_delete');
    const queued=await submit(companion,owner,randomUUID());
    assert.equal(queued.status,202);
    const firstClaim=await req('/internal/live2d/jobs/claim',{token:workerToken,method:'POST',body:{}});
    raw.prepare("UPDATE live2d_jobs SET lease_until = '2000-01-01T00:00:00.000Z' WHERE id = ?").run(queued.data.job.id);
    const recovered=await req('/internal/live2d/jobs/claim',{token:workerToken,method:'POST',body:{}});
    assert.equal(recovered.data.job.id,queued.data.job.id);
    assert.notEqual(recovered.data.job.leaseToken,firstClaim.data.job.leaseToken);
    assert.equal((await req(`/internal/live2d/jobs/${queued.data.job.id}/heartbeat`,{token:workerToken,lease:firstClaim.data.job.leaseToken,method:'POST',body:{stage:'planning',progress:15}})).status,409);
    raw.prepare("UPDATE live2d_jobs SET attempts = 3, lease_until = '2000-01-01T00:00:00.000Z' WHERE id = ?").run(queued.data.job.id);
    assert.equal((await req('/internal/live2d/jobs/claim',{token:workerToken,method:'POST',body:{}})).data.job,null);
    assert.equal((await req(`/ai/live2d/jobs/${queued.data.job.id}`,{token:owner})).data.job.errorCode,'worker_unavailable');
    const deletion=await req(`/ai/companions/${companion}`,{token:owner,method:'DELETE'});
    assert.equal(deletion.status,200,JSON.stringify(deletion.data));
    assert.equal((await req(`/ai/live2d/jobs/${id}`,{token:owner})).status,404);
    assert.ok(raw.prepare("SELECT count(*) AS n FROM asset_delete_queue WHERE reason = 'live2d_companion_deleted'").get().n>=7);
  } finally { raw.close();await rm(root,{recursive:true,force:true}); }
});
