package com.zcy.lkkey;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.widget.FrameLayout;

/** Reuses the widget's finished scene; the vehicle is never scaled apart from its ground. */
public final class VehicleSceneView extends FrameLayout {
    private static final int SURFACE_COLOR=0xFF10151B;
    // Vehicle silhouette in the original 1774 x 887 widget scene, including tire contacts.
    private static final RectF SOURCE_CAR=new RectF(709,222,1585,592);
    private final Bitmap backdrop;
    private final Bitmap emptyBackdrop;
    private boolean threeDActive;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint transition=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sky=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint groundMask=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint atmosphereMask=new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Bitmap effectsMask;
    private final Rect sourceEdge=new Rect();
    private final RectF extension=new RectF();
    private final RectF sceneBounds=new RectF();
    private final RectF carBounds=new RectF();
    // The widget's unobstructed left foreground has the same road texture,
    // without the baked-in vehicle or its shadow.
    private final Rect clearGround=new Rect(0,400,650,887);
    private final RectF threeDBackdrop=new RectF();
    private final RectF threeDGround=new RectF();
    private final float density;
    private int vehicleTop,vehicleHeight;

    public VehicleSceneView(Context context) {
        super(context);
        setWillNotDraw(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        density=getResources().getDisplayMetrics().density;
        backdrop=BitmapFactory.decodeResource(getResources(),R.drawable.widget_dusk_scene);
        emptyBackdrop=BitmapFactory.decodeResource(getResources(),R.drawable.dusk_scene);
        atmosphereMask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        groundMask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
    }

    public void setThreeDActive(boolean active){if(threeDActive==active)return;threeDActive=active;updateComposition();invalidate();}

    /** Align the finished landscape with the real layout slot, including font scaling. */
    public void setVehicleLayout(int top,int height) {
        if(vehicleTop==top&&vehicleHeight==height)return;
        vehicleTop=top;vehicleHeight=height;
        updateComposition();invalidate();
    }

    @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight) {
        super.onSizeChanged(width,height,oldWidth,oldHeight);
        updateComposition();
    }

    private void updateComposition() {
        int width=getWidth();
        float availableHeight=vehicleHeight-20*density;
        if(width<=0||getHeight()<=0||availableHeight<=0){sceneBounds.setEmpty();carBounds.setEmpty();return;}
        if(threeDActive){
            // Compose the landscape around the lower 3D camera, rather than
            // independently center-cropping a portrait background.
            float horizon=vehicleTop+vehicleHeight*.52f;
            float scale=Math.max((float)width/emptyBackdrop.getWidth(),
                (getHeight()-horizon)/(emptyBackdrop.getHeight()-578f));
            float imageWidth=emptyBackdrop.getWidth()*scale;
            float imageTop=horizon-578f*scale;
            threeDBackdrop.set((width-imageWidth)/2,imageTop,(width+imageWidth)/2,imageTop+emptyBackdrop.getHeight()*scale);
            threeDGround.set(0,horizon,width,getHeight());
            sky.setShader(new LinearGradient(threeDBackdrop.left,0,threeDBackdrop.right,0,
                new int[]{emptyBackdrop.getPixel(0,0),emptyBackdrop.getPixel(emptyBackdrop.getWidth()/2,0),
                    emptyBackdrop.getPixel(emptyBackdrop.getWidth()-1,0)},null,Shader.TileMode.CLAMP));
            groundMask.setShader(new LinearGradient(0,horizon,0,horizon+28*density,
                new int[]{Color.TRANSPARENT,Color.WHITE},null,Shader.TileMode.CLAMP));
            transition.setShader(new LinearGradient(0,getHeight()-64*density,0,getHeight(),
                new int[]{SURFACE_COLOR & 0x00FFFFFF,SURFACE_COLOR},null,Shader.TileMode.CLAMP));
            sceneBounds.set(threeDBackdrop);
            carBounds.set(width*.10f,vehicleTop+vehicleHeight*.36f,width*.90f,vehicleTop+vehicleHeight*.75f);
            updateEffectsMask(horizon);
            return;
        }
        float scale=Math.min(width*.68f/SOURCE_CAR.width(),availableHeight/SOURCE_CAR.height());
        float groundLine=vehicleTop+vehicleHeight-28*density;
        float left=width*.53f-SOURCE_CAR.centerX()*scale;
        float top=groundLine-SOURCE_CAR.bottom*scale;
        sceneBounds.set(left,top,left+backdrop.getWidth()*scale,top+backdrop.getHeight()*scale);
        carBounds.set(left+SOURCE_CAR.left*scale,top+SOURCE_CAR.top*scale,
            left+SOURCE_CAR.right*scale,groundLine);
        // A clean gradient extends the sky without magnifying texture into vertical stripes.
        sky.setShader(new LinearGradient(0,0,0,Math.max(1,top),
            new int[]{0xFF111D30,0xFF1C2B42},null,Shader.TileMode.CLAMP));
        updateEffectsMask(top+350*scale);
        // Fade only the empty foreground, leaving the car and its baked-in shadow untouched.
        float fadeTop=top+720*scale;
        transition.setShader(new LinearGradient(0,fadeTop,0,sceneBounds.bottom,
            new int[]{SURFACE_COLOR & 0x00FFFFFF,SURFACE_COLOR},null,Shader.TileMode.CLAMP));
    }

