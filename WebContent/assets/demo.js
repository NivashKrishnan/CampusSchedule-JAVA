(() => {
'use strict';
// Synthetic presentation data only; no accounts, network requests or storage.
const subjects = [
 ['DMS','Discrete Mathematical Structures','Teacher A','Room 201'],
 ['COA','Computer Organization and Architecture','Teacher B','Room 201'],
 ['DS','Data Structures','Teacher C','Room 201'],
 ['OS','Operating Systems','Teacher D','Room 201'],
 ['AJP','Advanced Java Programming','Teacher E','Room 201'],
 ['DS Lab','Data Structures Laboratory','Teacher C','Computer Lab'],
 ['OS Lab','Operating Systems Laboratory','Teacher D','Computer Lab'],
 ['AJP Lab','Advanced Java Laboratory','Teacher E','Computer Lab'],
 ['Skills','Soft Skills Development','Teacher F','Room 201'],
 ['Mentor','Mentor Hour','Teacher A','Room 201']
];
const week = [
 ['OS','AJP Lab','AJP Lab','AJP Lab','COA','OS Lab','OS Lab','OS Lab'],
 ['DS','DS','DMS','DMS','OS','COA','COA','Skills'],
 ['DS','AJP','DMS','DMS','AJP Lab','AJP Lab','AJP Lab','OS'],
 ['DMS','COA','OS','OS','DS Lab','DS Lab','DS Lab','DMS'],
 ['COA','Mentor','Skills','Skills','DS Lab','DS Lab','DS Lab','AJP'],
 ['Mentor','OS','AJP','COA','DMS','DS','AJP','COA']
];
const days=['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];
const times=['09:00–09:50','09:50–10:40','10:55–11:45','11:45–12:35','13:30–14:20','14:20–15:10','15:10–16:00','16:00–16:50'];
const titles={dashboard:'Overview',timetable:'Sample timetable',subjects:'Subjects',teachers:'Teachers',rooms:'Rooms & labs',help:'Demo guide'};
const esc=v=>String(v).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const hours=code=>week.flat().filter(x=>x===code).length;
function table(headers,rows){
 return '<div class="table-wrap"><table><thead><tr>'+headers.map(x=>'<th scope="col">'+esc(x)+'</th>').join('')+'</tr></thead><tbody>'+rows.map(row=>'<tr>'+row.map(x=>'<td>'+esc(x)+'</td>').join('')+'</tr>').join('')+'</tbody></table></div>';
}
function records(title,headers,rows){
 return '<section class="panel"><h2>'+title+'</h2><p class="demo-caption">Illustrative records for this demonstration.</p><label class="demo-label">Search sample records<input id="demo-search" type="search" placeholder="Type to filter"></label>'+table(headers,rows)+'<p id="search-empty" hidden>No matching sample records.</p></section>';
}
function overview(){
 return '<section class="hero"><div><span class="eyebrow">PLAN THE WEEK WITH CONFIDENCE</span><h2>A clear schedule.<br>Room for every class.</h2><p>Explore the timetable project without creating an account or connecting a database.</p><a class="button light" href="#timetable">Open the sample timetable →</a></div><div class="hero-calendar" aria-hidden="true"><div>WEEKLY PLANNER</div><div class="mini-grid">'+Array.from({length:25},(_,i)=>'<span class="cell c'+i%4+'"></span>').join('')+'</div><small>Class · Teacher · Room</small></div></section>'+
 '<section class="stats">'+[['teachers','Teachers',6],['timetable','Sample class',1],['subjects','Subjects & activities',10],['rooms','Rooms & labs',2],['timetable','Filled periods',48]].map(([p,label,n])=>'<a class="stat" href="#'+p+'"><span>'+label+'</span><strong>'+n+'</strong><small>View sample →</small></a>').join('')+'</section>'+
 '<div class="two-col"><section class="panel"><h2>Ready to present</h2><ol class="steps"><li><strong>Explore the records</strong><p>View sample subjects, teachers and rooms.</p></li><li><strong>Review the weekly plan</strong><p>See six days with eight filled teaching periods each.</p></li><li><strong>Print the timetable</strong><p>Use the print button to print or save as PDF.</p></li></ol></section><section class="panel"><h2>About this demonstration</h2><p>This is a read-only preview using an example timetable. It does not run the scheduling algorithm or save changes.</p><p>The original Java application includes account access, scheduling and database storage. Those functions require the full setup.</p><a href="#help">View the presentation guide →</a></section></div>';
}
function timetable(){
 return '<section class="panel timetable-panel"><div class="result-heading"><h2>AI &amp; DS — Sample weekly timetable</h2><p class="demo-caption">Illustrative teaching plan · 6 days × 8 periods · All 48 teaching periods filled</p></div><p class="demo-note">Morning break: 10:40–10:55 · Lunch: 12:35–13:30. Teacher and room names are sample records.</p><div class="demo-actions no-print"><button class="primary" id="print-demo">Print / Save PDF</button><a class="button secondary" href="#subjects">View subjects</a></div><div class="table-wrap"><table class="timetable"><thead><tr><th scope="col">Day</th>'+times.map((t,i)=>'<th scope="col">Period '+(i+1)+'<span class="period-time">'+t+'</span></th>').join('')+'</tr></thead><tbody>'+week.map((day,i)=>'<tr><th scope="row">'+days[i]+'</th>'+day.map(code=>{const s=subjects.find(x=>x[0]===code);return '<td><div class="lesson"><strong>'+esc(s[0])+'</strong><span>'+esc(s[2])+'</span><small>'+esc(s[3])+'</small></div></td>';}).join('')+'</tr>').join('')+'</tbody></table></div><p class="demo-note">Sample presentation only. This plan is not imported from the college database or generated live.</p></section>';
}
function render(){
 let page=location.hash.slice(1);if(!Object.prototype.hasOwnProperty.call(titles,page))page='dashboard';
 document.getElementById('page-title').textContent=titles[page];
 document.title=titles[page]+' | CampusSchedule demo';
 document.querySelectorAll('nav .nav').forEach(a=>{const active=a.hash==='#'+page;a.classList.toggle('active',active);if(active)a.setAttribute('aria-current','page');else a.removeAttribute('aria-current');});
 let html='';
 if(page==='dashboard')html=overview();
 if(page==='timetable')html=timetable();
 if(page==='subjects')html=records('Sample subjects and activities',['Code','Subject / activity','Teacher','Periods in sample week'],subjects.map(s=>[s[0],s[1],s[2],hours(s[0])]));
 if(page==='teachers')html=records('Sample teachers',['Teacher','Subjects / activities','Periods in sample week'],Array.from(new Set(subjects.map(s=>s[2]))).map(t=>[t,subjects.filter(s=>s[2]===t).map(s=>s[0]).join(', '),subjects.filter(s=>s[2]===t).reduce((n,s)=>n+hours(s[0]),0)]));
 if(page==='rooms')html=records('Sample teaching spaces',['Room','Type','Used by'],[['Room 201','Classroom','AI & DS — Sample class'],['Computer Lab','Laboratory','AJP Lab, OS Lab, DS Lab']]);
 if(page==='help')html='<section class="panel guide"><h2>Show the project in three steps</h2><ol class="steps"><li><strong>Open Overview</strong><p>Explain how classes, subjects, teachers and rooms fit into a teaching week.</p></li><li><strong>Open Sample timetable</strong><p>Show the filled periods, laboratory blocks and break times. Print the page if you need a copy.</p></li><li><strong>Explain the full application</strong><p>The original Java app uses JDBC and a database for separate user accounts and saved schedules. This presentation uses synthetic data and does not demonstrate database connectivity or live timetable generation.</p></li></ol><p>No password is needed here. Refreshing restores the same example. No registration, editing or database writes are performed.</p></section>';
 document.getElementById('view').innerHTML=html;
 const search=document.getElementById('demo-search');
 if(search)search.addEventListener('input',()=>{let count=0;document.querySelectorAll('#view tbody tr').forEach(row=>{row.hidden=!row.textContent.toLowerCase().includes(search.value.toLowerCase());if(!row.hidden)count++;});document.getElementById('search-empty').hidden=count!==0;});
 const print=document.getElementById('print-demo');if(print)print.addEventListener('click',()=>window.print());
}
window.addEventListener('hashchange',render);render();
})();
