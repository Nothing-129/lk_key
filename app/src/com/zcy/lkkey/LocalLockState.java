package com.zcy.lkkey;

/** Last acknowledged lock command, not a measurement of the physical door locks. */
final class LocalLockState {
    static final int UNKNOWN=0,LOCKED=1,UNLOCKED=2;
    static int nextCommand(int recorded){return recorded==UNLOCKED?1:2;}
    static int afterAcknowledgement(int recorded,int result,int opcode,String action){
        if(result!=0)return recorded;
        if(opcode==2&&"开锁".equals(action))return UNLOCKED;
        if(opcode==1&&"关锁升窗".equals(action))return LOCKED;
        return recorded;
    }
}
