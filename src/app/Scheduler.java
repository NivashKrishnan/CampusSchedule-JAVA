package app;

import java.util.*;

/** Bounded backtracking scheduler. One request owns all mutable state. */
public final class Scheduler {
    public static final class Lesson {
        public int subject,group,teacher,hours,size;
        public String name,type,unavailable;
        public Lesson(int s,int g,int t,int h,int n,String name,String type,String unavailable) {
            subject=s;group=g;teacher=t;hours=h;size=n;this.name=name;this.type=type;this.unavailable=unavailable;
        }
    }
    public static final class Room {
        public int id,capacity; public String type;
        public Room(int i,int c,String t) {id=i;capacity=c;type=t;}
    }
    public static final class Entry {
        public Lesson lesson;public int day,period,room;
        Entry(Lesson l,int d,int p,int r){lesson=l;day=d;period=p;room=r;}
    }
    private static final class Unit {Lesson l;int occurrence;Unit(Lesson x,int i){l=x;occurrence=i;}}
    private final int days,periods;
    private final List<Room> rooms;
    private final List<Unit> units=new ArrayList<Unit>();
    private final List<Entry> result=new ArrayList<Entry>();
    private final Set<String> busy=new HashSet<String>();
    private final Map<Integer,Integer> last=new HashMap<Integer,Integer>();
    private long nodes,deadline;
    public Scheduler(int d,int p,List<Lesson> lessons,List<Room> rooms) {
        this(d,p,lessons,rooms,false);
    }
    public Scheduler(int d,int p,List<Lesson> lessons,List<Room> rooms,boolean fullWeek) {
        if(d<1||d>6||p<1||p>8)throw new IllegalArgumentException("Use 1–6 days and 1–8 teaching periods.");
        days=d;periods=p;this.rooms=rooms;
        if(lessons.isEmpty())throw new IllegalArgumentException("Add at least one subject before generating a timetable.");
        if(rooms.isEmpty())throw new IllegalArgumentException("Add a classroom before generating a timetable.");
        Map<Integer,Integer> groupHours=new HashMap<Integer,Integer>(),teacherHours=new HashMap<Integer,Integer>();
        for(Lesson l:lessons){
            if(l.hours<1||l.hours>days*periods)throw new IllegalArgumentException("Weekly hours for "+l.name+" exceed available periods.");
            boolean fits=false;for(Room r:rooms)if(suitable(l,r))fits=true;
            if(!fits)throw new IllegalArgumentException("No suitable room has enough seats for "+l.name+".");
            groupHours.put(l.group,groupHours.getOrDefault(l.group,0)+l.hours);
            teacherHours.put(l.teacher,teacherHours.getOrDefault(l.teacher,0)+l.hours);
            for(int i=0;i<l.hours;i++)units.add(new Unit(l,i));
        }
        for(int h:groupHours.values())if(h>days*periods)throw new IllegalArgumentException("A class needs more teaching periods than the selected week provides.");
        if(fullWeek)for(int h:groupHours.values())if(h!=days*periods)throw new IllegalArgumentException("Fill every period requires assigned subject periods to equal the teaching week. Add the missing subject or activity periods, or change the week size.");
        for(int h:teacherHours.values())if(h>days*periods)throw new IllegalArgumentException("A teacher needs more periods than the selected week provides.");
        Collections.sort(units,new Comparator<Unit>(){public int compare(Unit a,Unit b){
            int c=Boolean.compare(!"Lab".equals(a.l.type),!"Lab".equals(b.l.type));
            if(c==0)c=Integer.compare(b.l.size,a.l.size);
            if(c==0)c=Integer.compare(a.l.subject,b.l.subject);
            return c==0?Integer.compare(a.occurrence,b.occurrence):c;
        }});
    }
    private boolean suitable(Lesson l,Room r){return r.capacity>=l.size && (!"Lab".equals(l.type)||"Lab".equals(r.type));}
    private String key(String kind,int id,int slot){return kind+id+":"+slot;}
    public List<Entry> generate(){
        deadline=System.nanoTime()+5_000_000_000L;
        if(!search(0))throw new IllegalArgumentException(nodes>500000||System.nanoTime()>deadline?
            "Search limit reached. Reduce the workload or add rooms and try again.":
            "No feasible timetable was found for these inputs. Check teacher availability, room capacity and weekly hours.");
        return new ArrayList<Entry>(result);
    }
    private boolean search(int i){
        if(i==units.size())return true;
        if(++nodes>500000||System.nanoTime()>deadline)return false;
        Lesson l=units.get(i).l;int old=last.getOrDefault(l.subject,-1);
        // Visit slots by period first to spread lessons across the week.
        for(int rank=old+1;rank<days*periods;rank++){
            int day=rank%days,period=rank/days,slot=day*periods+period;
            if(Arrays.asList(l.unavailable.split(",")).contains((day+1)+":"+(period+1)))continue;
            String g=key("G",l.group,slot),t=key("T",l.teacher,slot);
            if(busy.contains(g)||busy.contains(t))continue;
            for(Room r:rooms){
                String rk=key("R",r.id,slot);if(!suitable(l,r)||busy.contains(rk))continue;
                busy.add(g);busy.add(t);busy.add(rk);last.put(l.subject,rank);result.add(new Entry(l,day,period,r.id));
                if(search(i+1))return true;
                result.remove(result.size()-1);busy.remove(g);busy.remove(t);busy.remove(rk);last.put(l.subject,old);
            }
        }
        return false;
    }
}
