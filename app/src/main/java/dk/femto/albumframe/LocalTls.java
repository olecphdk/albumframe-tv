package dk.femto.albumframe;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.net.ssl.*;

/** Per-session self-signed certificate. Private key never leaves memory. */
final class LocalTls {
    static byte[] join(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] p : parts) out.write(p, 0, p.length);
        return out.toByteArray();
    }
    static byte[] der(int tag, byte[] data) {
        byte[] len = data.length < 128 ? new byte[]{(byte)data.length}
            : data.length < 256 ? new byte[]{(byte)0x81,(byte)data.length}
            : new byte[]{(byte)0x82,(byte)(data.length>>8),(byte)data.length};
        return join(new byte[]{(byte)tag}, len, data);
    }
    static byte[] seq(byte[]... parts) { return der(0x30, join(parts)); }
    static byte[] integer(BigInteger n) { return der(2, n.toByteArray()); }
    static byte[] utc(long t) {
        SimpleDateFormat f = new SimpleDateFormat("yyMMddHHmmss'Z'", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return der(0x17, f.format(new Date(t)).getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }
    static SSLContext context() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA"); gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();
        byte[] algorithm = seq(der(6, new byte[]{0x2a,(byte)0x86,0x48,(byte)0x86,(byte)0xf7,0x0d,1,1,0x0b}), der(5,new byte[0]));
        byte[] name = seq(der(0x31, seq(der(6,new byte[]{0x55,4,3}), der(12,"AlbumFrame TV local setup".getBytes(java.nio.charset.StandardCharsets.UTF_8)))));
        long now = System.currentTimeMillis();
        byte[] tbs = seq(integer(new BigInteger(120,new SecureRandom()).add(BigInteger.ONE)), algorithm, name,
            seq(utc(now-86400000L),utc(now+86400000L)), name, pair.getPublic().getEncoded());
        Signature signer = Signature.getInstance("SHA256withRSA"); signer.initSign(pair.getPrivate()); signer.update(tbs);
        byte[] cert = seq(tbs, algorithm, der(3,join(new byte[]{0},signer.sign())));
        java.security.cert.Certificate parsed = CertificateFactory.getInstance("X.509").generateCertificate(new java.io.ByteArrayInputStream(cert));
        KeyStore ks = KeyStore.getInstance("PKCS12"); ks.load(null,null);
        char[] password = UUID.randomUUID().toString().toCharArray();
        ks.setKeyEntry("session",pair.getPrivate(),password,new java.security.cert.Certificate[]{parsed});
        KeyManagerFactory km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()); km.init(ks,password);
        SSLContext ctx = SSLContext.getInstance("TLSv1.2"); ctx.init(km.getKeyManagers(),null,new SecureRandom());
        Arrays.fill(password,'\0');
        return ctx;
    }

    static void enableModernProtocols(SSLServerSocket socket) {
        Set<String> supported=new HashSet<>(Arrays.asList(socket.getSupportedProtocols()));
        List<String> enabled=new ArrayList<>();
        if(supported.contains("TLSv1.3"))enabled.add("TLSv1.3");
        if(supported.contains("TLSv1.2"))enabled.add("TLSv1.2");
        if(enabled.isEmpty())throw new IllegalStateException("TLS 1.2 or newer is unavailable");
        socket.setEnabledProtocols(enabled.toArray(new String[0]));
    }
}
