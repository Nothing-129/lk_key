package com.zcy.lkkey;

public class NfcTapGateTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        NfcTapGate gate=new NfcTapGate();long first=gate.begin(),firstTag=gate.observed();check(first!=0,"first tap rejected");
        check(gate.begin()==0,"duplicate during execution accepted");
        gate.complete(first);check(gate.begin()==0,"held tag triggered after ACK");
        gate.removed(firstTag);long second=gate.begin(),secondTag=gate.observed();check(second>first,"removed tag must allow immediate next tap");
        gate.removed(secondTag);check(gate.begin()==0,"removal before ACK enabled an overlapping operation");
        gate.complete(second);long third=gate.begin(),thirdTag=gate.observed();check(third>second,"ACK after removal must enable next tap");
        gate.removed(firstTag);gate.complete(first);check(gate.begin()==0,"stale callbacks released a newer operation");
        gate.complete(third);check(gate.begin()==0,"failure still requires moving held tag away");
        gate.removed(thirdTag);long retry=gate.begin(),retryTag=gate.observed();check(retry>third,"failure plus removal must allow retry");
        check(gate.pending(retry),"current operation missing");check(!gate.pending(third),"stale operation still pending");
        gate.removed(retryTag);check(gate.begin()==0,"early retap must not start while pending");
        long earlyTag=gate.observed();gate.complete(retry);
        gate.removed(retryTag);check(gate.begin()==0,"held early retap or stale removal allowed reversal after ACK");
        gate.removed(earlyTag);check(gate.begin()>retry,"early retap can retry once removed and operation completed");
        System.out.println("PASS: completion + removal in either order enables immediate next tap; held/repeated/premature/stale events cannot trigger another command; failure can retry after removal");
    }
}
