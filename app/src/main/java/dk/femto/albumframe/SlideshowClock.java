package dk.femto.albumframe;

/** One pending advance at most; loading and pause never race a second timer. */
final class SlideshowClock {
    interface Scheduler { void cancel(Runnable r); void after(Runnable r,long ms); }
    private final Scheduler scheduler;
    private final Runnable advance;
    private boolean paused,ready,closed;
    private int seconds;
    SlideshowClock(Scheduler scheduler,Runnable advance,int seconds) {
        this.scheduler=scheduler;this.advance=advance;setSeconds(seconds);
    }
    int seconds(){return seconds;}
    boolean paused(){return paused;}
    void setSeconds(int value){seconds=valid(value)?value:4;reschedule();}
    static boolean valid(int value){return value==1 || value==2 || value==4 || value==8 || value==16 || value==32;}
    void setPaused(boolean value){paused=value;reschedule();}
    void loading(){ready=false;reschedule();}
    void displayed(){ready=true;reschedule();}
    void close(){closed=true;reschedule();}
    private void reschedule(){
        scheduler.cancel(advance);
        if(!closed && ready && !paused)scheduler.after(advance,seconds*1000L);
    }
}
