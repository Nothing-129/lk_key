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
   activity=startActivitySync(new Intent().setClassName("com.zcy.lkkey","com.zcy.lkkey.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
   if("retry".equals(mode)){retry(result);return;}
   if("controls".equals(mode)){controls(result);return;}
   if("offline".equals(mode)){offline(result);return;}
   if(!awaitReady(35000))throw new IllegalStateException("未就绪："+text());
   result.putString("connection","READY");
   if("open-close".equals(mode)){
    click("开锁");if(!awaitText("设备已确认开锁指令",8000))throw new IllegalStateException("开锁未确认："+text());
    result.putString("open","ACK_SUCCESS");sendStatus(0,result);SystemClock.sleep(2500);
    click("关锁");if(!awaitText("设备已确认关锁升窗指令",8000))throw new IllegalStateException("关锁未确认："+text());
    result.putString("close","ACK_SUCCESS");
   }
   result.putString("screen",text());finish(Activity.RESULT_OK,result);
  }catch(Exception e){result.putString("error",e.toString());finish(Activity.RESULT_CANCELED,result);}
 }
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
 private Button find(View v,String label){if(v instanceof Button && (label.equals(v.getContentDescription()) || label.contentEquals(((Button)v).getText()) || ((Button)v).getText().toString().startsWith(label+"\n")))return (Button)v;if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++){Button found=find(group.getChildAt(i),label);if(found!=null)return found;}}return null;}
 private boolean awaitReady(long ms){long end=SystemClock.elapsedRealtime()+ms;while(SystemClock.elapsedRealtime()<end){if(text().contains("● 已连接"))return true;SystemClock.sleep(200);}return false;}
 private void click(String label){runOnMainSync(()->{Button b=find(activity.getWindow().getDecorView(),label);if(b==null||!b.isEnabled())throw new IllegalStateException("按钮不可用");b.performClick();});}
 private String text(){StringBuilder b=new StringBuilder();runOnMainSync(()->collect(activity.getWindow().getDecorView(),b));return b.toString();}
 private void collect(View v,StringBuilder b){if(v instanceof TextView)b.append(((TextView)v).getText()).append("\n");if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)collect(g.getChildAt(i),b);}}
 private boolean awaitText(String expected,long ms){long end=SystemClock.elapsedRealtime()+ms;while(SystemClock.elapsedRealtime()<end){if(text().contains(expected))return true;SystemClock.sleep(100);}return false;}
}
