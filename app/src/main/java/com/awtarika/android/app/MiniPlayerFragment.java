package com.awtarika.android.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.app.Fragment;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.awtarika.android.app.model.Song;
import com.awtarika.android.app.util.MusicService;
import com.awtarika.android.app.util.MusicServiceManager;
import com.bumptech.glide.Glide;

public class MiniPlayerFragment extends Fragment {

    private Song mSong = null;
    private Intent mPlayerIntent;
    private MusicService mMusicService;
    private boolean mBound = false;
    private boolean mPlayWhenReady = false;

    private ImageButton mPlayPause;
    private TextView mSongTitle;
    private TextView mArtistName;
    private ImageView mSongImage;

    private OnFragmentInteractionListener mListener;

    private static final String TAG = MiniPlayerFragment.class.getSimpleName();

    public MiniPlayerFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_mini_player, container, false);

        mPlayPause = (ImageButton) rootView.findViewById(R.id.play_pause);
        mPlayPause.setEnabled(true);
        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onButtonPressed();
            }
        });

        mSongTitle = (TextView) rootView.findViewById(R.id.title);
        mArtistName = (TextView) rootView.findViewById(R.id.artist);
        mSongImage = (ImageView) rootView.findViewById(R.id.album_art);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        // bind to MusicService if needed to
        if (MusicServiceManager.shouldBindMusicService) {
            bindMusicService();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        unbindMusicService();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // set listener
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void setSong(Song song) {
        if (song != null) {
            mSongTitle.setText(song.title);
            mArtistName.setText(song.artistName);

            // if song has an image then show it, otherwise, show app icon
            if (!song.imageURL.isEmpty()) {
                Glide.with(this)
                        .load(song.imageURL)
                        .centerCrop()
                        .dontAnimate()
                        .into(mSongImage);
            } else {
                Glide.with(this)
                        .load(R.mipmap.ic_launcher)
                        .centerCrop()
                        .dontAnimate()
                        .into(mSongImage);
            }
        }
    }

    public void startPlaying(Song song) {
        // set song
        mSong = song;
        setSong(song);

        // if not bound then start service and bind then play when bound, if already bound then play
        if (!mBound) {
            bindMusicService();
            mPlayWhenReady = true;
        } else {
            mMusicService.mSong = mSong;
            mMusicService.playSong();
        }
    }

    protected void bindMusicService() {
        // set the intent
        if (mPlayerIntent == null) {
            mPlayerIntent = new Intent(getActivity(), MusicService.class);
        }

        // start service if not started. this is needed to keep the service alive when changing activities
        if (!MusicServiceManager.musicServiceStarted) {
            MusicServiceManager.musicServiceStarted = true;
            getActivity().startService(mPlayerIntent);
        }

        // bind
        getActivity().bindService(mPlayerIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    protected void unbindMusicService() {

        // Unbind from the service
        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
        }
    }

    // defines callbacks for service binding, passed to bindService()
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // bound to MusicService, cast the IBinder and get MusicService instance
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            mMusicService = binder.getService();
            mBound = true;

            // if it's a new song then play when bound
            if (mPlayWhenReady) {
                mPlayWhenReady = false;
                mMusicService.mSong = mSong;
                mMusicService.playSong();
            }

            // set service song
            setSong(mMusicService.mSong);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed() {
        if (mListener != null) {
            mListener.onFragmentInteraction();
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction();
    }
}
