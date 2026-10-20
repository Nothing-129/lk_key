package com.zcy.lkkey.test;
import android.app.*;
import android.content.Intent;
import android.os.*;
import android.view.*;
import android.widget.*;

public class DeviceTest extends Instrumentation {
 private Activity activity;
 private String mode;
 @Override public void onCreate(Bundle args){mode=args.getString("mode","connect");start();}
 @Override public void onStart(){
  Bundle result=new Bundle();
  try{
   if("nfc".equals(mode)){nfc(result);return;}
   if("nfc-silent".equals(mode)){nfcSilent(result);return;}
   activity=startActivitySync(new Intent().setClassName("com.zcy.lkkey","com.zcy.lkkey.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
   if("layout".equals(mode)){layout(result);return;}
   if("3d".equals(mode)){vehicle3D(result);return;}
   if("scene".equals(mode)){scene(result);return;}
   if("wash-style".equals(mode)){washStyle(result);return;}
   if("retry".equals(mode)){retry(result);return;}
   if("controls".equals(mode)){controls(result);return;}
   if("offline".equals(mode)){offline(result);return;}
   if(!awaitReady(35000))throw new IllegalStateException("未就绪："+text());
   result.putString("connection","READY");
   if("open-close".equals(mode)){
    click("开锁");if(!awaitText("设备已确认开锁指令",8000))throw new IllegalStateException("开锁未确认："+text());
    result.putString("open","ACK_SUCCESS");sendStatus(0,result);SystemClock.sleep(2500);
    click("关锁");if(!awaitText("设备已确认关锁指令",8000))throw new IllegalStateException("关锁未确认："+text());
    result.putString("close","ACK_SUCCESS");
   }
   result.putString("screen",text());finish(Activity.RESULT_OK,result);
  }catch(Exception e){result.putString("error",e.toString());finish(Activity.RESULT_CANCELED,result);}
 }
 // NFC setup and isolated routing checks only; do not create a BLE session.
 private void nfc(Bundle result)throws Exception{
  android.content.Context target=getTargetContext();
  android.content.SharedPreferences prefs=target.getSharedPreferences("nfc_test_isolated",0);prefs.edit().clear().commit();
  android.content.Context isolated=new android.content.ContextWrapper(target){
   @Override public android.content.SharedPreferences getSharedPreferences(String name,int mode){return prefs;}
  };
  Class<?> access=target.getClassLoader().loadClass("com.zcy.lkkey.NfcAccess");
  java.lang.reflect.Method ensure=access.getDeclaredMethod("ensureToken",android.content.Context.class),message=access.getDeclaredMethod("message",android.content.Context.class),matches=access.getDeclaredMethod("matches",android.content.Context.class,android.nfc.NdefMessage.class),issue=access.getDeclaredMethod("createTicket",android.content.Context.class,long.class),consume=access.getDeclaredMethod("consumeTicket",Intent.class);
  for(java.lang.reflect.Method method:new java.lang.reflect.Method[]{ensure,message,matches,issue,consume})method.setAccessible(true);
  String token=(String)ensure.invoke(null,isolated);
  if(!token.matches("[0-9a-f]{32}")||!token.equals(ensure.invoke(null,isolated)))throw new IllegalStateException("identifier not stable");
  android.nfc.NdefMessage own=(android.nfc.NdefMessage)message.invoke(null,isolated);
  if((boolean)matches.invoke(null,isolated,own))throw new IllegalStateException("disabled tag accepted");
  prefs.edit().putBoolean("enabled",true).commit();
  if(!(boolean)matches.invoke(null,isolated,own))throw new IllegalStateException("own tag rejected");
  android.nfc.NdefMessage other=new android.nfc.NdefMessage(new android.nfc.NdefRecord[]{android.nfc.NdefRecord.createUri("lkkey://nfc/unlock?token=11111111111111111111111111111111")});
  if((boolean)matches.invoke(null,isolated,other))throw new IllegalStateException("foreign tag accepted");
  android.nfc.NdefMessage reordered=new android.nfc.NdefMessage(new android.nfc.NdefRecord[]{android.nfc.NdefRecord.createTextRecord("en","text"),own.getRecords()[0]});
  if((boolean)matches.invoke(null,isolated,reordered))throw new IllegalStateException("unrelated first record accepted");
  if((long)consume.invoke(null,new Intent().putExtra("com.zcy.lkkey.NFC_TICKET","forged"))!=0)throw new IllegalStateException("forged ticket accepted");
  java.lang.reflect.Field gateField=access.getDeclaredField("TAP_GATE");gateField.setAccessible(true);Object gate=gateField.get(null);
  java.lang.reflect.Method begin=gate.getClass().getDeclaredMethod("begin"),observed=gate.getClass().getDeclaredMethod("observed"),removed=gate.getClass().getDeclaredMethod("removed",long.class),complete=gate.getClass().getDeclaredMethod("complete",long.class);
  for(java.lang.reflect.Method method:new java.lang.reflect.Method[]{begin,observed,removed,complete})method.setAccessible(true);
  long operation=(long)begin.invoke(gate),presence=(long)observed.invoke(gate);
  Intent ticket=(Intent)issue.invoke(null,target,operation);
  if(ticket.getComponent()==null||!"com.zcy.lkkey.NfcService".equals(ticket.getComponent().getClassName()))throw new IllegalStateException("NFC handoff still launches an activity");
  if(ticket==null||(long)consume.invoke(null,ticket)!=operation||(long)consume.invoke(null,ticket)!=0)throw new IllegalStateException("ticket not single use");
  if((long)begin.invoke(gate)!=0)throw new IllegalStateException("duplicate scan accepted");
  complete.invoke(gate,operation);
  if((long)begin.invoke(gate)!=0)throw new IllegalStateException("held tag accepted after ACK");
  removed.invoke(gate,presence);
  Class<?> store=target.getClassLoader().loadClass("com.zcy.lkkey.LocalLockStore");
  java.lang.reflect.Method next=store.getDeclaredMethod("nextCommand",android.content.Context.class),ack=store.getDeclaredMethod("acknowledge",android.content.Context.class,int.class,int.class,String.class);
  next.setAccessible(true);ack.setAccessible(true);
  if((int)next.invoke(null,isolated)!=2)throw new IllegalStateException("first touch must unlock");
  if(!(boolean)ack.invoke(null,isolated,0,2,"开锁")||(int)next.invoke(null,isolated)!=1)throw new IllegalStateException("unlock confirmation not persisted");
  if(!prefs.contains("last_confirmed"))throw new IllegalStateException("record was not written to preferences");
  ack.invoke(null,isolated,1,1,"关锁升窗");
  if((int)next.invoke(null,isolated)!=1)throw new IllegalStateException("failed lock advanced record");
  ack.invoke(null,isolated,0,1,"关锁升窗");
  if((int)next.invoke(null,isolated)!=2)throw new IllegalStateException("lock confirmation not persisted");
  // Simulate replies on a fresh isolated BLE object; never connect or write GATT.
  Class<?> ble=target.getClassLoader().loadClass("com.zcy.lkkey.BleKey");
  java.lang.reflect.Constructor<?> constructor=ble.getDeclaredConstructor(android.content.Context.class);constructor.setAccessible(true);Object simulated=constructor.newInstance(isolated);
  java.lang.reflect.Field pending=ble.getDeclaredField("pending"),label=ble.getDeclaredField("actionLabel"),completion=ble.getDeclaredField("commandCompletion");
  for(java.lang.reflect.Field field:new java.lang.reflect.Field[]{pending,label,completion})field.setAccessible(true);
  java.lang.reflect.Method receive=ble.getDeclaredMethod("receive",byte[].class);receive.setAccessible(true);int[] callbacks={0};
  runOnMainSync(()->{try{
   pending.setInt(simulated,2);label.set(simulated,"开锁");
   completion.set(simulated,(Runnable)()->{try{if((int)next.invoke(null,isolated)!=1)throw new IllegalStateException("completion ran before lock record updated");callbacks[0]++;}catch(Exception error){throw new RuntimeException(error);}});
   receive.invoke(simulated,(Object)new byte[]{0,1,0,0});
   receive.invoke(simulated,(Object)new byte[]{0,2,1,0});
   if(callbacks[0]!=0)throw new IllegalStateException("unrelated/truncated ACK completed command");
   receive.invoke(simulated,(Object)new byte[]{0,2,0,0});
   receive.invoke(simulated,(Object)new byte[]{0,2,0,0});
   if(callbacks[0]!=1)throw new IllegalStateException("completion did not run exactly once");
   java.lang.reflect.Method confirmed=ble.getDeclaredMethod("completionConfirmed"),completionMessage=ble.getDeclaredMethod("completionMessage");confirmed.setAccessible(true);completionMessage.setAccessible(true);
   if(!(boolean)confirmed.invoke(simulated)||!((String)completionMessage.invoke(simulated)).contains("已确认开锁"))throw new IllegalStateException("success result missing");
   pending.setInt(simulated,1);label.set(simulated,"关锁升窗");completion.set(simulated,(Runnable)()->callbacks[0]++);
   receive.invoke(simulated,(Object)new byte[]{1,1,0,0});
   if(callbacks[0]!=2||(int)next.invoke(null,isolated)!=1)throw new IllegalStateException("failure did not complete or changed lock record");
   if((boolean)confirmed.invoke(simulated)||!((String)completionMessage.invoke(simulated)).contains("设备执行失败"))throw new IllegalStateException("failure result lost during disconnect");
  }catch(Exception error){throw new RuntimeException(error);}});
  prefs.edit().clear().commit();
  activity=startActivitySync(new Intent().setClassName("com.zcy.lkkey","com.zcy.lkkey.NfcSetupActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  if(!text().contains("NFC 贴片")||!text().contains("设置过程中不会发送开锁指令"))throw new IllegalStateException("setup instructions missing");
  boolean[] armed={true};
  java.lang.reflect.Field arm=activity.getClass().getDeclaredField("armed");arm.setAccessible(true);
  runOnMainSync(()->{try{armed[0]=((java.util.concurrent.atomic.AtomicBoolean)arm.get(activity)).get();}catch(Exception error){throw new RuntimeException(error);}});
  if(armed[0])throw new IllegalStateException("setup writes before explicit click");
  // Never arm the writer on a physical phone: a user's nearby tag could be written.
  if("ranchu".equals(Build.HARDWARE)&&android.nfc.NfcAdapter.getDefaultAdapter(target)!=null&&android.nfc.NfcAdapter.getDefaultAdapter(target).isEnabled()){
   click("写入控制贴片");
   if(!awaitText("请将贴片贴近",1000))throw new IllegalStateException("writer did not arm");
   click("取消写入");
   if(!awaitText("已取消写入",1000))throw new IllegalStateException("writer did not cancel");
  }
  snapshot("nfc-setup-preview.png");
  result.putString("nfc","PASS: stable local tag identifier; disabled/foreign/unrelated tags rejected; forged/replayed tickets rejected; completion and removal gate; local lock record persistence; command completion ignores unrelated/truncated/duplicate ACKs, runs after record update and releases on failure; setup initially unarmed. Physical-device writer not activated. No BLE session or vehicle commands.");finish(Activity.RESULT_OK,result);
 }
 private void nfcSilent(Bundle result)throws Exception{
  if(!"ranchu".equals(Build.HARDWARE))throw new IllegalStateException("silent NFC test is emulator-only");
  android.content.Context target=getTargetContext();
  if(target.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)==android.content.pm.PackageManager.PERMISSION_GRANTED)throw new IllegalStateException("test requires Bluetooth permission denied to guarantee no BLE session");
  NotificationManager notifications=target.getSystemService(NotificationManager.class);
  if(!notifications.areNotificationsEnabled())throw new IllegalStateException("grant emulator notification permission for feedback test");
  activity=startActivitySync(new Intent().setClassName("com.zcy.lkkey","com.zcy.lkkey.NfcSetupActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  ActivityMonitor main=addMonitor("com.zcy.lkkey.MainActivity",null,false);
  Class<?> access=target.getClassLoader().loadClass("com.zcy.lkkey.NfcAccess");
  java.lang.reflect.Field gateField=access.getDeclaredField("TAP_GATE");gateField.setAccessible(true);Object gate=gateField.get(null);
  java.lang.reflect.Method begin=gate.getClass().getDeclaredMethod("begin"),observed=gate.getClass().getDeclaredMethod("observed"),removed=gate.getClass().getDeclaredMethod("removed",long.class),pending=gate.getClass().getDeclaredMethod("pending",long.class),issue=access.getDeclaredMethod("createTicket",android.content.Context.class,long.class);
  for(java.lang.reflect.Method method:new java.lang.reflect.Method[]{begin,observed,removed,pending,issue})method.setAccessible(true);
  android.content.SharedPreferences config=target.getSharedPreferences("nfc_tags",0),lockRecord=target.getSharedPreferences("local_lock_state",0);
  boolean hadEnabled=config.contains("enabled"),wasEnabled=config.getBoolean("enabled",false);
  java.util.Map<String,?> originalRecord=new java.util.HashMap<>(lockRecord.getAll());
  final int id=2002;notifications.cancel(id);
  Intent forged=new Intent().setClassName("com.zcy.lkkey","com.zcy.lkkey.NfcService").putExtra("com.zcy.lkkey.NFC_TICKET","forged");
  target.startService(forged);waitForIdleSync();
  for(android.service.notification.StatusBarNotification note:notifications.getActiveNotifications())if(note.getId()==id)throw new IllegalStateException("forged ticket produced notification");
  long operation=(long)begin.invoke(gate),presence=(long)observed.invoke(gate);
  try{
   config.edit().putBoolean("enabled",true).commit();
   target.startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
   long hostDeadline=SystemClock.elapsedRealtime()+4000;
   while(!"com.android.settings".equals(activePackage())&&SystemClock.elapsedRealtime()<hostDeadline)SystemClock.sleep(50);
   if(!"com.android.settings".equals(activePackage()))throw new IllegalStateException("external foreground screen missing");
   Intent valid=(Intent)issue.invoke(null,target,operation);
   // Simulate only the *validated read handoff* inside the real transparent
   // Activity. No mock Tag or production bypass; Bluetooth stays denied.
   Application application=(Application)target.getApplicationContext();String[] launchError={null};
   Application.ActivityLifecycleCallbacks handoff=new Application.ActivityLifecycleCallbacks(){
    public void onActivityCreated(Activity a,Bundle b){if(a.getClass().getName().equals("com.zcy.lkkey.NfcTagActivity"))try{java.lang.reflect.Field handled=a.getClass().getDeclaredField("handled");handled.setAccessible(true);handled.setBoolean(a,true);}catch(Exception e){launchError[0]=e.toString();}}
    public void onActivityResumed(Activity a){if(a.getClass().getName().equals("com.zcy.lkkey.NfcTagActivity"))a.getWindow().getDecorView().postDelayed(()->{
     try{
      java.lang.reflect.Field resumed=a.getClass().getDeclaredField("resumed");resumed.setAccessible(true);
      if(!resumed.getBoolean(a)||a.isFinishing())throw new IllegalStateException("transparent entry did not remain resumed during asynchronous read");
      a.startForegroundService(valid);
     }catch(Exception error){launchError[0]=error.toString();}finally{a.finish();}
    },200);}
    public void onActivityStarted(Activity a){} public void onActivityPaused(Activity a){} public void onActivityStopped(Activity a){} public void onActivitySaveInstanceState(Activity a,Bundle b){} public void onActivityDestroyed(Activity a){}
   };
   application.registerActivityLifecycleCallbacks(handoff);
   target.startActivity(new Intent(android.nfc.NfcAdapter.ACTION_NDEF_DISCOVERED).setClassName("com.zcy.lkkey","com.zcy.lkkey.NfcTagActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
   long end=SystemClock.elapsedRealtime()+6000,posted=0;String feedback="";
   try{
    while(SystemClock.elapsedRealtime()<end){
     for(android.service.notification.StatusBarNotification note:notifications.getActiveNotifications())if(note.getId()==id){feedback=String.valueOf(note.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT));posted=note.getPostTime();}
     if(feedback.contains("附近设备权限")||launchError[0]!=null)break;SystemClock.sleep(50);
    }
   }finally{application.unregisterActivityLifecycleCallbacks(handoff);}
   if(!feedback.contains("附近设备权限"))throw new IllegalStateException("background failure feedback missing: "+feedback+" launch="+launchError[0]);
   if((boolean)pending.invoke(gate,operation))throw new IllegalStateException("failure did not release NFC operation");
   if(!originalRecord.equals(lockRecord.getAll()))throw new IllegalStateException("failed silent operation changed lock record");
   long originalPost=posted;
   target.startService(valid);waitForIdleSync();SystemClock.sleep(250);
   for(android.service.notification.StatusBarNotification note:notifications.getActiveNotifications())if(note.getId()==id&&note.getPostTime()!=originalPost)throw new IllegalStateException("replayed ticket ran again");
   NotificationChannel channel=notifications.getNotificationChannel("nfc_control");
   if(channel==null||channel.getImportance()>NotificationManager.IMPORTANCE_LOW||channel.getSound()!=null||channel.shouldVibrate())throw new IllegalStateException("NFC notification is not silent");
   ActivityMonitor dispatch=addMonitor("com.zcy.lkkey.NfcTagActivity",null,false);
   target.startActivity(new Intent(android.nfc.NfcAdapter.ACTION_NDEF_DISCOVERED).setClassName("com.zcy.lkkey","com.zcy.lkkey.NfcTagActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
   long deadline=SystemClock.elapsedRealtime()+3000;
   while(dispatch.getHits()==0&&SystemClock.elapsedRealtime()<deadline)SystemClock.sleep(50);
   if(dispatch.getHits()==0)throw new IllegalStateException("transparent NFC entry did not run");
   boolean focus=false;
   while(SystemClock.elapsedRealtime()<deadline){focus="com.android.settings".equals(activePackage());if(focus)break;SystemClock.sleep(50);}
   removeMonitor(dispatch);
   if(!focus||main.getHits()!=0)throw new IllegalStateException("NFC entry stole the foreground or opened MainActivity");
   android.content.pm.ActivityInfo entry=target.getPackageManager().getActivityInfo(new android.content.ComponentName("com.zcy.lkkey","com.zcy.lkkey.NfcTagActivity"),0);
   android.content.pm.ActivityInfo home=target.getPackageManager().getActivityInfo(new android.content.ComponentName("com.zcy.lkkey","com.zcy.lkkey.MainActivity"),0);
   if(entry.taskAffinity.equals(home.taskAffinity))throw new IllegalStateException("NFC entry can bring the home task forward");
   boolean running=false;
   for(ActivityManager.RunningServiceInfo service:target.getSystemService(ActivityManager.class).getRunningServices(100))if("com.zcy.lkkey.NfcService".equals(service.service.getClassName()))running=true;
   if(running)throw new IllegalStateException("silent service did not stop");
   result.putString("nfc-silent","PASS: transparent NFC Activity survives async read and starts foreground service over external Settings screen; service ticket rejects forgery/replay; missing Bluetooth permission reports failure via silent notification; local record unchanged; Settings remains foreground, separate task affinity, never opens MainActivity; service terminates. No NFC write, BLE session or vehicle commands.");
  }finally{
   if(hadEnabled)config.edit().putBoolean("enabled",wasEnabled).commit();else config.edit().remove("enabled").commit();
   removed.invoke(gate,presence);removeMonitor(main);notifications.cancel(id);
  }
  finish(Activity.RESULT_OK,result);
 }
 private String activePackage(){android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();if(root==null)return "";try{return String.valueOf(root.getPackageName());}finally{root.recycle();}}
 private String javascript(android.webkit.WebView web,String script)throws Exception{
  java.util.concurrent.CountDownLatch done=new java.util.concurrent.CountDownLatch(1);String[] value={null};
  runOnMainSync(()->web.evaluateJavascript(script,result->{value[0]=result;done.countDown();}));
  if(!done.await(20,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("WebView evaluation timed out");
  return value[0];
 }
 private void awaitJavascript(android.webkit.WebView web,String script,long timeout)throws Exception{
  long end=SystemClock.uptimeMillis()+timeout;
  while(SystemClock.uptimeMillis()<end){if("true".equals(javascript(web,script)))return;SystemClock.sleep(100);}
  throw new IllegalStateException("3D condition failed: "+script+" state="+javascript(web,"JSON.stringify(window.vehicle3D && {ready:vehicle3D.ready,error:vehicle3D.error})"));
 }
 // Emulator-only ACK simulation: never call BLE commands or native action buttons.
 private void vehicle3D(Bundle result)throws Exception{
  if(!"ranchu".equals(Build.HARDWARE))throw new IllegalStateException("3d test is emulator-only");
  java.lang.reflect.Field sceneField=activity.getClass().getDeclaredField("vehicle3D");sceneField.setAccessible(true);
  Object scene=sceneField.get(activity);
  java.lang.reflect.Field webField=scene.getClass().getDeclaredField("web");webField.setAccessible(true);
  android.webkit.WebView web=(android.webkit.WebView)webField.get(scene);
  if(web==null)throw new IllegalStateException("WebView unavailable");
  awaitJavascript(web,"!!window.vehicle3D && vehicle3D.ready",45000);
  long visibleBy=SystemClock.uptimeMillis()+5000;boolean[] visible={false};
  while(!visible[0]&&SystemClock.uptimeMillis()<visibleBy){runOnMainSync(()->visible[0]=web.getVisibility()==View.VISIBLE);if(!visible[0])SystemClock.sleep(100);}
  if(!visible[0])throw new IllegalStateException("model ready but native view remained hidden");
  java.util.concurrent.CountDownLatch painted=new java.util.concurrent.CountDownLatch(1);
  runOnMainSync(()->web.postVisualStateCallback(1,new android.webkit.WebView.VisualStateCallback(){public void onComplete(long id){painted.countDown();}}));
  if(!painted.await(10,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("first 3D frame not drawn");
  awaitJavascript(web,"!vehicle3D.needsReset()",5000);
  View[] reset={null};runOnMainSync(()->{for(int i=0;i<((FrameLayout)scene).getChildCount();i++){View child=((FrameLayout)scene).getChildAt(i);if(child instanceof ImageButton)reset[0]=child;}});
  if(reset[0]==null)throw new IllegalStateException("missing reset button");
  assertResetVisible(reset[0],false);
  for(String key:new String[]{"ArrowLeft","ArrowUp","-"}){
   javascript(web,"document.getElementById('scene').dispatchEvent(new KeyboardEvent('keydown',{key:'"+key+"'}));true");
   awaitJavascript(web,"vehicle3D.needsReset()",3000);assertResetVisible(reset[0],true);
   runOnMainSync(()->reset[0].performClick());
   awaitJavascript(web,"!vehicle3D.needsReset()",5000);assertResetVisible(reset[0],false);
  }
  snapshot("vehicle-3d-default.png");
  java.lang.reflect.Field keyField=activity.getClass().getDeclaredField("key"),listenerField=activity.getClass().getDeclaredField("listener");keyField.setAccessible(true);listenerField.setAccessible(true);
  Object key=keyField.get(activity),listener=listenerField.get(activity);
  java.lang.reflect.Field action=key.getClass().getDeclaredField("actionLabel");action.setAccessible(true);
  java.lang.reflect.Method ack=listener.getClass().getDeclaredMethod("result",int.class);ack.setAccessible(true);
  String[] actions={"开锁","开窗","关锁升窗","后备箱","后备箱"};
  String[] conditions={"s.motion.mirrors.progress===0","s.motion.windows.progress===1","s.motion.mirrors.progress===1 && s.motion.windows.progress===0","s.motion.trunk.progress===1","s.motion.trunk.progress===1"};
  for(int i=0;i<actions.length;i++){
   final String label=actions[i];
   runOnMainSync(()->{try{action.set(key,label);ack.invoke(listener,"后备箱".equals(label)?3:2);}catch(Exception e){throw new RuntimeException(e);}});
   if(i==0)awaitJavascript(web,"vehicle3D.getState().lights.action==='unlock'",1000);
   if(i==2)awaitJavascript(web,"vehicle3D.getState().lights.action==='lock'",1000);
   awaitJavascript(web,"(()=>{const s=vehicle3D.getState();return "+conditions[i]+"})()",5000);
  }
  awaitJavascript(web,"vehicle3D.getState().lights.segments.every(v=>v===0)&&vehicle3D.getState().lights.lowerLoop===0",5000);
  snapshot("vehicle-3d-trunk.png");
  // A blocked external request must never leave the offline asset origin.
  javascript(web,"window.blockedResource=false;fetch('https://example.com/blocked').then(r=>window.blockedResource=!r.ok).catch(()=>window.blockedResource=true);true");
  awaitJavascript(web,"window.blockedResource===true",5000);
  java.lang.reflect.Method fallback=scene.getClass().getDeclaredMethod("unavailable");fallback.setAccessible(true);
  runOnMainSync(()->{try{fallback.invoke(scene);if(web.getVisibility()!=View.INVISIBLE)throw new IllegalStateException("fallback still shows WebView");}catch(Exception e){throw new RuntimeException(e);}});
  snapshot("vehicle-3d-fallback.png");
  result.putString("3d","PASS: local WebGL/Draco, ACK-only mirror/window/trunk animation and distinct lock/unlock light sequences returning dark, repeated trunk release does not close, external fetch blocked, static fallback. No vehicle commands sent.");finish(Activity.RESULT_OK,result);
 }
 private void assertResetVisible(View button,boolean expected)throws Exception{
  long deadline=SystemClock.uptimeMillis()+3000;boolean[] matches={false};
  while(!matches[0]&&SystemClock.uptimeMillis()<deadline){runOnMainSync(()->matches[0]=(button.getVisibility()==View.VISIBLE)==expected);if(!matches[0])SystemClock.sleep(100);}
  if(!matches[0])throw new IllegalStateException("reset button visibility should be "+expected);
 }
 private void washStyle(Bundle result)throws Exception{
  if(!"ranchu".equals(Build.HARDWARE))throw new IllegalStateException("wash-style test is emulator-only");
  java.lang.reflect.Method open=activity.getClass().getDeclaredMethod("openWash"),showPicker=activity.getClass().getDeclaredMethod("showWashPicker",AlertDialog.class);
  open.setAccessible(true);showPicker.setAccessible(true);
  java.lang.reflect.Field dialogField=activity.getClass().getDeclaredField("washDialog");dialogField.setAccessible(true);
  java.lang.reflect.Field loadedField=activity.getClass().getDeclaredField("washLoaded");loadedField.setAccessible(true);
  java.lang.reflect.Method enable=activity.getClass().getDeclaredMethod("enableWash",boolean.class);enable.setAccessible(true);
  runOnMainSync(()->{try{open.invoke(activity);}catch(Exception e){throw new RuntimeException(e);}});
  SystemClock.sleep(500);
  runOnMainSync(()->{try{
   AlertDialog dialog=(AlertDialog)dialogField.get(activity);
   android.graphics.drawable.GradientDrawable background=(android.graphics.drawable.GradientDrawable)dialog.getWindow().getDecorView().getBackground();
   if(background.getColor().getDefaultColor()!=0xFF1D232B)throw new IllegalStateException("wash panel not dark");
   if(!dialog.getWindow().getDecorView().getClipToOutline())throw new IllegalStateException("dialog corners not clipped");
   loadedField.setBoolean(activity,false);enable.invoke(activity,false);
   for(String name:new String[]{"washStart","washEnd","washSave","washStop","washAutoExit"}){
    java.lang.reflect.Field field=activity.getClass().getDeclaredField(name);field.setAccessible(true);
    if(((View)field.get(activity)).isEnabled())throw new IllegalStateException("editable before state read: "+name);
   }
   java.lang.reflect.Field statusField=activity.getClass().getDeclaredField("washStatus");statusField.setAccessible(true);
   if(((TextView)statusField.get(activity)).getCurrentTextColor()!=0xFFAAB6C4)throw new IllegalStateException("unreadable status color");
  }catch(Exception e){throw new RuntimeException(e);}});
  snapshot("wash-dark-preview.png");
  int theme=getTargetContext().getResources().getIdentifier("DuskDialogTheme","style","com.zcy.lkkey");
  AlertDialog[] picker={null};
  runOnMainSync(()->{try{picker[0]=new DatePickerDialog(activity,theme,null,2026,9,10);showPicker.invoke(activity,picker[0]);}catch(Exception e){throw new RuntimeException(e);}});
  snapshot("wash-date-dark-preview.png");
  runOnMainSync(()->{try{
   picker[0].dismiss();picker[0]=new TimePickerDialog(activity,theme,null,14,30,true);showPicker.invoke(activity,picker[0]);
   if(picker[0].getButton(AlertDialog.BUTTON_POSITIVE).getCurrentTextColor()!=0xFFBDD0E4)throw new IllegalStateException("picker accent inconsistent");
  }catch(Exception e){throw new RuntimeException(e);}});
  snapshot("wash-time-dark-preview.png");
  runOnMainSync(()->{try{picker[0].dismiss();((AlertDialog)dialogField.get(activity)).dismiss();if(dialogField.get(activity)!=null)throw new IllegalStateException("dialog retained after dismiss");}catch(Exception e){throw new RuntimeException(e);}});
  result.putString("wash-style","PASS: dark rounded panel; readable status; edit controls disabled before reading; dark date/time pickers; dismissal cleanup. No save/stop commands.");finish(Activity.RESULT_OK,result);
 }
 // Visual regression only: never click a control or send a vehicle command.
 private void scene(Bundle result)throws Exception{
  Class<?> type=activity.getClassLoader().loadClass("com.zcy.lkkey.VehicleSceneView");
  java.lang.reflect.Field boundsField=type.getDeclaredField("carBounds"),sceneField=type.getDeclaredField("sceneBounds"),backdropField=type.getDeclaredField("backdrop"),maskField=type.getDeclaredField("effectsMask");
  boundsField.setAccessible(true);sceneField.setAccessible(true);backdropField.setAccessible(true);maskField.setAccessible(true);
  java.lang.reflect.Method layout=type.getMethod("setVehicleLayout",int.class,int.class);
  runOnMainSync(()->{try{
   View live=findClass(activity.getWindow().getDecorView(),type);
   if(live==null||((ViewGroup)live).getChildCount()!=1)throw new IllegalStateException("missing unified scene/atmosphere");
   if(((android.graphics.RectF)sceneField.get(live)).isEmpty())throw new IllegalStateException("live scene did not receive layout slot");
   android.graphics.Bitmap backdrop=(android.graphics.Bitmap)backdropField.get(live);
   int id=getTargetContext().getResources().getIdentifier("widget_dusk_scene","drawable","com.zcy.lkkey");
   android.graphics.Bitmap original=android.graphics.BitmapFactory.decodeResource(getTargetContext().getResources(),id);
   if(!original.sameAs(backdrop))throw new IllegalStateException("main scene differs from widget resource");
   original.recycle();
   float density=getTargetContext().getResources().getDisplayMetrics().density;
   for(int width:new int[]{320,393,600,1000}){
    View view=(View)type.getConstructor(android.content.Context.class).newInstance(activity);
    int w=Math.round(width*density),h=Math.round(520*density);
    view.layout(0,0,w,h);layout.invoke(view,Math.round(132*density),Math.round(210*density));
    android.graphics.RectF bounds=(android.graphics.RectF)boundsField.get(view);
    android.graphics.RectF sceneBounds=(android.graphics.RectF)sceneField.get(view);
    if(bounds.left<0||bounds.right>w||bounds.bottom>=h)throw new IllegalStateException("car bounds invalid at "+width);
    float sx=sceneBounds.width()/backdrop.getWidth(),sy=sceneBounds.height()/backdrop.getHeight();
    if(Math.abs(sx-sy)>.001f)throw new IllegalStateException("scene aspect ratio distorted at "+width);
    if(sceneBounds.top+720*sy<=bounds.bottom)throw new IllegalStateException("foreground fade covers vehicle at "+width);
    android.graphics.Bitmap mask=(android.graphics.Bitmap)maskField.get(view);
    if((mask.getPixel(Math.round(bounds.centerX()),Math.round(bounds.centerY()))>>>24)!=0)throw new IllegalStateException("effects cover car at "+width);
    int horizonY=Math.round(sceneBounds.top+350*sy);
    if((mask.getPixel(Math.round(w*.04f),horizonY)>>>24)<128)throw new IllegalStateException("horizon effects still clipped at "+width);
    int dockY=Math.round((132+210+5)*density);
    if((mask.getPixel(w/2,dockY)>>>24)!=0)throw new IllegalStateException("effects cover dock at "+width);
    int x=Math.round(bounds.centerX());
    int y=(int)Math.ceil(bounds.bottom)+1;
    android.graphics.Bitmap output=android.graphics.Bitmap.createBitmap(w,h,android.graphics.Bitmap.Config.ARGB_8888);
    view.draw(new android.graphics.Canvas(output));
    if((output.getPixel(x,y)>>>24)!=255)throw new IllegalStateException("scene background not opaque");
    if(output.getPixel(0,h-1)!=0xFF10151B)throw new IllegalStateException("foreground does not blend into surface");
    output.recycle();mask.recycle();((android.graphics.Bitmap)backdropField.get(view)).recycle();
   }
  }catch(Exception e){throw new RuntimeException(e);}});
  snapshot("scene-preview.png");
  result.putString("scene","PASS: exact widget image; 320/393/600/1000dp aspect ratio preserved; effects mask protects car/dock without clipping horizon; foreground blends into surface");finish(Activity.RESULT_OK,result);
 }
 private View findClass(View view,Class<?> type){if(type.isInstance(view))return view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=findClass(group.getChildAt(i),type);if(found!=null)return found;}}return null;}
 private void retry(Bundle result)throws Exception{
  if(!"ranchu".equals(Build.HARDWARE))throw new IllegalStateException("retry test is emulator-only");
  java.lang.reflect.Field pending=activity.getClass().getDeclaredField("pendingAction"),listener=activity.getClass().getDeclaredField("listener");pending.setAccessible(true);listener.setAccessible(true);
  java.lang.reflect.Method request=activity.getClass().getDeclaredMethod("requestAction",Runnable.class);request.setAccessible(true);
  Object observer=listener.get(activity);
  java.lang.reflect.Method changed=observer.getClass().getDeclaredMethod("changed",String.class,boolean.class),failed=observer.getClass().getDeclaredMethod("failed",String.class);changed.setAccessible(true);failed.setAccessible(true);
  for(String title:new String[]{"开锁","关锁","开窗","关窗","后备箱"}){
   click(title);if(!awaitText("未找到已配对",1500)&&!awaitText("请开启手机蓝牙",500))throw new IllegalStateException("no reconnect error for "+title);
   if(pending.get(activity)!=null)throw new IllegalStateException("failed action retained: "+title);
  }
  click("洗车");if(!awaitText("未找到已配对",1500)&&!awaitText("请开启手机蓝牙",500))throw new IllegalStateException("wash read did not reconnect");
  java.lang.reflect.Field loaded=activity.getClass().getDeclaredField("washLoaded");loaded.setAccessible(true);
  java.lang.reflect.Method enableWash=activity.getClass().getDeclaredMethod("enableWash",boolean.class);enableWash.setAccessible(true);
  for(String name:new String[]{"washRefresh","washSave","washStop"}){
   java.lang.reflect.Field field=activity.getClass().getDeclaredField(name);field.setAccessible(true);
   runOnMainSync(()->{try{loaded.setBoolean(activity,true);enableWash.invoke(activity,false);Button button=(Button)field.get(activity);if(!button.isEnabled())throw new IllegalStateException(name+" disabled offline");button.performClick();}catch(Exception e){throw new RuntimeException(e);}});
   if(pending.get(activity)!=null)throw new IllegalStateException("wash failed action retained");
  }
  java.lang.reflect.Field dialog=activity.getClass().getDeclaredField("washDialog");dialog.setAccessible(true);runOnMainSync(()->{try{((AlertDialog)dialog.get(activity)).dismiss();}catch(Exception e){throw new RuntimeException(e);}});
  int[] calls={0};
  runOnMainSync(()->{try{
   pending.set(activity,(Runnable)()->calls[0]++);
   request.invoke(activity,(Runnable)()->calls[0]+=100);
   changed.invoke(observer,"已连接，可以操作",true);changed.invoke(observer,"已连接，可以操作",true);
   if(calls[0]!=1||pending.get(activity)!=null)throw new IllegalStateException("duplicate or missing execution");
   pending.set(activity,(Runnable)()->calls[0]+=100);failed.invoke(observer,"连接超时");changed.invoke(observer,"已连接，可以操作",true);
   if(calls[0]!=1||pending.get(activity)!=null)throw new IllegalStateException("failed action replayed");
   pending.set(activity,(Runnable)()->calls[0]+=100);callActivityOnStop(activity);changed.invoke(observer,"已连接，可以操作",true);
   if(calls[0]!=1||pending.get(activity)!=null)throw new IllegalStateException("action survived leaving app");
  }catch(Exception e){throw new RuntimeException(e);}});
  result.putString("retry","PASS: five controls reconnect offline; wash read reconnects; simulated ready executes once; duplicate request ignored; failure and leaving screen cancel pending action");finish(Activity.RESULT_OK,result);
 }
 private void layout(Bundle result)throws Exception{
  if(!"ranchu".equals(Build.HARDWARE))throw new IllegalStateException("layout test is emulator-only");
  SystemClock.sleep(350);
  View root=activity.getWindow().getDecorView();
  HorizontalScrollView[] rail={null};int[] collapsedHeight={0};
  runOnMainSync(()->{
   rail[0]=(HorizontalScrollView)findClass(root,HorizontalScrollView.class);
   if(rail[0]==null||rail[0].getVisibility()!=View.GONE)throw new IllegalStateException("drawer must start collapsed");
   if(find(root,"NFC 贴片").isShown())throw new IllegalStateException("NFC visible outside drawer");
   try{collapsedHeight[0]=findClass(root,Class.forName("com.zcy.lkkey.Vehicle3DView")).getHeight();}catch(Exception e){throw new RuntimeException(e);}
  });
  snapshot("layout-collapsed.png");click("更多控制");SystemClock.sleep(350);
  runOnMainSync(()->{
   if(rail[0].getVisibility()!=View.VISIBLE)throw new IllegalStateException("drawer did not expand");
   if(rail[0].getChildAt(0).getWidth()<=rail[0].getWidth())throw new IllegalStateException("controls are not horizontally scrollable");
   for(String title:new String[]{"开窗","关窗","后备箱","洗车","NFC 贴片"})if(find(root,title)==null)throw new IllegalStateException("missing "+title);
   try{if(findClass(root,Class.forName("com.zcy.lkkey.Vehicle3DView")).getHeight()>collapsedHeight[0])throw new IllegalStateException("vehicle expanded with drawer");}catch(Exception e){throw new RuntimeException(e);}
   rail[0].scrollTo(rail[0].getChildAt(0).getWidth(),0);
  });
  snapshot("layout-expanded.png");
  runOnMainSync(()->{
   if(rail[0].getScrollX()<=0)throw new IllegalStateException("horizontal scroll failed");
   android.graphics.Rect bounds=new android.graphics.Rect();
   if(!find(root,"NFC 贴片").getGlobalVisibleRect(bounds)||bounds.width()<=0)throw new IllegalStateException("NFC unreachable");
  });
  click("更多控制");SystemClock.sleep(350);
  runOnMainSync(()->{if(rail[0].getVisibility()!=View.GONE)throw new IllegalStateException("drawer did not collapse");});
  result.putString("layout","PASS: collapsed by default; five horizontally scrolling controls; NFC hidden until expanded and reachable after scroll; collapse works; vehicle area reflows");
  finish(Activity.RESULT_OK,result);
 }
 private void controls(Bundle result)throws Exception{
  if(!"ranchu".equals(Build.HARDWARE))throw new IllegalStateException("controls test is emulator-only");
  for(String title:new String[]{"开窗","关窗","后备箱","洗车"}){
   boolean[] found={false};runOnMainSync(()->{Button b=find(activity.getWindow().getDecorView(),title);found[0]=b!=null&&b.isEnabled();});
   if(!found[0])throw new IllegalStateException("missing or disabled while offline: "+title);
  }
  runOnMainSync(()->{
   Button close=find(activity.getWindow().getDecorView(),"关锁"),open=find(activity.getWindow().getDecorView(),"开锁");
   if(close==null||open==null||close.getLeft()>=open.getLeft())throw new IllegalStateException("关锁必须在左、开锁必须在右");
  });
  if(text().contains("锁车并升窗")||text().contains("锁车升窗 ·"))throw new IllegalStateException("removed helper text still present");
  snapshot("app-outline-preview.png");
  runOnMainSync(()->{View content=((android.view.ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0);((ScrollView)content).fullScroll(View.FOCUS_DOWN);});
  SystemClock.sleep(400);snapshot("more-controls-preview.png");
  java.lang.reflect.Method method=activity.getClass().getDeclaredMethod("openWash");method.setAccessible(true);
  runOnMainSync(()->{try{method.invoke(activity);}catch(Exception e){throw new RuntimeException(e);}});
  SystemClock.sleep(400);snapshot("wash-preview.png");
  result.putString("controls","PASS: four secondary controls present and retryable offline; wash panel renders");finish(Activity.RESULT_OK,result);
 }
 private void snapshot(String name)throws Exception{
  java.util.concurrent.CountDownLatch drawn=new java.util.concurrent.CountDownLatch(1);
  runOnMainSync(()->activity.getWindow().getDecorView().postOnAnimation(()->activity.getWindow().getDecorView().postOnAnimation(drawn::countDown)));
  if(!drawn.await(3,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("screenshot frame timed out");
  android.graphics.Bitmap bitmap=getUiAutomation().takeScreenshot();
  try(java.io.FileOutputStream output=new java.io.FileOutputStream(new java.io.File(getTargetContext().getCacheDir(),name))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);}bitmap.recycle();
 }
 private void offline(Bundle result)throws Exception{
  if(!"ranchu".equals(Build.HARDWARE))throw new IllegalStateException("offline test is emulator-only");
  android.content.Context context=getTargetContext();
  android.appwidget.AppWidgetHost host=new android.appwidget.AppWidgetHost(context,734);
  android.appwidget.AppWidgetManager manager=android.appwidget.AppWidgetManager.getInstance(context);
  int id=host.allocateAppWidgetId();
  android.content.ComponentName provider=new android.content.ComponentName("com.zcy.lkkey","com.zcy.lkkey.KeyWidget");
  if(!manager.bindAppWidgetIdIfAllowed(id,provider))throw new IllegalStateException("widget bind permission required");
  runOnMainSync(()->{
   host.startListening();
   android.appwidget.AppWidgetHostView view=host.createView(context,id,manager.getAppWidgetInfo(id));
   LinearLayout root=new LinearLayout(activity);root.setGravity(Gravity.CENTER);root.setBackgroundColor(0xFFF5F6F1);
   int width=(int)(340*context.getResources().getDisplayMetrics().density),height=(int)(180*context.getResources().getDisplayMetrics().density);
   view.updateAppWidget(new RemoteViews(context.getPackageName(),context.getResources().getIdentifier("key_widget","layout",context.getPackageName())));
   root.addView(view,new LinearLayout.LayoutParams(width,height));activity.setContentView(root);
  });
  context.sendBroadcast(new Intent("android.appwidget.action.APPWIDGET_UPDATE").setComponent(provider).putExtra("appWidgetIds",new int[]{id}));
  SystemClock.sleep(1000);
  int openId=context.getResources().getIdentifier("widget_open","id",context.getPackageName());
  int closeId=context.getResources().getIdentifier("widget_close","id",context.getPackageName());
  for(int button:new int[]{openId,closeId}){
   boolean[] found={false};
   runOnMainSync(()->{View v=findId(activity.getWindow().getDecorView(),button);found[0]=v!=null;if(v!=null)v.performClick();});
   if(!found[0])throw new IllegalStateException("widget view missing id="+button+" package="+context.getPackageName()+": "+text());
   if(!awaitText("未找到已配对",5000)&&!awaitText("请开启手机蓝牙",1000))throw new IllegalStateException("widget error not displayed: "+text());
   SystemClock.sleep(300);
  }
  result.putString("offline","PASS: widget inflates; both commands return offline result without crash");
  result.putString("screen",text());
  android.graphics.Bitmap screenshot=getUiAutomation().takeScreenshot();
  try(java.io.FileOutputStream output=new java.io.FileOutputStream(new java.io.File(context.getCacheDir(),"widget-preview.png"))){screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);}
  screenshot.recycle();
  for(ActivityManager.RunningServiceInfo service:context.getSystemService(ActivityManager.class).getRunningServices(100))
   if(service.service.getClassName().equals("com.zcy.lkkey.WidgetService"))throw new IllegalStateException("widget service did not finish");
  host.stopListening();host.deleteHost();finish(Activity.RESULT_OK,result);
 }
 private View findId(View v,int id){if(v.getId()==id)return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View result=findId(g.getChildAt(i),id);if(result!=null)return result;}}return null;}
 private Button find(View v,String label){if(v instanceof Button && (label.equals(v.getContentDescription()) || (v.getContentDescription()!=null && v.getContentDescription().toString().startsWith(label+"，")) || label.contentEquals(((Button)v).getText()) || ((Button)v).getText().toString().startsWith(label+"\n")))return (Button)v;if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++){Button found=find(group.getChildAt(i),label);if(found!=null)return found;}}return null;}
 private boolean awaitReady(long ms){long end=SystemClock.elapsedRealtime()+ms;while(SystemClock.elapsedRealtime()<end){if(text().contains("● 已连接"))return true;SystemClock.sleep(200);}return false;}
 private void click(String label){runOnMainSync(()->{Button b=find(activity.getWindow().getDecorView(),label);if(b==null||!b.isEnabled())throw new IllegalStateException("按钮不可用");b.performClick();});}
 private String text(){StringBuilder b=new StringBuilder();runOnMainSync(()->collect(activity.getWindow().getDecorView(),b));return b.toString();}
 private void collect(View v,StringBuilder b){if(v instanceof TextView)b.append(((TextView)v).getText()).append("\n");if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)collect(g.getChildAt(i),b);}}
 private boolean awaitText(String expected,long ms){long end=SystemClock.elapsedRealtime()+ms;while(SystemClock.elapsedRealtime()<end){if(text().contains(expected))return true;SystemClock.sleep(100);}return false;}
}
