package me.flavors.cnheider.dronecam.ui.activity;

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;

import me.flavors.cnheider.dronecam.R;
import me.flavors.cnheider.dronecam.ui.ar.Renderer;
import me.flavors.cnheider.dronecam.ui.widget.CardboardOverlayView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import timber.log.Timber;

public class camActivity extends CardboardActivity {

    @InjectView(R.id.cardboard_view)
    CardboardView cardboardView;

    @InjectView(R.id.overlay)
    CardboardOverlayView mOverlayView;

    private Renderer mRenderer;
    private Vibrator mVibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mOverlayView = (CardboardOverlayView) findViewById(R.id.overlay);

        setContentView(R.layout.activity_cam);

        //setConvertTapIntoTrigger(true);

        // Inject views
        ButterKnife.inject(this);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Associate a CardboardView.StereoRenderer with cardboardView.
        mRenderer = new Renderer(this);
        cardboardView.setRenderer(mRenderer);

        // Associate the cardboardView with this activity.
        setCardboardView(cardboardView);
    }

    @Override
    public void onCardboardTrigger() {
        Timber.i("onCardboardTrigger");

        // Always give user feedback
        mVibrator.vibrate(100);

        mOverlayView.show3DToast("Peeew");
    }

}
