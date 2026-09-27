package dk.femto.albumframe;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/** Preserve HTTP status without exposing URLs, credentials or server response bodies. */
final class NetworkFailure extends IOException {
    private final int status;
    NetworkFailure(int status,String message){super(message);this.status=status;}
    static boolean retryable(Exception error){
        if(error instanceof NetworkFailure){
            int status=((NetworkFailure)error).status;
            return status==408 || status==429 || (status>=500 && status<=599);
        }
        return error instanceof SocketTimeoutException || error instanceof SocketException
            || error instanceof UnknownHostException || error instanceof EOFException;
    }
}
