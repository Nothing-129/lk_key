package com.zcy.lkkey;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;
import java.util.Random;

/** Visible, slow-moving dusk atmosphere; the scene masks out the vehicle and controls. */
public final class DuskEffectsView extends View {
    private final Paint glow=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint haze=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint light=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint speck=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[][] stars=new float[24][4];
    private final float[][] dust=new float[14][3];
    private final float density;
    private float roof,horizon,ground,effectBottom;
    private boolean animating;
    private long lastFrame;
    private float seconds;

    private final Runnable frame=new Runnable() {
        @Override public void run() {
            if(!shouldAnimate())return;
            long now=SystemClock.uptimeMillis();
            seconds+=(now-lastFrame)/1000f;
            lastFrame=now;
            invalidate();
            // Slow atmospheric movement needs only 25 frames per second.
            postDelayed(this,40);
        }
    };

    public DuskEffectsView(Context context) {
        super(context);
        density=getResources().getDisplayMetrics().density;
        glow.setShader(softGlow(0xFFE5AF85));
        haze.setShader(softGlow(0xFF8FAEC9));
        light.setShader(softGlow(0xFF74A8C5));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setClickable(false);
        setFocusable(false);
        Random random=new Random(37);
        for(float[] star:stars){
            star[0]=.06f+random.nextFloat()*.88f;
            star[1]=.05f+random.nextFloat()*.90f;
            star[2]=random.nextFloat()*6.2832f;
            star[3]=.65f+random.nextFloat()*.60f;
        }
        for(int i=0;i<dust.length;i++){
            float[] mote=dust[i];
            // Most motes rise in the open scenery beside the protected vehicle.
            mote[0]=i%2==0?.04f+random.nextFloat()*.12f:.88f+random.nextFloat()*.08f;
            mote[1]=random.nextFloat();
            mote[2]=random.nextFloat();
        }
    }

    private Shader softGlow(int color) {
        int rgb=color & 0x00FFFFFF;
        return new RadialGradient(0,0,1,
            new int[]{color,0x80000000 | rgb,0x18000000 | rgb,rgb},
            new float[]{0,.38f,.76f,1},Shader.TileMode.CLAMP);
    }

    public void setSceneGeometry(float roof,float horizon,float ground,float effectBottom) {
        this.roof=roof;this.horizon=horizon;this.ground=ground;this.effectBottom=effectBottom;
        invalidate();
    }

    public void setAnimating(boolean enabled) {
        animating=enabled;
        refreshAnimation();
    }

    private boolean shouldAnimate() {
        return animating&&isAttachedToWindow()&&isShown()
            &&getWindowVisibility()==VISIBLE&&hasWindowFocus()
            &&ValueAnimator.areAnimatorsEnabled();
    }

    private void refreshAnimation() {
        // Visibility callbacks can run during View's superclass constructor.
        if(frame==null)return;
        removeCallbacks(frame);
        if(shouldAnimate()){
            lastFrame=SystemClock.uptimeMillis();
            post(frame);
        }
        invalidate();
    }

    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();refreshAnimation();}
    @Override protected void onDetachedFromWindow(){removeCallbacks(frame);super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);refreshAnimation();}
    @Override protected void onVisibilityChanged(View view,int visibility){super.onVisibilityChanged(view,visibility);refreshAnimation();}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);refreshAnimation();}

    private float wave(float speed,float phase){return (float)Math.sin(seconds*speed+phase);}

    private void ellipse(Canvas canvas,Paint paint,float x,float y,float rx,float ry,float angle,int alpha) {
        paint.setAlpha(alpha);
        canvas.save();
        canvas.translate(x,y);
        canvas.rotate(angle);
        canvas.scale(rx,ry);
        canvas.drawCircle(0,0,1,paint);
        canvas.restore();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width=getWidth(),height=getHeight();
        if(width<=0||height<=0)return;
        if(effectBottom<=0)return;
        float skyTop=64*density;
        float skyBottom=Math.max(skyTop+1,roof-12*density);
        float skySpan=skyBottom-skyTop;
        canvas.save();
        canvas.clipRect(0,skyTop,width,effectBottom);
        // Broad haze drifts far enough to be perceptible within a few seconds.
        ellipse(canvas,haze,width*(.38f+.22f*wave(.32f,0)),skyTop+skySpan*.52f,
            width*.70f,14*density,-8,42+(int)(10*wave(.65f,1)));
        ellipse(canvas,haze,width*(.76f+.18f*wave(.27f,2)),skyBottom-8*density,
            width*.58f,10*density,4,36+(int)(10*wave(.7f,3)));
        // Use the actual scene horizon, not a fraction of the full page height.
        ellipse(canvas,glow,width*(.48f+.09f*wave(.35f,1)),horizon+2*density*wave(.45f,2),
            width*.70f,26*density,0,84+(int)(24*wave(.85f,0)));

        // Mist and reflections are visible beside and below the car; the cached
        // scene mask keeps them off its paintwork and out of the control dock.
        ellipse(canvas,haze,width*(.15f+.18f*wave(.4f,0)),horizon+22*density,
            width*.65f,12*density,-2,64+(int)(12*wave(.6f,0)));
        ellipse(canvas,haze,width*(.82f+.16f*wave(.33f,2)),ground+12*density,
            width*.58f,13*density,2,58);
        ellipse(canvas,light,width*(.46f+.28f*wave(.48f,4)),ground+17*density,
            width*.46f,8*density,-2,82+(int)(18*wave(.75f,2)));

        for(int i=0;i<stars.length;i++){
            float[] star=stars[i];
            float brightness=.5f+.5f*wave(1.05f,star[2]);
            float x=width*(star[0]+.014f*wave(.18f,star[2]));
            float y=skyTop+skySpan*(star[1]+.012f*wave(.15f,star[2]+1));
            if(i%5==0)ellipse(canvas,light,x,y,3.5f*density,3.5f*density,0,20+(int)(28*brightness));
            speck.setColor(0xFFDDE7F3);speck.setAlpha(62+(int)(112*brightness));
            canvas.drawCircle(x,y,star[3]*density,speck);
        }
        speck.setColor(0xFFE3C3A2);
        for(float[] mote:dust){
            float progress=(seconds/16f+mote[1])%1f;
            speck.setAlpha((int)(104*Math.sin(progress*Math.PI)));
            float x=width*(mote[0]+.014f*wave(.42f,mote[2]*6.2832f));
            float y=horizon+32*density-progress*100*density;
            canvas.drawCircle(x,y,(.65f+.4f*mote[2])*density,speck);
        }
        canvas.restore();
    }
}