    private void updateEffectsMask(float horizon) {
        int width=getWidth(),height=getHeight();
        if(effectsMask!=null&&(effectsMask.getWidth()!=width||effectsMask.getHeight()!=height)){
            effectsMask.recycle();effectsMask=null;
        }
        if(effectsMask==null)effectsMask=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        effectsMask.eraseColor(Color.TRANSPARENT);
        Canvas mask=new Canvas(effectsMask);
        Paint maskPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        float effectBottom=Math.min(height,vehicleTop+vehicleHeight+4*density);
        maskPaint.setShader(new LinearGradient(0,effectBottom-24*density,0,effectBottom,
            new int[]{Color.WHITE,Color.TRANSPARENT},null,Shader.TileMode.CLAMP));
        mask.drawRect(0,0,width,height,maskPaint);
        maskPaint.setShader(null);
        maskPaint.setColor(Color.BLACK);
        maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        // Cache a soft exclusion zone rather than clipping away the entire
        // horizon. The silhouette and tire shadows stay clear of moving haze.
        RectF protection=new RectF(carBounds);protection.inset(-8*density,-8*density);
        maskPaint.setMaskFilter(new BlurMaskFilter(12*density,BlurMaskFilter.Blur.NORMAL));
        mask.drawRoundRect(protection,8*density,8*density,maskPaint);
        maskPaint.setMaskFilter(null);
        mask.drawRect(carBounds,maskPaint);
        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);
            if(child instanceof DuskEffectsView)((DuskEffectsView)child).setSceneGeometry(carBounds.top,horizon,carBounds.bottom,effectBottom);
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(SURFACE_COLOR);
        if(sceneBounds.isEmpty())return;
        if(threeDActive){
            canvas.drawRect(0,0,getWidth(),Math.max(0,threeDBackdrop.top)+1,sky);
            canvas.drawBitmap(emptyBackdrop,null,threeDBackdrop,paint);
            canvas.saveLayer(threeDGround,null);
            canvas.drawBitmap(backdrop,clearGround,threeDGround,paint);
            canvas.drawRect(threeDGround,groundMask);
            canvas.restore();
            canvas.drawRect(0,getHeight()-64*density,getWidth(),getHeight(),transition);
            return;
        }
        // Extend the empty sky only: no stretching of the vehicle, no second
        // cutout, and no change to the widget resource.
        canvas.drawRect(0,0,getWidth(),Math.max(0,sceneBounds.top)+1,sky);
        canvas.drawBitmap(backdrop,null,sceneBounds,paint);
        // On very wide layouts extend the empty edge columns rather than distort the car.
        if(sceneBounds.left>0){
            sourceEdge.set(0,0,1,backdrop.getHeight());
            extension.set(0,sceneBounds.top,sceneBounds.left,sceneBounds.bottom);
            canvas.drawBitmap(backdrop,sourceEdge,extension,paint);
        }
        if(sceneBounds.right<getWidth()){
            sourceEdge.set(backdrop.getWidth()-1,0,backdrop.getWidth(),backdrop.getHeight());
            extension.set(sceneBounds.right,sceneBounds.top,getWidth(),sceneBounds.bottom);
            canvas.drawBitmap(backdrop,sourceEdge,extension,paint);
        }
        canvas.drawRect(0,Math.max(0,sceneBounds.top),getWidth(),sceneBounds.bottom,transition);
    }

    @Override protected void dispatchDraw(Canvas canvas) {
        if(effectsMask==null||carBounds.isEmpty())return;
        canvas.saveLayer(0,0,getWidth(),getHeight(),null);
        super.dispatchDraw(canvas);
        canvas.drawBitmap(effectsMask,0,0,atmosphereMask);
        canvas.restore();
    }
}
