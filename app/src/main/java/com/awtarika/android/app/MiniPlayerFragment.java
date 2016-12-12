package com.awtarika.android.app;

import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.awtarika.android.app.model.Song;
import com.awtarika.android.app.util.AudioPlayerSingleton;
import com.bumptech.glide.Glide;

public class MiniPlayerFragment extends Fragment {

//    private Song mSong;
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

    public void setSong(Song song) {
        if (song != null) {
            mSongTitle.setText(song.title);
            mArtistName.setText(song.artistName);

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

    @Override
    public void onResume() {
        super.onResume();

        setSong(AudioPlayerSingleton.getInstance().mSong);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed() {
        if (mListener != null) {
            mListener.onFragmentInteraction();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
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
