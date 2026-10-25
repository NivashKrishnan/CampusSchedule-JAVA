import app.Scheduler;
import java.util.*;

/** Independent assertions for hard constraints and impossible inputs. */
public final class SchedulerCheck {
    static int passed;
    static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
    static void rejects(String name,Runnable run){try{run.run();throw new AssertionError(name+" accepted");}catch(IllegalArgumentException expected){passed++;System.out.println("PASS "+name);}}
    public static void main(String[] args){
        List<Scheduler.Lesson> lessons=Arrays.asList(
            new Scheduler.Lesson(1,1,1,4,30,"Java A","Theory","1:1"),
            new Scheduler.Lesson(2,2,1,4,30,"Java B","Theory","1:1"),
            new Scheduler.Lesson(3,1,2,2,30,"Lab A","Lab",""),
            new Scheduler.Lesson(4,2,2,2,30,"Lab B","Lab",""));
        List<Scheduler.Room> rooms=Arrays.asList(new Scheduler.Room(1,35,"Theory"),new Scheduler.Room(2,35,"Lab"));
        List<Scheduler.Entry> result=new Scheduler(5,4,lessons,rooms).generate();
        require(result.size()==12,"Every required lesson must be placed");
        Set<String> teachers=new HashSet<String>(),groups=new HashSet<String>(),roomSlots=new HashSet<String>();
        Map<Integer,Integer> hours=new HashMap<Integer,Integer>();
        for(Scheduler.Entry e:result){
            String slot=e.day+":"+e.period;
            require(teachers.add(e.lesson.teacher+":"+slot),"Teacher clash");
            require(groups.add(e.lesson.group+":"+slot),"Class clash");
            require(roomSlots.add(e.room+":"+slot),"Room clash");
            require(!(e.lesson.teacher==1&&e.day==0&&e.period==0),"Unavailable period used");
            require(!"Lab".equals(e.lesson.type)||e.room==2,"Lab room required");
            hours.put(e.lesson.subject,hours.getOrDefault(e.lesson.subject,0)+1);
        }
        for(Scheduler.Lesson l:lessons)require(hours.get(l.subject)==l.hours,"Subject quota mismatch");
        passed++;System.out.println("PASS feasible schedule: quotas, availability, class/teacher/room uniqueness, lab assignment");
        rejects("No rooms",()->new Scheduler(5,4,lessons,Collections.<Scheduler.Room>emptyList()));
        rejects("Room too small",()->new Scheduler(5,4,lessons,Arrays.asList(new Scheduler.Room(1,10,"Lab"))));
        rejects("No lab room",()->new Scheduler(5,4,lessons,Arrays.asList(new Scheduler.Room(1,35,"Theory"))));
        rejects("Weekly quota exceeds week",()->new Scheduler(1,1,lessons,rooms));
        rejects("No subjects",()->new Scheduler(5,4,Collections.<Scheduler.Lesson>emptyList(),rooms));
        rejects("Invalid day count",()->new Scheduler(0,4,lessons,rooms));
        rejects("All teacher periods unavailable",()->new Scheduler(1,1,Arrays.asList(new Scheduler.Lesson(1,1,1,1,30,"Java","Theory","1:1")),rooms).generate());
        rejects("Shared teacher overloaded",()->new Scheduler(1,1,Arrays.asList(new Scheduler.Lesson(1,1,1,1,30,"A","Theory",""),new Scheduler.Lesson(2,2,1,1,30,"B","Theory","")),rooms));
        rejects("Shared room overbooked",()->new Scheduler(1,1,Arrays.asList(new Scheduler.Lesson(1,1,1,1,30,"A","Theory",""),new Scheduler.Lesson(2,2,2,1,30,"B","Theory","")),Arrays.asList(new Scheduler.Room(1,35,"Theory"))).generate());
        // A later invocation must be independent of earlier dimensions and data.
        require(new Scheduler(1,1,Arrays.asList(new Scheduler.Lesson(1,1,1,1,30,"A","Theory","")),rooms).generate().size()==1,"Request state leaked");
        passed++;System.out.println("PASS repeated runs use independent state");
        rejects("Full week rejects missing periods",()->new Scheduler(5,4,lessons,rooms,true));
        // Equal total hours across classes must not conceal an underfilled class.
        rejects("Full coverage is checked per class",()->new Scheduler(2,2,Arrays.asList(new Scheduler.Lesson(1,1,1,4,30,"A","Theory",""),new Scheduler.Lesson(2,2,2,2,30,"B","Theory","")),rooms,true));
        List<Scheduler.Lesson> fullLessons=Arrays.asList(new Scheduler.Lesson(10,3,3,12,30,"Java","Theory",""),new Scheduler.Lesson(11,3,4,12,30,"Maths","Theory",""),new Scheduler.Lesson(12,3,5,12,30,"DS","Theory",""),new Scheduler.Lesson(13,3,6,12,30,"Lab","Lab",""));
        List<Scheduler.Entry> full=new Scheduler(6,8,fullLessons,rooms,true).generate();
        Set<String> occupied=new HashSet<String>();Map<Integer,Integer> fullHours=new HashMap<Integer,Integer>();
        for(Scheduler.Entry e:full){require(occupied.add(e.day+":"+e.period),"Full-week duplicate slot");fullHours.put(e.lesson.subject,fullHours.getOrDefault(e.lesson.subject,0)+1);}
        for(int d=0;d<6;d++)for(int p=0;p<8;p++)require(occupied.contains(d+":"+p),"Full-week slot missing");
        for(Scheduler.Lesson l:fullLessons)require(fullHours.get(l.subject)==l.hours,"Full-week subject quota changed");
        require(full.size()==48,"Full week must contain exactly 48 entries");passed++;System.out.println("PASS full 6x8 week: every slot occupied and subject quotas unchanged");
        System.out.println("TOTAL "+passed+" checks passed");
    }
}
