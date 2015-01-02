package net.floodlightcontroller.traceroute;
public class SwitchLinks {
    
    private String srcMac;
    private int srcPort;
    private String destMac;
    private int destPort;

    public String getSrcMac() {
        return srcMac;
    }

    public void setSrcMac(String srcMac) {
        this.srcMac = srcMac;
    }

    public int getSrcPort() {
        return srcPort;
    }

    public void setSrcPort(int srcPort) {
        this.srcPort = srcPort;
    }

    public String getDestMac() {
        return destMac;
    }

    public void setDestMac(String destMac) {
        this.destMac = destMac;
    }

    public int getDestPort() {
        return destPort;
    }

    public void setDestPort(int destPort) {
        this.destPort = destPort;
    }

    public SwitchLinks(String sm, int sp, String dm, int dp){
        srcMac = sm;
        srcPort = sp;
        destMac = dm;
        destPort = dp;
    }

    @Override
    public String toString(){
        return (srcMac + " - " + srcPort + " --> " + destMac + " - " + destPort);
    }

}
