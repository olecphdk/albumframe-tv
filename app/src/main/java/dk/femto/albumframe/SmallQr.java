package dk.femto.albumframe;

import java.nio.charset.StandardCharsets;

/** Fixed QR version 4-L, byte mode, mask 0. 78-byte maximum; no network dependency. */
final class SmallQr {
    private final boolean[][] cells = new boolean[33][33], reserved = new boolean[33][33];
    private void put(int x,int y,boolean b) { if(x>=0&&y>=0&&x<33&&y<33) { cells[y][x]=b; reserved[y][x]=true; } }
    private void finder(int x,int y) {
        for(int dy=-4;dy<=4;dy++) for(int dx=-4;dx<=4;dx++) {
            int d=Math.max(Math.abs(dx),Math.abs(dy)); put(x+dx,y+dy,d!=2&&d!=4);
        }
    }
    private void format() {
        int data=8, rem=data;
        for(int i=0;i<10;i++) rem=(rem<<1)^((rem>>>9)*0x537);
        int bits=((data<<10)|rem)^0x5412;
        for(int i=0;i<=5;i++) put(8,i,((bits>>>i)&1)!=0);
        put(8,7,((bits>>>6)&1)!=0); put(8,8,((bits>>>7)&1)!=0); put(7,8,((bits>>>8)&1)!=0);
        for(int i=9;i<15;i++) put(14-i,8,((bits>>>i)&1)!=0);
        for(int i=0;i<8;i++) put(32-i,8,((bits>>>i)&1)!=0);
        for(int i=8;i<15;i++) put(8,18+i,((bits>>>i)&1)!=0);
        put(8,25,true);
    }
    static int mul(int x,int y) {
        int z=0; for(int i=7;i>=0;i--) { z=(z<<1)^((z>>>7)*0x11d); z^=((y>>>i)&1)*x; } return z;
    }
    static boolean[][] encode(String text) {
        byte[] src=text.getBytes(StandardCharsets.UTF_8);
        if(src.length>78) throw new IllegalArgumentException("QR URL too long");
        byte[] data=new byte[80]; int pos=0;
        // Byte mode indicator, 8-bit character count, payload and terminator.
        int[] values=new int[src.length+2]; values[0]=4; values[1]=src.length;
        for(int i=0;i<src.length;i++) values[i+2]=src[i]&255;
        for(int k=0;k<values.length;k++) for(int b=(k==0?3:7);b>=0;b--,pos++) data[pos/8]|=((values[k]>>>b)&1)<<(7-pos%8);
        pos+=4; int used=(pos+7)/8;
        for(int i=used;i<80;i++) data[i]=(byte)((i-used)%2==0?0xec:0x11);
        int[] divisor=new int[20]; divisor[19]=1; int root=1;
        for(int i=0;i<20;i++) {
            for(int j=0;j<20;j++) { divisor[j]=mul(divisor[j],root); if(j+1<20) divisor[j]^=divisor[j+1]; }
            root=mul(root,2);
        }
        int[] ecc=new int[20];
        for(byte d:data) {
            int factor=(d&255)^ecc[0]; System.arraycopy(ecc,1,ecc,0,19); ecc[19]=0;
            for(int j=0;j<20;j++) ecc[j]^=mul(divisor[j],factor);
        }
        byte[] all=new byte[100]; System.arraycopy(data,0,all,0,80);
        for(int i=0;i<20;i++) all[80+i]=(byte)ecc[i];
        SmallQr q=new SmallQr();
        for(int i=0;i<33;i++) { q.put(6,i,i%2==0); q.put(i,6,i%2==0); }
        q.finder(3,3); q.finder(29,3); q.finder(3,29);
        for(int dy=-2;dy<=2;dy++) for(int dx=-2;dx<=2;dx++) q.put(26+dx,26+dy,Math.max(Math.abs(dx),Math.abs(dy))!=1);
        q.format(); pos=0;
        for(int right=32;right>=1;right-=2) {
            if(right==6) right=5;
            for(int vert=0;vert<33;vert++) for(int j=0;j<2;j++) {
                int x=right-j, y=((right+1)&2)==0?32-vert:vert;
                if(!q.reserved[y][x]) {
                    boolean bit=pos<800&&((all[pos/8]>>>(7-pos%8))&1)!=0;
                    q.cells[y][x]=bit^((x+y)%2==0); pos++;
                }
            }
        }
        return q.cells;
    }
}
