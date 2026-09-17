// Local integration smoke: isolated database, real worker packaging, actual WebGL model load.
import { createHash, randomBytes, randomUUID } from 'node:crypto';
import { mkdtemp, mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { spawn } from 'node:child_process';
import { createRequire } from 'node:module';
import { serve } from '@hono/node-server';
import app from '../dist-node/src/index.js';
import { openSqliteDatabase,migrateSqlite,SqliteDatabaseAdapter } from '../dist-node/server/sqlite-db.js';
import { LocalAssetBucket } from '../dist-node/server/local-assets.js';

const generate=process.argv.includes('--generate');
const [pipelineArg,workspaceArg,pythonArg,playwrightArg]=process.argv.slice(2).filter(a=>a!=='--generate');
if(!pipelineArg||!workspaceArg||!pythonArg) throw new Error('Usage: node scripts/smoke-live2d.mjs PIPELINE EXISTING_VERIFIED_OUTPUT PYTHON [PLAYWRIGHT_PACKAGE]');
const pipeline=path.resolve(pipelineArg),workspace=path.resolve(workspaceArg);
const tempRoot=path.resolve('../.tmp');await mkdir(tempRoot,{recursive:true});
const root=await mkdtemp(path.join(tempRoot,'live2d-smoke-'));
const raw=openSqliteDatabase(path.join(root,'smoke.sqlite'));migrateSqlite(raw,path.resolve('migrations'));
const token=randomBytes(32).toString('hex');
const env={DB:new SqliteDatabaseAdapter(raw),ASSETS:new LocalAssetBucket(path.join(root,'uploads')),
  AUTH_SECRET:randomBytes(32).toString('hex'),LIVE2D_ENABLED:'true',LIVE2D_WORKER_TOKEN:token,RATE_LIMIT_ENABLED:'false'};
const server=serve({hostname:'127.0.0.1',port:0,fetch:r=>app.fetch(r,env)});
await new Promise(resolve=>server.once('listening',resolve));
const url=`http://127.0.0.1:${server.address().port}`;
let browser;
try {
  async function request(pathname,options={},auth='') {
    const headers=new Headers(options.headers);if(auth)headers.set('Authorization','Bearer '+auth);
    const response=await fetch(url+pathname,{...options,headers});
    const result=await response.json();if(!response.ok)throw new Error(`${response.status}: ${JSON.stringify(result)}`);return result;
  }
  const image=await readFile(path.join(workspace,'01_reference.png'));
  const form=new FormData();
  for(const [key,value] of Object.entries({username:'smoke_'+randomUUID().slice(0,8),password:randomBytes(16).toString('hex'),displayName:'本地接入测试',gender:'男',acceptedTerms:'true',acceptedPrivacy:'true'}))form.set(key,value);
  form.set('file',new File([image],'reference.png',{type:'image/png'}));
  const owner=await request('/auth/register',{method:'POST',body:form});
  const {companion}=await request('/ai/companions',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({displayName:'老爷爷联调样例',relation:'朋友'})},owner.token);
  const input=new FormData();input.set('file',new File([image],'reference.png',{type:'image/png'}));
  const {job}=await request(`/ai/companions/${companion.id}/live2d/jobs`,{method:'POST',headers:{'Idempotency-Key':randomUUID()},body:input},owner.token);
  const code=`import sys,os,json\nfrom pathlib import Path\nsys.path.insert(0,sys.argv[1]+'/scripts')\nfrom anyi_worker import Api,collect_delivery\na=Api(os.environ['ANYI_API_URL'],os.environ['ANYI_WORKER_TOKEN'])\nj=a.call('/internal/live2d/jobs/claim',{})['job']\nf=collect_delivery(Path(sys.argv[2]),Path(sys.argv[3]))\nfor n,p in f.items():\n a.call('/internal/live2d/jobs/'+j['id']+'/heartbeat',{'stage':'uploading','progress':96},j['leaseToken'])\n a.upload(j,n,p)\na.call('/internal/live2d/jobs/'+j['id']+'/complete',{},j['leaseToken'])\nprint('Published verified fixture through actual worker protocol')`;
  const workerArgs=generate ? [path.join(pipeline,'scripts/anyi_worker.py'),'--once','--work-dir',path.join(root,'worker')]
    : ['-c',code,pipeline,workspace,path.join(root,'delivery')];
  const child=spawn(pythonArg,workerArgs,{env:{...process.env,ANYI_API_URL:url,ANYI_WORKER_TOKEN:token},stdio:'inherit'});
  await new Promise((resolve,reject)=>{child.on('error',reject);child.on('exit',code=>code===0?resolve():reject(new Error('worker packaging failed')));});
  const status=await request(`/ai/live2d/jobs/${job.id}`,{},owner.token);
  await request(`/ai/live2d/jobs/${job.id}/activate`,{method:'POST'},owner.token);
  const bound=await request(`/ai/companions/${companion.id}`,{},owner.token);
  const project=await fetch(url+status.job.projectPath,{headers:{Authorization:'Bearer '+owner.token}});
  if(!project.ok)throw new Error('Project download failed');
  const zip=Buffer.from(await project.arrayBuffer());
  const report={mode:generate ? 'real worker: new image-input generation through API, GPU, export and upload' : 'existing verified model; no generation API called',jobStatus:status.job.status,modelId:bound.companion.live2dModel,
    projectBytes:zip.length,projectSha256:createHash('sha256').update(zip).digest('hex'),browser:[]};
  if(playwrightArg){
    const require=createRequire(import.meta.url);const {chromium}=require(playwrightArg);
    browser=await chromium.launch({channel:'chrome',headless:true,args:['--enable-unsafe-swiftshader']});
    for(const viewport of [{width:390,height:700},{width:1200,height:800}]){
      const page=await browser.newPage({viewport});const failures=[];page.on('pageerror',e=>failures.push(e.message));
      await page.addInitScript(()=>{window.events=[];window.AndroidBridge={onPageReady:()=>window.events.push('page'),onReady:id=>window.events.push(id),onError:e=>window.events.push('ERROR:'+e)};});
      await page.route('**/*',async route=>{
        const parsed=new URL(route.request().url());
        if(parsed.hostname!=='appassets.androidplatform.net')return route.abort();
        const generated=`/generated-live2d/${job.id}/`;
        if(parsed.pathname.startsWith(generated)){
          const response=await fetch(`${url}/ai/live2d/jobs/${job.id}/files/${parsed.pathname.slice(generated.length)}`,{headers:{Authorization:'Bearer '+owner.token}});
          return route.fulfill({status:response.status,headers:{'Content-Type':response.headers.get('Content-Type')||'application/octet-stream'},body:Buffer.from(await response.arrayBuffer())});
        }
        if(!parsed.pathname.startsWith('/assets/live2d/'))return route.abort();
        const asset=path.resolve('../app/src/main/assets/live2d',parsed.pathname.slice('/assets/live2d/'.length));
        const body=await readFile(asset);
        await route.fulfill({body,contentType:asset.endsWith('.html')?'text/html':asset.endsWith('.js')?'text/javascript':'application/octet-stream'});
      });
      await page.goto('https://appassets.androidplatform.net/assets/live2d/index.html');
      await page.waitForFunction(()=>window.events.includes('page'));
      await page.evaluate(id=>window.anyiLive2d.load('generated:'+id,location.origin+'/generated-live2d/'+id+'/runtime/model.model3.json'),job.id);
      await page.waitForFunction(id=>window.events.some(e=>e==='generated:'+id||e.startsWith('ERROR:')),job.id,{timeout:60000});
      const events=await page.evaluate(()=>window.events);
      if(events.some(e=>e.startsWith('ERROR:')))throw new Error(events.join(';'));
      await page.waitForTimeout(1200);
      const pixels=await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>{
        const canvas=document.querySelector('canvas');const gl=canvas.getContext('webgl2')||canvas.getContext('webgl');
        const data=new Uint8Array(canvas.width*canvas.height*4);gl.readPixels(0,0,canvas.width,canvas.height,gl.RGBA,gl.UNSIGNED_BYTE,data);
        let visible=0;for(let i=3;i<data.length;i+=4)if(data[i]>20)visible++;resolve({visible,total:canvas.width*canvas.height});
      })));
      // Read inside an animation frame before the compositor discards the drawing buffer.
      if(pixels.visible<2000)throw new Error('Empty WebGL drawing buffer');
      const screenshot=await page.screenshot({path:path.join(root,`preview-${viewport.width}.png`)});
      const sharp=require('sharp');const {data,info}=await sharp(screenshot).raw().toBuffer({resolveWithObject:true});
      let colored=0;for(let i=0;i<data.length;i+=info.channels)if(Math.max(data[i],data[i+1],data[i+2])-Math.min(data[i],data[i+1],data[i+2])>12)colored++;
      if(colored<2000)throw new Error('Blank Live2D screenshot');
      await page.evaluate(()=>window.anyiLive2d.speak('测试动态形象与口型'));
      await page.waitForTimeout(700);
      const moving=await page.screenshot();if(moving.equals(screenshot))throw new Error('Model did not move');
      report.browser.push({viewport,loaded:true,coloredPixels:colored,canvasPixels:pixels,moving:true,errors:failures});
      await page.close();
    }
  }
  await writeFile(path.join(root,'report.json'),JSON.stringify(report,null,2));
  console.log(JSON.stringify({report:path.join(root,'report.json'),...report},null,2));
}finally{if(browser)await browser.close();await new Promise(resolve=>server.close(resolve));raw.close();}
