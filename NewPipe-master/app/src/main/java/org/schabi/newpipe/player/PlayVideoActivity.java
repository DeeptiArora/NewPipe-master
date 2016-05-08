package org.schabi.newpipe.player;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.app.NotificationCompat.Builder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.VideoView;

import org.schabi.newpipe.ActivityCommunicator;
import org.schabi.newpipe.App;
import org.schabi.newpipe.BuildConfig;
import org.schabi.newpipe.R;
import org.schabi.newpipe.VideoItemDetailActivity;
import org.schabi.newpipe.VideoItemDetailFragment;

/**
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 * PlayVideoActivity.java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

public class PlayVideoActivity extends AppCompatActivity {

    //// TODO: 11.09.15 add "choose stream" menu 
    
    private static final String TAG = PlayVideoActivity.class.toString();
    public static final String VIDEO_URL = "video_url";
    public static final String STREAM_URL = "stream_url";
    public static final String VIDEO_TITLE = "video_title";
    private static final String POSITION = "position";
    public static final String START_POSITION = "start_position";
    public static final String CHANNEL_NAME = "channel_name";

    private static final long HIDING_DELAY = 3000;

    private String videoUrl = "";

    private ActionBar actionBar;
    private VideoView videoView;
    private int position = 0;
    private MediaController mediaController;
    private ProgressBar progressBar;
    private View decorView;
    private boolean uiIsHidden = false;
    private static long lastUiShowTime = 0;
    private boolean isLandscape = true;
    private boolean hasSoftKeys = false;

    private SharedPreferences prefs;
    private static final String PREF_IS_LANDSCAPE = "is_landscape";


    //////////////////// comp530
    private NotificationCompat.Builder noteBuilder;
    private PlayVideoActivity owner;
    private int noteID = TAG.hashCode();

    //private static final String TAG = BackgroundPlayer.class.toString();
    private static final String ACTION_STOP = TAG + ".STOP";
    private static final String ACTION_PLAYPAUSE = TAG + ".PLAYPAUSE";
    private volatile int serviceId = -1;
    private volatile String webUrl = "";
    private String title = "";
    private Bitmap videoThumbnail = null;
    private volatile String channelName = "";
    private Notification note;
    private NotificationManager noteMgr;
    ////////////////////


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_play_video);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        //set background arrow style
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);

        isLandscape = checkIfLandscape();
        hasSoftKeys = checkIfHasSoftKeys();

        actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        final Intent intent = getIntent();
        if(mediaController == null) {
            //prevents back button hiding media controller controls (after showing them)
            //instead of exiting video
            //see http://stackoverflow.com/questions/6051825
            //also solves https://github.com/theScrabi/NewPipe/issues/99
            mediaController = new MediaController(this) {
                @Override
                public boolean dispatchKeyEvent(KeyEvent event) {
                    int keyCode = event.getKeyCode();
                    final boolean uniqueDown = event.getRepeatCount() == 0
                            && event.getAction() == KeyEvent.ACTION_DOWN;
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        if (uniqueDown)
                        {
                            if (isShowing()) {
                                finish();
                            } else {
                                hide();
                            }
                        }
                        return true;
                    }
                    return super.dispatchKeyEvent(event);
                }


            };
        }

        position = intent.getIntExtra(START_POSITION, 0)*1000;//convert from seconds to milliseconds

        videoView = (VideoView) findViewById(R.id.video_view);


        progressBar = (ProgressBar) findViewById(R.id.play_video_progress_bar);
        try {
            videoView.setMediaController(mediaController);
            videoView.setVideoURI(Uri.parse(intent.getStringExtra(STREAM_URL)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try//////comp530
        {
            videoThumbnail = ActivityCommunicator.getCommunicator().backgroundPlayerThumbnail;
        }
        catch (Exception e) {
            Log.e(TAG, "Could not get video thumbnail from ActivityCommunicator");
            e.printStackTrace();
        }
        title = intent.getStringExtra(VIDEO_TITLE); //////comp530
        channelName = getIntent().getStringExtra(CHANNEL_NAME); //////comp530

        videoView.requestFocus();

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                progressBar.setVisibility(View.GONE);
                videoView.seekTo(position);
                if (position <= 0) {
                    videoView.start();
                    //createNotification();

                    showUi();
                } else {



                    videoView.pause();

                }
            }


        });
        videoUrl = intent.getStringExtra(VIDEO_URL);

        Button button = (Button) findViewById(R.id.content_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(uiIsHidden) {
                    showUi();
                } else {
                    hideUi();
                }
            }
        });
        decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if (visibility == View.VISIBLE && uiIsHidden) {
                    showUi();
                }
            }
        });

        if (android.os.Build.VERSION.SDK_INT >= 17) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        prefs = getPreferences(Context.MODE_PRIVATE);
        if(prefs.getBoolean(PREF_IS_LANDSCAPE, false) && !isLandscape) {
            toggleOrientation();
        }
    }

    @Override
    public boolean onCreatePanelMenu(int featured, Menu menu) {
        super.onCreatePanelMenu(featured, menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.video_player, menu);

        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        videoView.pause();


        ////   New Code    //////comp530
        IntentFilter filter = new IntentFilter();
        filter.setPriority(Integer.MAX_VALUE);
        filter.addAction(ACTION_PLAYPAUSE);
        filter.addAction(ACTION_STOP);
        registerReceiver(broadcastReceiver, filter);

        if(note == null)
        {
            note = buildNotification();

            //startForeground(noteID, note);
            noteMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            noteMgr.notify(noteID, note);
        }

        showPlaySign();
        ///New Code
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        prefs = getPreferences(Context.MODE_PRIVATE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_item_share:
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_TEXT, videoUrl);
                intent.setType("text/plain");
                startActivity(Intent.createChooser(intent, getString(R.string.share_dialog_title)));
                break;
            case R.id.menu_item_screen_rotation:
                toggleOrientation();
                break;
            default:
                Log.e(TAG, "Error: MenuItem not known");
                return false;
        }
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);

        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscape = true;
            adjustMediaControlMetrics();
        } else if (config.orientation == Configuration.ORIENTATION_PORTRAIT){
            isLandscape = false;
            adjustMediaControlMetrics();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        //savedInstanceState.putInt(POSITION, videoView.getCurrentPosition());
        //videoView.pause();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        position = savedInstanceState.getInt(POSITION);
        //videoView.seekTo(position);
    }

    private void showUi() {
        try {
            uiIsHidden = false;
            mediaController.show(100000);
            actionBar.show();
            adjustMediaControlMetrics();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if ((System.currentTimeMillis() - lastUiShowTime) >= HIDING_DELAY) {
                        hideUi();
                    }

                }
            }, HIDING_DELAY);
            lastUiShowTime = System.currentTimeMillis();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void hideUi() {
        uiIsHidden = true;
        actionBar.hide();
        mediaController.hide();
        if (android.os.Build.VERSION.SDK_INT >= 17) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void adjustMediaControlMetrics() {
        MediaController.LayoutParams mediaControllerLayout
                = new MediaController.LayoutParams(MediaController.LayoutParams.MATCH_PARENT,
                MediaController.LayoutParams.WRAP_CONTENT);

        if(!hasSoftKeys) {
            mediaControllerLayout.setMargins(20, 0, 20, 20);
        } else {
            int width = getNavigationBarWidth();
            int height = getNavigationBarHeight();
            mediaControllerLayout.setMargins(width + 20, 0, width + 20, height + 20);
        }
        mediaController.setLayoutParams(mediaControllerLayout);
    }

    private boolean checkIfHasSoftKeys(){
        return Build.VERSION.SDK_INT >= 17 ||
                getNavigationBarHeight() != 0 ||
                getNavigationBarWidth() != 0;
    }

    private int getNavigationBarHeight() {
        if(Build.VERSION.SDK_INT >= 17) {
            Display d = getWindowManager().getDefaultDisplay();

            DisplayMetrics realDisplayMetrics = new DisplayMetrics();
            d.getRealMetrics(realDisplayMetrics);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            d.getMetrics(displayMetrics);

            int realHeight = realDisplayMetrics.heightPixels;
            int displayHeight = displayMetrics.heightPixels;
            return (realHeight - displayHeight);
        } else {
            return 50;
        }
    }

    private int getNavigationBarWidth() {
        if(Build.VERSION.SDK_INT >= 17) {
            Display d = getWindowManager().getDefaultDisplay();

            DisplayMetrics realDisplayMetrics = new DisplayMetrics();
            d.getRealMetrics(realDisplayMetrics);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            d.getMetrics(displayMetrics);

            int realWidth = realDisplayMetrics.widthPixels;
            int displayWidth = displayMetrics.widthPixels;
            return (realWidth - displayWidth);
        } else {
            return 50;
        }
    }

    private boolean checkIfLandscape() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels < displayMetrics.widthPixels;
    }

    private void toggleOrientation() {
        if(isLandscape)  {
            isLandscape = false;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            isLandscape = true;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_IS_LANDSCAPE, isLandscape);
        editor.apply();
    }


    private Notification buildNotification() {//////comp530
        Notification note;
        owner = this;
        Resources res = getApplicationContext().getResources();
        noteBuilder = new NotificationCompat.Builder(owner);

        PendingIntent playPI = PendingIntent.getBroadcast(owner, noteID,
                new Intent(ACTION_PLAYPAUSE), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stopPI = PendingIntent.getBroadcast(owner, noteID,
                new Intent(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT);
            /*
            NotificationCompat.Action pauseButton = new NotificationCompat.Action.Builder
                    (R.drawable.ic_pause_white_24dp, "Pause", playPI).build();
            */

        //build intent to return to video, on tapping notification
        Intent openDetailViewIntent = new Intent(getApplicationContext(),
                VideoItemDetailActivity.class);
        openDetailViewIntent.putExtra(VideoItemDetailFragment.STREAMING_SERVICE, serviceId);
        openDetailViewIntent.putExtra(VideoItemDetailFragment.VIDEO_URL, webUrl);
        openDetailViewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent openDetailView = PendingIntent.getActivity(owner, noteID,
                openDetailViewIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        noteBuilder
                .setOngoing(true)
                .setDeleteIntent(stopPI)
                        //doesn't fit with Notification.MediaStyle
                        //.setProgress(vidLength, 0, false)
                .setSmallIcon(R.drawable.ic_play_circle_filled_white_24dp)
                .setTicker(
                        String.format(res.getString(
                                R.string.background_player_time_text), title))
                .setContentIntent(PendingIntent.getActivity(getApplicationContext(),
                        noteID, openDetailViewIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentIntent(openDetailView);


        RemoteViews view =
                new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.player_notification);
        view.setImageViewBitmap(R.id.notificationCover, videoThumbnail);
        view.setTextViewText(R.id.notificationSongName, title);
        view.setTextViewText(R.id.notificationArtist, channelName);
        view.setOnClickPendingIntent(R.id.notificationStop, stopPI);
        view.setOnClickPendingIntent(R.id.notificationPlayPause, playPI);
        view.setOnClickPendingIntent(R.id.notificationContent, openDetailView);

        //possibly found the expandedView problem,
        //but can't test it as I don't have a 5.0 device. -medavox
        RemoteViews expandedView =
                new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.player_notification_expanded);
        expandedView.setImageViewBitmap(R.id.notificationCover, videoThumbnail);
        expandedView.setTextViewText(R.id.notificationSongName, title);
        expandedView.setTextViewText(R.id.notificationArtist, channelName);
        expandedView.setOnClickPendingIntent(R.id.notificationStop, stopPI);
        expandedView.setOnClickPendingIntent(R.id.notificationPlayPause, playPI);
        expandedView.setOnClickPendingIntent(R.id.notificationContent, openDetailView);


        noteBuilder.setCategory(Notification.CATEGORY_TRANSPORT);

        //Make notification appear on lockscreen
        noteBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
        noteBuilder.setVisibility(Notification.DEFAULT_SOUND);

        note = noteBuilder.build();
        note.contentView = view;

        if (android.os.Build.VERSION.SDK_INT > 16) {
            note.bigContentView = expandedView;
        }

        return note;
    }

    /**Handles button presses from the notification. */   //////comp530
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //Log.i(TAG, "received broadcast action:"+action);
            if(action.equals(ACTION_PLAYPAUSE)) {
                if(videoView.isPlaying()) {
                    videoView.pause();
                    showPlaySign();
                }
                else {
                    //reacquire CPU lock after auto-releasing it on pause
                    //videoView.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                    videoView.start();
                    showPauseSign();
                }
            }
            else if(action.equals(ACTION_STOP)) {
                ////this auto-releases CPU lock
                ////videoView.stop();
                //videoView.stopPlayback();
                //afterPlayCleanup();
            }
        }
    };

    private void afterPlayCleanup() {//////comp530
        //remove progress bar
        //noteBuilder.setProgress(0, 0, false);

        //remove notification
        noteMgr.cancel(noteID);
        unregisterReceiver(broadcastReceiver);
        //release mediaPlayer's system resources
        //videoView.release();
        //videoView.
        //release wifilock
        //videoView.release();
        //remove foreground status of service; make BackgroundPlayer killable
        //stopForeground(true);

        //stopSelf();
    }

    private void showPlaySign()//////comp530
    {


        note.contentView.setImageViewResource(R.id.notificationPlayPause, R.drawable.ic_play_circle_filled_white_24dp);
        if(android.os.Build.VERSION.SDK_INT >=16){
            note.bigContentView.setImageViewResource(R.id.notificationPlayPause, R.drawable.ic_play_circle_filled_white_24dp);
        }
        noteMgr.notify(noteID, note);

    }

    private void showPauseSign()//////comp530
    {
        note.contentView.setImageViewResource(R.id.notificationPlayPause, R.drawable.ic_pause_white_24dp);
        if(android.os.Build.VERSION.SDK_INT >=16){
            note.bigContentView.setImageViewResource(R.id.notificationPlayPause, R.drawable.ic_pause_white_24dp);
        }
        noteMgr.notify(noteID, note);

    }
}
