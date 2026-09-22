package dk.femto.albumframe;

/** A fake scheduler exercises pause/loading/resume without Android timing flakiness. */
public final class SlideshowClockTest {
    static class Scheduler implements SlideshowClock.Scheduler {
        Runnable pending;long delay;int fired;
        public void cancel(Runnable r){if(pending==r)pending=null;}
        public void after(Runnable r,long ms){if(pending!=null)throw new AssertionError("duplicate timer");pending=r;delay=ms;}
        void tick(){Runnable r=pending;pending=null;if(r!=null){fired++;r.run();}}
    }
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        Scheduler s=new Scheduler();int[] moves={0};
        SlideshowClock c=new SlideshowClock(s,()->moves[0]++,4);
        check(s.pending==null);c.displayed();check(s.delay==4000);s.tick();check(moves[0]==1);
        c.loading();check(s.pending==null);c.setPaused(true);c.displayed();check(s.pending==null);
        c.setPaused(false);check(s.pending!=null);c.setPaused(false);s.tick();check(moves[0]==2);
        for(int seconds:new int[]{1,2,4,8,16,32}){c.setSeconds(seconds);check(s.delay==seconds*1000L);}
        c.loading();c.setPaused(true);c.setPaused(false);check(s.pending==null);
        c.displayed();check(s.pending!=null);c.setPaused(true);s.tick();check(moves[0]==2);
        c.setSeconds(3);check(c.seconds()==4);c.setPaused(false);check(s.delay==4000);
        c.close();check(s.pending==null);c.displayed();c.setPaused(false);check(s.pending==null);
        System.out.println("PASS automatic advance, pause during load, resume, one timer, all intervals, close");
    }
}
