package com.zcy.lkkey;

/** Last acknowledged command illustration, never a report of physical sensors. */
final class VehicleVisualState {
    private boolean mirrorsFolded=true,windowsOpen=false,trunkOpen=false;
    private int revision;
    private String lightAction="none";
    boolean acknowledge(String action) {
        String lights="none";
        switch(action) {
            case "关锁升窗": mirrorsFolded=true;windowsOpen=false;lights="lock";break;
            case "开锁": mirrorsFolded=false;lights="unlock";break;
            case "开窗": windowsOpen=true;break;
            case "关窗": windowsOpen=false;break;
            // The protocol only releases the boot: it does not command closure.
            case "后备箱": trunkOpen=true;break;
            default: return false;
        }
        lightAction=lights;revision++;return true;
    }
    String json() {
        return "{\"mirrorsFolded\":"+mirrorsFolded+",\"windowsOpen\":"+windowsOpen+
            ",\"trunkOpen\":"+trunkOpen+",\"revision\":"+revision+",\"lightAction\":\""+lightAction+"\"}";
    }
}
