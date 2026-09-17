// Isolated local API for Android integration; never uses the production database.
import { randomBytes } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const backend=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const directory=path.resolve(backend,'../.tmp/live2d-local');
mkdirSync(directory,{recursive:true,mode:0o700});
const configPath=path.join(directory,'private-config.json');
const config=existsSync(configPath) ? JSON.parse(readFileSync(configPath,'utf8')) : {
  authSecret:randomBytes(32).toString('hex'),workerToken:randomBytes(32).toString('hex')
};
if(!existsSync(configPath))writeFileSync(configPath,JSON.stringify(config),{mode:0o600});
process.env.HOST='127.0.0.1';
process.env.PORT=process.argv[2] || '8789';
process.env.NODE_ENV='development';
process.env.DB_DRIVER='sqlite';
process.env.ANYI_DATA_DIR=directory;
process.env.ANYI_DB_PATH=path.join(directory,'anyi.sqlite');
process.env.ANYI_UPLOADS_DIR=path.join(directory,'uploads');
process.env.ANYI_MIGRATIONS_DIR=path.join(backend,'migrations');
process.env.AUTH_SECRET=config.authSecret;
process.env.LIVE2D_ENABLED='true';
process.env.LIVE2D_WORKER_TOKEN=config.workerToken;
process.env.PUBLIC_ASSET_BASE_URL=`http://127.0.0.1:${process.env.PORT}`;
process.env.ALLOWED_ORIGINS=process.env.PUBLIC_ASSET_BASE_URL;
process.env.TRUST_PROXY='false';
console.log('Local worker configuration is stored privately at '+configPath);
await import('../dist-node/server/server.js');
