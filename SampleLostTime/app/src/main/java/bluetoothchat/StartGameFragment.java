package bluetoothchat;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.android.bluetoothchat.R;

import common.model.Utility;

/**
 * Created by Kapil on 02-03-2018.
 */

public class StartGameFragment extends Fragment {

    Button btnStartGame;
    StartGameFragment.IStartGameFragment iStartGameFragment;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_start_fragment, container, false);
        btnStartGame = (Button) view.findViewById(R.id.btnStartGame);
        iStartGameFragment = (StartGameFragment.IStartGameFragment) ((LaunchActivity) getActivity());
        btnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iStartGameFragment.onStartGame();
            }
        });

        return view;
    }

    interface IStartGameFragment {
        void onStartGame();
    }
}