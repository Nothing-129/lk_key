package com.zcy.lkkey;

/** A new tap requires both operation completion and removal of the previous tag. */
final class NfcTapGate {
    private long generation;
    private long presenceGeneration;
    private boolean pending,present;
    synchronized long begin(){
        if(pending||present)return 0;
        pending=true;present=true;return ++generation;
    }
    // A premature repeat is ignored but still has to be moved away. Removal
    // callbacks from an older detection must not release a newly held tag.
    synchronized long observed(){present=true;return ++presenceGeneration;}
    synchronized void removed(long presence){if(presence==presenceGeneration)present=false;}
    synchronized void complete(long operation){if(operation==generation)pending=false;}
    synchronized boolean pending(long operation){return operation==generation&&pending;}
}
