package com.zcy.lkkey;

public class LocalLockStateTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        int state=LocalLockState.UNKNOWN;
        check(LocalLockState.nextCommand(state)==2,"first touch must unlock");
        for(int i=0;i<8;i++){
            int command=LocalLockState.nextCommand(state);
            String action=command==2?"开锁":"关锁升窗";
            for(int error=1;error<=7;error++)check(LocalLockState.afterAcknowledgement(state,error,command,action)==state,"failure must retain next action");
            int after=LocalLockState.afterAcknowledgement(state,0,command,action);
            check(after==command,"successful command recorded");
            check(LocalLockState.nextCommand(after)!=command,"next successful operation must alternate");
            state=after;
        }
        for(int before:new int[]{LocalLockState.UNKNOWN,LocalLockState.LOCKED,LocalLockState.UNLOCKED}){
            for(String action:new String[]{"开窗","关窗","后备箱","读取洗车模式","设置洗车模式","",null}){
                for(int command:new int[]{1,2,3,6,10})check(LocalLockState.afterAcknowledgement(before,0,command,action)==before,"unrelated ACK changed lock record");
            }
            check(LocalLockState.afterAcknowledgement(before,0,1,"开锁")==before,"mismatched ACK must be ignored");
            check(LocalLockState.afterAcknowledgement(before,0,2,"关锁升窗")==before,"mismatched ACK must be ignored");
        }
        check(LocalLockState.nextCommand(99)==2,"unknown/corrupt record defaults to unlock");
        System.out.println("PASS: first unlock; confirmed operations alternate; failed/unrelated/mismatched ACKs preserve local record");
    }
}
