package com.zcy.lkkey;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.widget.RemoteViews;

public class KeyWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){
        render(context,ids,"",false);
    }
    static void update(Context context,String message,boolean busy){
        int[] ids=AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context,KeyWidget.class));
        render(context,ids,message,busy);
    }
    private static void render(Context context,int[] ids,String message,boolean busy){
        RemoteViews views=new RemoteViews(context.getPackageName(),R.layout.key_widget);
        views.setTextViewText(R.id.widget_status,message);
        views.setViewVisibility(R.id.widget_status,message.isEmpty()?android.view.View.GONE:android.view.View.VISIBLE);
        // Keep taps recoverable if Android kills the process during an operation. The service rejects duplicates.
        views.setFloat(R.id.widget_open,"setAlpha",busy?.45f:1f);views.setFloat(R.id.widget_close,"setAlpha",busy?.45f:1f);
        views.setOnClickPendingIntent(R.id.widget_open,command(context,2));
        views.setOnClickPendingIntent(R.id.widget_close,command(context,1));
        Intent app=new Intent(context,MainActivity.class);
        views.setOnClickPendingIntent(R.id.widget_heading,PendingIntent.getActivity(context,0,app,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        AppWidgetManager.getInstance(context).updateAppWidget(ids,views);
    }
    private static PendingIntent command(Context context,int opcode){
        Intent intent=new Intent(context,WidgetService.class).setAction(opcode==2?WidgetService.OPEN:WidgetService.CLOSE);
        return PendingIntent.getForegroundService(context,opcode,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
}
