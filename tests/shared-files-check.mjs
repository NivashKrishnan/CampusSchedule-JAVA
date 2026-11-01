import assert from 'node:assert/strict';
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {spawn} from 'node:child_process';
import {fileURLToPath} from 'node:url';

// Integration checks use disposable local files, two Tomcat processes and a
// conditional-write file server. This does not claim to test Vercel itself.
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const tomcat=process.env.TEST_TOMCAT || 'C:/Users/HP/Downloads/server/apache-tomcat-9.0.123';
const java=process.env.TEST_JAVA || 'C:/Users/HP/Downloads/server/jdk8u504-b01/bin/java.exe';
const work=fs.mkdtempSync(path.join(root,'.local-files-test-'));
const stateFile=path.join(work,'state.bin');
const setupKey=crypto.randomBytes(32).toString('hex');
const password='Integration-'+crypto.randomBytes(16).toString('hex');
const etag=b=>'"'+crypto.createHash('sha256').update(b).digest('hex')+'"';
const bridge=http.createServer(async(req,res)=>{
  if(req.url!=='/state'){res.writeHead(404).end();return;}
  if(req.method==='GET'){
    if(!fs.existsSync(stateFile)){res.writeHead(404).end();return;}
    const bytes=fs.readFileSync(stateFile);res.setHeader('ETag',etag(bytes));res.end(bytes);return;
  }
  const chunks=[];for await(const c of req)chunks.push(c);
  const old=fs.existsSync(stateFile)?fs.readFileSync(stateFile):null;
  if(old ? req.headers['if-match']!==etag(old) : Boolean(req.headers['if-match'])){res.writeHead(409).end();return;}
  const bytes=Buffer.concat(chunks);fs.writeFileSync(stateFile+'.new',bytes);fs.renameSync(stateFile+'.new',stateFile);res.setHeader('ETag',etag(bytes));res.end();
});
await new Promise((resolve,reject)=>bridge.once('error',reject).listen(8181,'127.0.0.1',resolve));
const children=[];
function launch(port){
  const base=path.join(work,'tomcat-'+port);fs.mkdirSync(base,{recursive:true});
  for(const dir of ['conf','logs','temp','work','webapps'])fs.mkdirSync(path.join(base,dir),{recursive:true});
  fs.cpSync(path.join(tomcat,'conf'),path.join(base,'conf'),{recursive:true});
  fs.copyFileSync(path.join(root,'Timetable.war'),path.join(base,'webapps','Timetable.war'));
  fs.writeFileSync(path.join(base,'conf','server.xml'),`<Server port="-1"><Service name="Catalina"><Connector address="127.0.0.1" port="${port}" maxPostSize="65536"/><Engine name="Catalina" defaultHost="localhost"><Host name="localhost" appBase="webapps" autoDeploy="false"><Valve className="org.apache.catalina.valves.ErrorReportValve" showReport="false" showServerInfo="false"/></Host></Engine></Service></Server>`);
  const log=fs.openSync(path.join(base,'server.log'),'a');
  const child=spawn(java,[`-Dcatalina.home=${tomcat}`,`-Dcatalina.base=${base}`,`-Djava.io.tmpdir=${base}/temp`,'-cp',`${tomcat}/bin/bootstrap.jar;${tomcat}/bin/tomcat-juli.jar`,'org.apache.catalina.startup.Bootstrap','start'],{env:{...process.env,TIMETABLE_STORAGE:'files',TIMETABLE_PUBLIC_DEPLOYMENT:'false',TIMETABLE_SETUP_KEY:setupKey},stdio:['ignore',log,log],windowsHide:true});children.push(child);return child;
}
async function ready(port){for(let i=0;i<60;i++){try{const res=await fetch(`http://127.0.0.1:${port}/Timetable/app`);if(res.status===200)return;}catch{}await new Promise(r=>setTimeout(r,500));}throw new Error('Tomcat startup failed; inspect '+work);}
class Client{
  constructor(port=18081){this.port=port;this.cookies={};}
  async request(query='',form){const response=await fetch(`http://127.0.0.1:${this.port}/Timetable/app${query}`,{method:form?'POST':'GET',headers:{Cookie:Object.entries(this.cookies).map(([k,v])=>`${k}=${v}`).join('; '),...(form?{'Content-Type':'application/x-www-form-urlencoded'}:{})},body:form?new URLSearchParams(form):undefined,redirect:'manual'});for(const c of response.headers.getSetCookie()){const [k,v]=c.split(';')[0].split('=');this.cookies[k]=v;}return {status:response.status,location:response.headers.get('location'),text:await response.text()};}
  async page(page='dashboard'){const out=await this.request('?page='+page);assert.equal(out.status,200,out.text.slice(0,150));return out.text;}
  async post(form,page='dashboard',expected=302){const html=await this.page(page);const rev=html.match(/name="revision" value="([^"]*)"/)?.[1].replaceAll('&quot;','"').replaceAll('&amp;','&');const out=await this.request('',{csrf:this.cookies.csCsrf,revision:rev||'',...form});assert.equal(out.status,expected,out.text.slice(0,300));return out;}
  async login(username,role){await this.request();const out=await this.request('',{action:'login',csrf:this.cookies.csCsrf,username,password,loginRole:role});assert.equal(out.status,302);assert.ok(this.cookies.csAuth,'No authentication cookie');}
}
let passes=0;
function pass(s){console.log('PASS '+s);passes++;}
try{
  let first=launch(18081);launch(18082);await Promise.all([ready(18081),ready(18082)]);
  const admin=new Client();await admin.request();
  assert.equal((await admin.request('',{action:'setup',csrf:admin.cookies.csCsrf,setup_key:'wrong',username:'owner',password,confirm_password:password})).status,403);
  assert.equal((await admin.request('',{action:'setup',csrf:admin.cookies.csCsrf,setup_key:setupKey,username:'owner',password,confirm_password:password})).status,302);
  assert.ok(admin.cookies.csAuth);pass('owner setup requires private key and closes after creation');
  assert.equal((await admin.request('',{action:'setup',csrf:admin.cookies.csCsrf,setup_key:setupKey,username:'other',password,confirm_password:password})).status,403);
  const second=new Client(18082);second.cookies={...admin.cookies};assert.match(await second.page(),/Your week|A clear schedule/);pass('signed login works across separate server instances');
  await admin.post({action:'save',entity:'departments',name:'Computing'});
  await admin.post({action:'save',entity:'classes',name:'II AI and DS',department_id:'1',student_count:'67'});
  await admin.post({action:'save',entity:'classrooms',name:'MIC F 216',capacity:'70',type:'Theory'});
  await admin.post({action:'save',entity:'classrooms',name:'Computer Lab',capacity:'70',type:'Lab'});
  for(let i=1;i<=6;i++){
    await admin.post({action:'save',entity:'faculty',name:'Teacher '+i,max_hours:'48',unavailable:''});
    await admin.post({action:'save',entity:'subjects',name:['DMS','COA','DS','OS','AJP','Java Lab'][i-1],class_id:'1',faculty_id:''+i,weekly_hours:'8',type:i===6?'Lab':'Theory'});
  }
  pass('academic record creation and cross-device retrieval');assert.match(await second.page('subjects'),/Java Lab/);
  for(const page of ['dashboard','generate','saved','departments','faculty','classes','classrooms','subjects','reports','availability','settings','users','activity','account','help'])await admin.page(page);
  pass('all 15 existing workspace pages render');
  await admin.post({action:'save',entity:'departments',name:'Temporary'});await admin.post({action:'save',entity:'departments',id:'2',name:'Edited'});assert.match(await second.page('departments'),/Edited/);await admin.post({action:'delete',entity:'departments',id:'2'});await admin.post({action:'delete',entity:'departments',id:'1'},'departments',400);pass('edit/delete and linked-record protection');
  const stale=await admin.page('departments');const staleRev=stale.match(/name="revision" value="([^"]*)"/)[1].replaceAll('&quot;','"');await second.post({action:'save',entity:'departments',name:'Second user change'});
  assert.equal((await admin.request('',{action:'save',entity:'departments',name:'Stale change',csrf:admin.cookies.csCsrf,revision:staleRev})).status,409);pass('stale form edits rejected without overwriting newer changes');
  assert.equal((await admin.request('',{action:'save',entity:'departments',name:'CSRF attack',csrf:'wrong'})).status,403);pass('CSRF rejected');
  const generated=await admin.post({action:'generate',name:'Complete teaching week',class_id:'1',days:'6',periods:'8',coverage:'full'});
  assert.match(generated.location,/id=1$/);const timetable=await admin.page('timetable&id=1');assert.equal((timetable.match(/class="lesson"/g)||[]).length,48);assert.ok(!timetable.includes('class="free"'));assert.equal((await admin.request('?page=export&id=1')).text.trim().split('\n').length,49);pass('full 48-period generation, saved retrieval and CSV');
  await admin.page('timetable&id=1&view=teacher');await admin.page('timetable&id=1&view=room');await admin.page('reports&id=1');pass('teacher/room views and workload reports');
  const csv=(await admin.request('?page=export&id=1')).text.trim().split('\n');const lesson=csv[9].split(',');const day=['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'].indexOf(lesson[1])+1;
  await admin.post({action:'moveLesson',id:'1',entry_id:'9',day_no:''+day,period_no:lesson[2],room_id:'2'});await admin.post({action:'moveLesson',id:'1',entry_id:'9',day_no:'1',period_no:'1',room_id:'2'},'dashboard',400);pass('lesson editing accepts valid changes and rejects clashes');
  await admin.post({action:'availability',faculty_id:'1',blocked:day+':'+lesson[2]});await admin.post({action:'moveLesson',id:'1',entry_id:'9',day_no:''+day,period_no:lesson[2],room_id:'1'},'dashboard',400);await admin.post({action:'availability',faculty_id:'1'});pass('teacher availability enforced during lesson editing');
  await admin.post({action:'settings',institution:'Test College',academic_year:'2026-2027',days:'6',periods:'8',start_time:'09:00',period_minutes:'50',break_after:'4',break_minutes:'55'});assert.match(await second.page('settings'),/Test College/);pass('shared settings persist');
  await admin.post({action:'cloneRun',id:'1'});await admin.post({action:'renameRun',id:'2',name:'Duplicate draft'});await admin.post({action:'deleteRun',id:'2'});pass('duplicate, rename and delete draft');
  await admin.post({action:'publishRun',id:'1'});
  await admin.post({action:'createUser',username:'student1',password,role:'student',class_id:'1'});await admin.post({action:'createUser',username:'teacher1',password,role:'teacher',faculty_id:'1'});
  const student=new Client(18082),teacher=new Client();await student.login('student1','student');await teacher.login('teacher1','teacher');
  assert.equal((await student.request('?page=users')).status,403);assert.equal((await student.page('timetable&id=1')).match(/class="lesson"/g).length,48);assert.equal((await teacher.page('timetable&id=1')).match(/class="lesson"/g).length,8);await student.post({action:'deleteRun',id:'1'},'dashboard',403);pass('teacher/student logins and role-scoped published schedules');
  await admin.post({action:'deleteRun',id:'1'},'dashboard',400);pass('published timetable deletion protected');
  const applicant=new Client();await applicant.page('register');await applicant.request('',{action:'register',csrf:applicant.cookies.csCsrf,username:'newstudent',role:'student',class_id:'1',password,confirm_password:password});
  await applicant.request('',{action:'login',csrf:applicant.cookies.csCsrf,username:'newstudent',loginRole:'student',password});assert.ok(!applicant.cookies.csAuth);await admin.post({action:'userStatus',id:'4'});await applicant.login('newstudent','student');pass('registration requires administrator approval');
  await admin.post({action:'userStatus',id:'2'});assert.match((await student.request()).text,/Choose your login/);pass('disabling an account invalidates its signed sessions');
  const oldTeacher=new Client(18082);oldTeacher.cookies={...teacher.cookies};await teacher.post({action:'account',current_password:password,new_password:password+'new',confirm_password:password+'new'},'account');assert.match((await oldTeacher.request()).text,/Choose your login/);assert.match(await teacher.page(),/teacher/);pass('password change invalidates older sessions across servers');
  const replay=new Client(18082);replay.cookies={...teacher.cookies};await teacher.post({action:'logout'});assert.match((await replay.request()).text,/Choose your login/);pass('sign-out revokes all device sessions');
  first.kill();await new Promise(r=>first.once('exit',r));first=launch(18081);await ready(18081);assert.match(await admin.page('saved'),/Complete teaching week/);pass('data and owner authentication survive server restart');
  const before=fs.readFileSync(stateFile);const wrong=await fetch('http://127.0.0.1:8181/state',{method:'PUT',headers:{'If-Match':'"stale"'},body:before});assert.equal(wrong.status,409);assert.deepEqual(fs.readFileSync(stateFile),before);pass('storage-level compare-and-swap prevents lost updates');
  console.log(`TOTAL ${passes} integration checks passed`);
  console.log('Disposable verification files: '+work);
} finally {for(const child of children)if(child.exitCode===null)child.kill();bridge.close();}
