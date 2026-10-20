package com.zcy.lkkey;

public final class VehicleVisualStateTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        VehicleVisualState state=new VehicleVisualState();
        String initial=state.json();
        check(!state.acknowledge("读取洗车模式"),"wash must not invent vehicle state");
        check(initial.equals(state.json()),"unsupported result changed illustration");
        check(state.acknowledge("开锁")&&state.json().contains("\"mirrorsFolded\":false"),"unlock unfolds mirrors");
        check(state.json().contains("\"lightAction\":\"unlock\""),"unlock welcome sequence");
        state.acknowledge("开窗");
        check(state.json().contains("\"windowsOpen\":true"),"open windows");
        check(state.json().contains("\"lightAction\":\"none\""),"windows must not replay lock lights");
        state.acknowledge("关锁升窗");
        check(state.json().contains("\"mirrorsFolded\":true")&&state.json().contains("\"windowsOpen\":false"),"lock must fold and close windows");
        check(state.json().contains("\"lightAction\":\"lock\""),"lock farewell sequence");
        state.acknowledge("后备箱");state.acknowledge("后备箱");
        check(state.json().contains("\"trunkOpen\":true"),"second release must not imply powered close");
        state.acknowledge("关窗");
        check(state.json().contains("\"trunkOpen\":true"),"unrelated command closed trunk");
        System.out.println("VehicleVisualStateTest PASS");
    }
}
