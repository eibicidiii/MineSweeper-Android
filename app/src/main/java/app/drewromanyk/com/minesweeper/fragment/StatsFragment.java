package app.drewromanyk.com.minesweeper.fragment;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;

import java.util.ArrayList;

import app.drewromanyk.com.minesweeper.R;
import app.drewromanyk.com.minesweeper.adapters.StatsGameDifficultyAdapter;
import app.drewromanyk.com.minesweeper.enums.GameDifficulty;
import app.drewromanyk.com.minesweeper.enums.ResultCodes;
import app.drewromanyk.com.minesweeper.models.YesNoDialogInfo;
import app.drewromanyk.com.minesweeper.util.DialogInfoUtils;
import app.drewromanyk.com.minesweeper.util.Helper;
import app.drewromanyk.com.minesweeper.util.UserPrefStorage;

/**
 * Created by Drew on 9/11/15.
 */
public class StatsFragment extends BaseFragment {

    StatsGameDifficultyAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_stats, container, false);

        setupToolbar((Toolbar) root.findViewById(R.id.toolbar), "Statistics");
        setHasOptionsMenu(true);
        setupStatView(root);

        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_stats, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_trash :
                YesNoDialogInfo dialogInfo = DialogInfoUtils.getInstance(getActivity()).getDialogInfo(ResultCodes.TRASH_STATS_DIALOG.ordinal());
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setTitle(dialogInfo.getTitle())
                        .setMessage(dialogInfo.getDescription())
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteLocalStats();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {}
                        })
                        .create();
                dialog.show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        Helper.getGoogAnalyticsTracker(getActivity()).setScreenName("Screen~" + "Stats");
        Helper.getGoogAnalyticsTracker(getActivity()).send(new HitBuilders.ScreenViewBuilder().build());
    }

    private void setupStatView(ViewGroup root) {
        RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.statsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new StatsGameDifficultyAdapter();
        recyclerView.setAdapter(adapter);

        updateStatsData();
    }

    private void updateStatsData() {
        ArrayList<GameDifficulty> difficulties = new ArrayList<>();
        difficulties.add(GameDifficulty.EASY);
        difficulties.add(GameDifficulty.MEDIUM);
        difficulties.add(GameDifficulty.EXPERT);
        adapter.setGameDifficultyList(difficulties);
    }

    private void updateStatTextViews1() {
        for(int mode = GameDifficulty.EASY.ordinal(); mode <= GameDifficulty.EXPERT.ordinal(); mode++) {
            // Offset is 2 due to RESUME && CUSTOM
//            TextView modeText = contentTextView[mode - 2];
//            TextView modeTitle = titleTextView[mode - 2];
            GameDifficulty difficulty = GameDifficulty.values()[mode];

            String title = "";
            switch (difficulty) {
                case EASY :
                    title = "Easy";
                    break;
                case MEDIUM :
                    title = "Medium";
                    break;
                case EXPERT :
                    title = "Expert";
                    break;
            }

            // Get data
            int wins = UserPrefStorage.getWinsForDifficulty(getActivity(), difficulty);
            int loses = UserPrefStorage.getLosesForDifficulty(getActivity(), difficulty);
            int bestTime = UserPrefStorage.getBestTimeForDifficulty(getActivity(), difficulty);
            float avgTime = UserPrefStorage.getAvgTimeForDifficulty(getActivity(), difficulty);
            float explorPerct = UserPrefStorage.getExplorPercentForDifficulty(getActivity(), difficulty);
            int winStreak = UserPrefStorage.getWinStreakForDifficulty(getActivity(), difficulty);
            int losesStreak = UserPrefStorage.getLoseStreakForDifficulty(getActivity(), difficulty);
            int currentWinStreak = UserPrefStorage.getCurWinStreakForDifficulty(getActivity(), difficulty);
            int currentLosesStreak = UserPrefStorage.getCurLoseStreakForDifficulty(getActivity(), difficulty);
            int bestScore = UserPrefStorage.getBestScoreForDifficulty(getActivity(), difficulty);
            float avgScore = UserPrefStorage.getAvgScoreForDifficulty(getActivity(), difficulty);

            int totalGames = wins + loses;

            // Show data
//            modeTitle.setText(title);
//            modeText.setText(
//                    "Best score: " + ((double) bestScore/1000) + "\nAverage score: " + ((double) avgScore/1000) +
//                            "\nBest time: " + bestTime + "\nAverage time: " + avgTime +
//                            "\nGames won: " + wins + "\nGames played: " + totalGames +
//                            "\nWin percentage: " + ((totalGames != 0) ? ((((double) wins/totalGames)) * 100) : 0) + "%" +
//                            "\nExploration percentage: " + explorPerct + "%" +
//                            "\nLongest winning streak: " + winStreak +
//                            "\nLongest losing streak: " + losesStreak +
//                            "\nCurrent streak: " + ((currentWinStreak == 0) ? currentLosesStreak : currentWinStreak) +
//                            "\n");
        }
    }

    private void deleteLocalStats() {
        for(int mode = GameDifficulty.EASY.ordinal(); mode <= GameDifficulty.EXPERT.ordinal(); mode++) {
            UserPrefStorage.updateStats(getActivity(), GameDifficulty.values()[mode], 0,0,0,0,0,0,0,0,0,0,0);
        }

        updateStatsData();
    }
}