package dk.femto.albumframe;

/** One pending action: display advance or recovery, both governed by pause and close. */
final class SlideshowClock {
    interface Scheduler { void cancel(Runnable r); void after(Runnable r,long ms); }
    private final Scheduler scheduler;
    private final Runnable advance;
    private Runnable recovery, pending;
    private boolean paused,ready,closed;
    private int seconds,failures;
    SlideshowClock(Scheduler scheduler,Runnable advance,int seconds) {
        this.scheduler=scheduler;this.advance=advance;setSeconds(seconds);
    }
    int seconds(){return seconds;}
    boolean paused(){return paused;}
    void setSeconds(int value){seconds=valid(value)?value:4;reschedule();}
    static boolean valid(int value){return value==1 || value==2 || value==4 || value==8 || value==16 || value==32;}
    void setPaused(boolean value){paused=value;reschedule();}
    void loading(){ready=false;recovery=null;reschedule();}
    void displayed(){ready=true;recovery=null;failures=0;reschedule();}
    /** Retry indefinitely at a capped rate, including when the first photo failed. */
    void failed(Runnable action){ready=false;recovery=action;failures=Math.min(failures+1,5);reschedule();}
    void close(){closed=true;recovery=null;reschedule();}
    private void reschedule(){
        if(pending!=null)scheduler.cancel(pending);
        pending=null;
        if(closed || paused)return;
        if(recovery!=null){
            pending=recovery;
            scheduler.after(pending,Math.min(60000L,5000L << (failures-1)));
        }else if(ready){pending=advance;scheduler.after(pending,seconds*1000L);}
    }
}
