package dk.femto.albumframe;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/** Offline failure scenarios use virtual time; no network, TV or credentials required. */
public final class RecoveryTest {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args){
        SlideshowClockTest.Scheduler scheduler=new SlideshowClockTest.Scheduler();
        int[] retries={0},advances={0};
        Runnable retry=()->retries[0]++;
        SlideshowClock clock=new SlideshowClock(scheduler,()->advances[0]++,4);
        // Recovery must work before the first photo, with no known album length.
        for(long delay:new long[]{5000,10000,20000,40000,60000,60000}){
            clock.loading();clock.failed(retry);
            check(scheduler.delay==delay,"bounded exponential retry delay");
            scheduler.tick();
        }
        check(retries[0]==6 && advances[0]==0,"retry the same photo without skipping");
        clock.loading();clock.failed(retry);clock.setPaused(true);scheduler.tick();
        check(retries[0]==6,"pause cancels recovery");
        clock.setPaused(false);check(scheduler.pending==retry,"resume restores recovery");
        clock.loading();check(scheduler.pending==null,"manual navigation cancels old recovery");
        clock.displayed();check(scheduler.delay==4000,"successful image resumes normal interval");
        clock.loading();clock.failed(retry);check(scheduler.delay==5000,"success resets backoff");
        clock.setSeconds(8);check(scheduler.delay==5000,"speed change does not replace recovery");
        clock.close();clock.failed(retry);clock.setPaused(false);scheduler.tick();
        check(retries[0]==6,"close blocks both retries and advances");
        for(int status:new int[]{408,429,500,502,503,504})check(NetworkFailure.retryable(new NetworkFailure(status,"HTTP")),"transient HTTP "+status);
        for(int status:new int[]{301,400,401,403,404})check(!NetworkFailure.retryable(new NetworkFailure(status,"HTTP")),"permanent HTTP "+status);
        check(NetworkFailure.retryable(new UnknownHostException()),"offline DNS retries");
        check(NetworkFailure.retryable(new SocketTimeoutException()),"timeouts retry");
        check(!NetworkFailure.retryable(new IOException("invalid format")),"bad image does not retry indefinitely");
        check(!NetworkFailure.retryable(new InterruptedException()),"cancel does not retry");
        System.out.println("PASS first-photo outage, capped backoff, pause/resume, navigation, success, close and HTTP classification");
    }
}
